package practice.j02.ex05;

import java.net.URI;
import java.util.Map;

/**
 * An immutable HTTP request: a method, a URL and headers. Build one with a {@link Builder}:
 *
 * <pre>{@code
 * HttpRequest request = HttpRequest.newBuilder("POST", "https://shop.example/api/orders")
 *         .header("Content-Type", "application/json")
 *         .header("Accept", "application/json")
 *         .build();
 * }</pre>
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>The method and the URL are required: {@link #newBuilder} takes both. Every builder method throws
 *       {@link NullPointerException} for a {@code null} argument.</li>
 *   <li>{@link Builder#build()} checks every value and throws {@link IllegalArgumentException}, with the bad value
 *       in the message, when
 *       <ul>
 *         <li>the method isn't one of {@code GET}, {@code POST}, {@code PUT}, {@code PATCH} or {@code DELETE}
 *             (uppercase, exactly);</li>
 *         <li>the URL isn't an absolute URL with the scheme {@code http} or {@code https} (in any letter case)
 *             and a host, such as {@code https://shop.example/api/orders};</li>
 *         <li>a header name is blank. A header value may be any string, even an empty one.</li>
 *       </ul></li>
 *   <li>Setting a header again replaces its value. Header names are compared exactly, letter case included (a
 *       simplification: real HTTP treats {@code Content-Type} and {@code content-type} as the same header).</li>
 *   <li>A request never changes after {@code build()}: later calls on the builder don't affect it, and
 *       {@link #headers()} returns a map that can't be modified. One builder can build several requests.</li>
 *   <li>{@link #toBuilder()} returns a new builder with this request's method, URL and headers. Changing that
 *       builder doesn't change this request.</li>
 * </ul>
 */
public final class HttpRequest {

    // Private: only Builder.build() creates requests. Give it the parameters and the body you need.
    private HttpRequest() {
        // TODO: J02-05
        throw new UnsupportedOperationException("TODO: J02-05");
    }

    /** A new builder for a request with the given method and URL. Both are required. */
    public static Builder newBuilder(String method, String url) {
        // TODO: J02-05
        throw new UnsupportedOperationException("TODO: J02-05");
    }

    /** The method, such as {@code "GET"}. */
    public String method() {
        // TODO: J02-05
        throw new UnsupportedOperationException("TODO: J02-05");
    }

    /** The URL. */
    public URI uri() {
        // TODO: J02-05
        throw new UnsupportedOperationException("TODO: J02-05");
    }

    /** The headers, as a map that can't be modified. Empty if there are none. */
    public Map<String, String> headers() {
        // TODO: J02-05
        throw new UnsupportedOperationException("TODO: J02-05");
    }

    /** A new builder with this request's values, for building a changed copy. */
    public Builder toBuilder() {
        // TODO: J02-05
        throw new UnsupportedOperationException("TODO: J02-05");
    }

    /**
     * Collects the values for an {@link HttpRequest}. Each method returns the builder itself, so calls can be
     * chained. A static nested class: it needs no outer {@code HttpRequest} object, because it creates one.
     */
    public static final class Builder {

        private Builder(String method, String url) {
            // TODO: J02-05
            throw new UnsupportedOperationException("TODO: J02-05");
        }

        /** Sets the method. */
        public Builder method(String method) {
            // TODO: J02-05
            throw new UnsupportedOperationException("TODO: J02-05");
        }

        /** Sets the URL. */
        public Builder url(String url) {
            // TODO: J02-05
            throw new UnsupportedOperationException("TODO: J02-05");
        }

        /** Adds a header, or replaces the value of a header with the same name. */
        public Builder header(String name, String value) {
            // TODO: J02-05
            throw new UnsupportedOperationException("TODO: J02-05");
        }

        /** Checks every value and creates the request. */
        public HttpRequest build() {
            // TODO: J02-05
            throw new UnsupportedOperationException("TODO: J02-05");
        }
    }
}
