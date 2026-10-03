package com.example.bookstore.shipping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.net.SocketTimeoutException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;

/**
 * {@link ShippingClient} against a mock of the carrier's API. {@link RestClientTest @RestClientTest} starts only the
 * client and its RestClient, and binds a {@link MockRestServiceServer} to the {@code RestClient.Builder}: requests
 * never leave the JVM, and each test says which request it expects and how the "carrier" answers.
 */
@RestClientTest(ShippingClient.class)
@Import(ShippingConfiguration.class)
@DisplayName("S11-01 · Calling an API with RestClient")
class ShippingClientTest {

    private static final String ESTIMATE_06236_800 =
            "https://api.carrier.example/v1/estimates?postalCode=06236&weightGrams=800";

    @Autowired
    private ShippingClient client;

    @Autowired
    private MockRestServiceServer carrier;

    @AfterEach
    void everyExpectedRequestWasSent() {
        carrier.verify();
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("asks GET /v1/estimates with the postal code and the weight, and maps the carrier's JSON")
        void estimates() {
            carrier.expect(requestTo(ESTIMATE_06236_800))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess("""
                            { "carrier": "CJ Logistics", "fee": 3000, "currency": "KRW", "estimated_days": 2 }""",
                            MediaType.APPLICATION_JSON));

            assertThat(client.estimate("06236", 800)).contains(new ShippingEstimate("CJ Logistics", 3000, 2));
        }

        @Test
        @DisplayName("asks for JSON with the Accept header")
        void asksForJson() {
            carrier.expect(requestTo(ESTIMATE_06236_800))
                    .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                    .andRespond(withSuccess("""
                            { "carrier": "Hanjin", "fee": 4000, "estimated_days": 1 }""", MediaType.APPLICATION_JSON));

            assertThat(client.estimate("06236", 800)).isPresent();
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("a 404 means that the carrier doesn't deliver there: an empty result, not an error")
        void notServed() {
            carrier.expect(requestTo("https://api.carrier.example/v1/estimates?postalCode=63000&weightGrams=800"))
                    .andRespond(withResourceNotFound().contentType(MediaType.APPLICATION_JSON)
                            .body("""
                                    { "error": "postal code not served" }"""));

            assertThat(client.estimate("63000", 800)).isEmpty();
        }

        @Test
        @DisplayName("properties that ShippingEstimate doesn't have are ignored")
        void ignoresUnknownProperties() {
            carrier.expect(requestTo(ESTIMATE_06236_800))
                    .andRespond(withSuccess("""
                            { "carrier": "CJ Logistics", "fee": 3000, "estimated_days": 2,
                              "service_level": "standard", "tracking": { "available": true } }""",
                            MediaType.APPLICATION_JSON));

            assertThat(client.estimate("06236", 800)).contains(new ShippingEstimate("CJ Logistics", 3000, 2));
        }

        @Test
        @DisplayName("the lightest and the heaviest parcel are both accepted")
        void weightLimits() {
            carrier.expect(requestTo("https://api.carrier.example/v1/estimates?postalCode=06236&weightGrams=1"))
                    .andRespond(withSuccess("""
                            { "carrier": "CJ Logistics", "fee": 3000, "estimated_days": 2 }""",
                            MediaType.APPLICATION_JSON));
            carrier.expect(requestTo("https://api.carrier.example/v1/estimates?postalCode=06236&weightGrams=30000"))
                    .andRespond(withSuccess("""
                            { "carrier": "CJ Logistics", "fee": 9000, "estimated_days": 3 }""",
                            MediaType.APPLICATION_JSON));

            assertThat(client.estimate("06236", 1)).map(ShippingEstimate::fee).contains(3000L);
            assertThat(client.estimate("06236", ShippingClient.MAX_WEIGHT_GRAMS)).map(ShippingEstimate::fee)
                    .contains(9000L);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @ParameterizedTest(name = "status {0}")
        @ValueSource(ints = {500, 502, 503})
        @DisplayName("a 5xx becomes a ShippingUnavailableException that names the status")
        void serverError(int status) {
            carrier.expect(requestTo(ESTIMATE_06236_800)).andRespond(withStatus(HttpStatus.valueOf(status)));

            assertThatThrownBy(() -> client.estimate("06236", 800))
                    .isInstanceOf(ShippingUnavailableException.class)
                    .hasMessageContaining(String.valueOf(status));
        }

        @Test
        @DisplayName("no answer at all becomes a ShippingUnavailableException, with the network error as its cause")
        void noAnswer() {
            carrier.expect(requestTo(ESTIMATE_06236_800))
                    .andRespond(withException(new SocketTimeoutException("Read timed out")));

            assertThatThrownBy(() -> client.estimate("06236", 800))
                    .isInstanceOf(ShippingUnavailableException.class)
                    .hasRootCauseInstanceOf(IOException.class)
                    .hasRootCauseMessage("Read timed out");
        }

        @Test
        @DisplayName("a 400 is a bug in our request: RestClient's HttpClientErrorException passes through")
        void badRequest() {
            carrier.expect(requestTo(ESTIMATE_06236_800)).andRespond(withBadRequest());

            assertThatThrownBy(() -> client.estimate("06236", 800))
                    .isInstanceOf(HttpClientErrorException.BadRequest.class);
        }

        @ParameterizedTest(name = "postal code {0}")
        @NullSource
        @ValueSource(strings = {"", "1234", "123456", "0623a", " 6236"})
        @DisplayName("a postal code that isn't 5 digits is rejected before any request is sent")
        void invalidPostalCode(String postalCode) {
            assertThatThrownBy(() -> client.estimate(postalCode, 800))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest(name = "weight {0} g")
        @ValueSource(ints = {0, -1, ShippingClient.MAX_WEIGHT_GRAMS + 1})
        @DisplayName("a weight outside 1 to 30,000 grams is rejected before any request is sent")
        void invalidWeight(int weightGrams) {
            assertThatThrownBy(() -> client.estimate("06236", weightGrams))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
