package practice.j02.ex05;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("J02-05 · Immutability, static factories and builders")
class HttpRequestTest {

    private static final String ORDERS = "https://shop.example/api/orders";

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("builds a request with a method, a URL and headers")
        void buildsRequest() {
            var request = HttpRequest.newBuilder("POST", ORDERS)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .build();

            assertThat(request.method()).isEqualTo("POST");
            assertThat(request.url()).isEqualTo(ORDERS);
            assertThat(request.headers()).containsOnly(
                    entry("Content-Type", "application/json"), entry("Accept", "application/json"));
        }

        @Test
        @DisplayName("a request without headers has an empty header map")
        void noHeaders() {
            var request = HttpRequest.newBuilder("GET", ORDERS).build();

            assertThat(request.headers()).isEmpty();
        }

        @Test
        @DisplayName("setting a header again replaces its value")
        void headerReplaced() {
            var request = HttpRequest.newBuilder("GET", ORDERS)
                    .header("Accept", "text/html")
                    .header("Accept", "application/json")
                    .build();

            assertThat(request.headers()).containsOnly(entry("Accept", "application/json"));
        }

        @Test
        @DisplayName("toBuilder() builds a changed copy and leaves the original as it was")
        void toBuilderMakesChangedCopy() {
            var original = HttpRequest.newBuilder("GET", ORDERS).header("Accept", "application/json").build();

            var copy = original.toBuilder()
                    .url("https://shop.example/api/orders/42")
                    .header("X-Trace-Id", "abc123")
                    .build();

            assertThat(copy.url()).isEqualTo("https://shop.example/api/orders/42");
            assertThat(copy.headers()).containsOnly(entry("Accept", "application/json"), entry("X-Trace-Id", "abc123"));
            assertThat(original.url()).isEqualTo(ORDERS);
            assertThat(original.headers()).containsOnly(entry("Accept", "application/json"));
        }

        @Test
        @DisplayName("toBuilder() keeps the method, the URL and every header")
        void toBuilderKeepsEverything() {
            var original = HttpRequest.newBuilder("PATCH", ORDERS)
                    .header("Content-Type", "application/json")
                    .header("If-Match", "\"v7\"")
                    .build();

            var copy = original.toBuilder().build();

            assertThat(copy.method()).isEqualTo("PATCH");
            assertThat(copy.url()).isEqualTo(original.url());
            assertThat(copy.headers()).isEqualTo(original.headers());
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("headers() returns a map that can't be modified")
        void headersUnmodifiable() {
            var request = HttpRequest.newBuilder("GET", ORDERS).header("Accept", "application/json").build();
            var headers = request.headers();

            assertThatThrownBy(() -> headers.put("X-Evil", "1")).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(headers::clear).isInstanceOf(UnsupportedOperationException.class);
            assertThat(request.headers()).containsOnly(entry("Accept", "application/json"));
        }

        @Test
        @DisplayName("later builder calls don't change a request that was already built")
        void builtRequestNeverChanges() {
            var builder = HttpRequest.newBuilder("GET", ORDERS).header("Accept", "application/json");
            var request = builder.build();

            builder.header("X-Trace-Id", "abc123").url("https://other.example/").method("DELETE");

            assertThat(request.method()).isEqualTo("GET");
            assertThat(request.url()).isEqualTo(ORDERS);
            assertThat(request.headers()).containsOnly(entry("Accept", "application/json"));
        }

        @Test
        @DisplayName("one builder can build several different requests")
        void builderIsReusable() {
            var builder = HttpRequest.newBuilder("GET", ORDERS);

            var first = builder.build();
            var second = builder.header("Accept", "application/json").build();

            assertThat(first.headers()).isEmpty();
            assertThat(second.headers()).containsOnly(entry("Accept", "application/json"));
        }

        @Test
        @DisplayName("changing the builder from toBuilder() doesn't change the original request")
        void toBuilderIsIndependent() {
            var original = HttpRequest.newBuilder("GET", ORDERS).header("Accept", "application/json").build();

            original.toBuilder().header("Accept", "text/html").method("DELETE").build();

            assertThat(original.method()).isEqualTo("GET");
            assertThat(original.headers()).containsOnly(entry("Accept", "application/json"));
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"GET", "POST", "PUT", "PATCH", "DELETE"})
        @DisplayName("accepts every supported method")
        void supportedMethods(String method) {
            var request = HttpRequest.newBuilder(method, ORDERS).build();

            assertThat(request.method()).isEqualTo(method);
        }

        @Test
        @DisplayName("a header value may be an empty string")
        void emptyHeaderValue() {
            var request = HttpRequest.newBuilder("GET", ORDERS).header("X-Empty", "").build();

            assertThat(request.headers()).containsOnly(entry("X-Empty", ""));
        }

        @Test
        @DisplayName("builder methods return the builder itself, so calls can be chained")
        void fluentBuilder() {
            var builder = HttpRequest.newBuilder("GET", ORDERS);

            assertThat(builder.method("POST")).isSameAs(builder);
            assertThat(builder.url(ORDERS)).isSameAs(builder);
            assertThat(builder.header("Accept", "application/json")).isSameAs(builder);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("newBuilder() rejects a null method or URL")
        void newBuilderRejectsNull() {
            assertThatThrownBy(() -> HttpRequest.newBuilder(null, ORDERS)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> HttpRequest.newBuilder("GET", null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("builder methods reject null")
        void builderRejectsNull() {
            var builder = HttpRequest.newBuilder("GET", ORDERS);

            assertThatThrownBy(() -> builder.method(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> builder.url(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> builder.header(null, "x")).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> builder.header("Accept", null)).isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"get", "FETCH", "CONNECT", "", " GET"})
        @DisplayName("build() rejects an unsupported method")
        void unsupportedMethod(String method) {
            var builder = HttpRequest.newBuilder(method, ORDERS);

            assertThatThrownBy(builder::build)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(method);
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"", " ", "\t"})
        @DisplayName("build() rejects a blank header name")
        void blankHeaderName(String name) {
            var builder = HttpRequest.newBuilder("GET", ORDERS).header(name, "x");

            assertThatThrownBy(builder::build).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("an invalid value set through toBuilder() is rejected by build() too")
        void toBuilderValidates() {
            var original = HttpRequest.newBuilder("GET", ORDERS).build();

            var builder = original.toBuilder().method("TRACE");

            assertThatThrownBy(builder::build).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
