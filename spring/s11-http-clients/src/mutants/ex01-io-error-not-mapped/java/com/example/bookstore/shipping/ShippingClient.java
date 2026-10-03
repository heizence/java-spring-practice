package com.example.bookstore.shipping;

import java.util.Optional;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Asks the carrier's API what shipping costs.
 *
 * <p>The client is built from the {@link RestClient.Builder} that Spring Boot configures: it brings the JSON
 * converters and the timeouts from {@code spring.http.clients.*}, and lets tests replace the network with a mock
 * server.
 */
@Component
public class ShippingClient {

    /** The heaviest parcel the carrier takes, in grams. */
    public static final int MAX_WEIGHT_GRAMS = 30_000;

    private final RestClient restClient;

    public ShippingClient(RestClient.Builder builder, ShippingProperties properties) {
        this.restClient = builder.baseUrl(properties.baseUrl()).build();
    }

    /**
     * Asks what it costs to ship a parcel to a postal code, and how long it takes.
     *
     * <h2>Rules</h2>
     * <ul>
     *   <li>The request is {@code GET /v1/estimates?postalCode=06236&weightGrams=800} on the base URL, with the
     *       header {@code Accept: application/json}.</li>
     *   <li>{@code 200}: the carrier's JSON becomes a {@link ShippingEstimate}.</li>
     *   <li>{@code 404}: the carrier doesn't deliver to this postal code. That's an answer, not an error: return an
     *       empty {@code Optional}.</li>
     *   <li>{@code 5xx}: the carrier has a problem. Throw a {@link ShippingUnavailableException} whose message
     *       contains the status code, such as {@code 503}.</li>
     *   <li>No answer at all (the connection fails or times out): throw a {@link ShippingUnavailableException}
     *       with the original exception as its cause.</li>
     *   <li>Any other {@code 4xx} means that our request was wrong, a bug on our side: let RestClient's
     *       {@link HttpClientErrorException} through unchanged.</li>
     * </ul>
     *
     * @param postalCode  a Korean postal code: exactly 5 digits
     * @param weightGrams the parcel's weight, from 1 to {@link #MAX_WEIGHT_GRAMS}
     * @return the estimate, or empty if the carrier doesn't deliver there
     * @throws IllegalArgumentException     if an argument is invalid; then no request is sent
     * @throws ShippingUnavailableException if the carrier fails or can't be reached
     */
    public Optional<ShippingEstimate> estimate(String postalCode, int weightGrams) {
        Assert.isTrue(postalCode != null && postalCode.matches("\\d{5}"),
                () -> "postalCode must be 5 digits: " + postalCode);
        Assert.isTrue(weightGrams >= 1 && weightGrams <= MAX_WEIGHT_GRAMS,
                () -> "weightGrams must be between 1 and %d: %d".formatted(MAX_WEIGHT_GRAMS, weightGrams));
        try {
            ShippingEstimate estimate = restClient.get()
                    // The URI builder encodes the parameters, so no value can break the query string.
                    .uri(uri -> uri.path("/v1/estimates")
                            .queryParam("postalCode", postalCode)
                            .queryParam("weightGrams", weightGrams)
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    // Without this handler, a 5xx would arrive as RestClient's HttpServerErrorException: an
                    // implementation detail that every caller would have to know.
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                        throw new ShippingUnavailableException(
                                "The carrier answered %d.".formatted(response.getStatusCode().value()));
                    })
                    .body(ShippingEstimate.class);
            return Optional.ofNullable(estimate);
        } catch (HttpClientErrorException.NotFound notServed) {
            return Optional.empty(); // the carrier doesn't deliver to this postal code: a normal answer
        }
    }
}
