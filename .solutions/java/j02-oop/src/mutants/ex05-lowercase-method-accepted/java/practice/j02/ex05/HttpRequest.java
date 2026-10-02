package practice.j02.ex05;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
 *         <li>a header name is blank. A header value may be any string, even an empty one.</li>
 *       </ul>
 *       The URL is kept exactly as it was given. To keep the exercise small, this class doesn't check it.</li>
 *   <li>Setting a header again replaces its value. Header names are compared exactly, letter case included (a
 *       simplification: real HTTP treats {@code Content-Type} and {@code content-type} as the same header).</li>
 *   <li>A request never changes after {@code build()}: later calls on the builder don't affect it, and
 *       {@link #headers()} returns a map that can't be modified. One builder can build several requests.</li>
 *   <li>{@link #toBuilder()} returns a new builder with this request's method, URL and headers. Changing that
 *       builder doesn't change this request.</li>
 * </ul>
 */
public final class HttpRequest {

    private static final Set<String> METHODS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE");

    private final String method;
    private final String url;
    private final Map<String, String> headers;

    // Only Builder.build() calls this, with values it has already checked.
    private HttpRequest(String method, String url, Map<String, String> headers) {
        this.method = method;
        this.url = url;
        // Map.copyOf makes an unmodifiable COPY. Collections.unmodifiableMap(headers) would only be a read-only
        // VIEW of the builder's map: a later builder.header(...) call would show up in this "immutable" request.
        this.headers = Map.copyOf(headers);
    }

    /** A new builder for a request with the given method and URL. Both are required. */
    public static Builder newBuilder(String method, String url) {
        return new Builder(method, url);
    }

    /** The method, such as {@code "GET"}. */
    public String method() {
        return method;
    }

    /** The URL. */
    public String url() {
        return url;
    }

    /** The headers, as a map that can't be modified. Empty if there are none. */
    public Map<String, String> headers() {
        return headers; // safe to return: the map is unmodifiable
    }

    /** A new builder with this request's values, for building a changed copy. */
    public Builder toBuilder() {
        // The builder gets its own mutable copy of the headers: our map is unmodifiable, and must stay unshared.
        var builder = new Builder(method, url);
        builder.headers.putAll(headers);
        return builder;
    }

    /**
     * Collects the values for an {@link HttpRequest}. Each method returns the builder itself, so calls can be
     * chained. A static nested class: it needs no outer {@code HttpRequest} object, because it creates one.
     */
    public static final class Builder {

        private String method;
        private String url;
        private final Map<String, String> headers = new LinkedHashMap<>();

        private Builder(String method, String url) {
            method(method);
            url(url);
        }

        /** Sets the method. */
        public Builder method(String method) {
            this.method = Objects.requireNonNull(method, "method");
            return this;
        }

        /** Sets the URL. */
        public Builder url(String url) {
            this.url = Objects.requireNonNull(url, "url");
            return this;
        }

        /** Adds a header, or replaces the value of a header with the same name. */
        public Builder header(String name, String value) {
            headers.put(Objects.requireNonNull(name, "name"), Objects.requireNonNull(value, "value"));
            return this;
        }

        /** Checks every value and creates the request. */
        public HttpRequest build() {
            // All checks happen here, in one place, before the immutable object exists.
            if (!METHODS.contains(method.toUpperCase(Locale.ROOT))) {
                throw new IllegalArgumentException("unsupported HTTP method: \"" + method + "\"");
            }
            for (String name : headers.keySet()) {
                if (name.isBlank()) {
                    throw new IllegalArgumentException("header name must not be blank, but was \"" + name + "\"");
                }
            }
            return new HttpRequest(method, url, headers);
        }
    }
}
