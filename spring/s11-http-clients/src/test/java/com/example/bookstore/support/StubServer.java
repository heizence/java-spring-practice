package com.example.bookstore.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * A real HTTP server on this computer that plays a remote service in tests. It listens on {@code 127.0.0.1} on a free
 * port, answers each path with the responses a test gave it, and records every request it receives.
 *
 * <pre>{@code
 * StubServer warehouse = StubServer.start();
 * warehouse.respond("/v1/stock/9791115881394",
 *         StubResponse.status(503),
 *         StubResponse.json(200, """
 *                 { "isbn": "9791115881394", "available": 12 }"""));
 * // first request: 503; second and every later request: 200
 *
 * assertThat(warehouse.requests()).hasSize(2);
 * }</pre>
 *
 * <p>Unlike a mock of the HTTP client, a real server shows what really happens on the network: timeouts, retries
 * and how many requests arrive at the same time. Each request runs on a virtual thread of its own, so the server
 * can hold many requests at once (see {@link #maxConcurrentRequests()}). A response with a delay waits until the
 * delay has passed or until the test calls {@link #reset()} or {@link #close()}, so a test never leaves a request
 * hanging.
 */
public final class StubServer implements AutoCloseable {

    private final HttpServer server;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final Map<String, Deque<StubResponse>> responses = new ConcurrentHashMap<>();
    private final List<RecordedRequest> requests = new CopyOnWriteArrayList<>();
    private final AtomicInteger inFlight = new AtomicInteger();
    private final AtomicInteger maxInFlight = new AtomicInteger();
    /** Counted down to end every delay early; replaced by {@link #reset()}. */
    private final AtomicReference<CountDownLatch> release = new AtomicReference<>(new CountDownLatch(1));

    private StubServer() {
        try {
            server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        server.setExecutor(executor);
        server.createContext("/", this::handle);
    }

    /** Starts a server on a free port. Close it when the tests are done. */
    public static StubServer start() {
        var stub = new StubServer();
        stub.server.start();
        return stub;
    }

    /** Where the server listens, such as {@code http://127.0.0.1:52811}. */
    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    /**
     * Answers requests for this path (without the query) with these responses, one after another. The last one
     * answers every further request. A path without responses answers {@code 404}.
     */
    public void respond(String path, StubResponse... responses) {
        if (responses.length == 0) {
            throw new IllegalArgumentException("Give at least one response for " + path);
        }
        this.responses.put(path, new ArrayDeque<>(List.of(responses)));
    }

    /** Every request received since the start or the last {@link #reset()}, oldest first. */
    public List<RecordedRequest> requests() {
        return List.copyOf(requests);
    }

    /** The highest number of requests that the server was working on at the same time. */
    public int maxConcurrentRequests() {
        return maxInFlight.get();
    }

    /** Ends all delays, and forgets the responses, the requests and the concurrency count. */
    public void reset() {
        release.getAndSet(new CountDownLatch(1)).countDown();
        responses.clear();
        requests.clear();
        maxInFlight.set(0);
    }

    /** Ends all delays and stops the server. */
    @Override
    public void close() {
        release.get().countDown();
        server.stop(0);
        executor.shutdownNow();
    }

    private void handle(HttpExchange exchange) throws IOException {
        long receivedAt = System.nanoTime();
        CountDownLatch releaseNow = release.get();
        int concurrent = inFlight.incrementAndGet();
        maxInFlight.accumulateAndGet(concurrent, Math::max);
        boolean counted = true;
        try (InputStream body = exchange.getRequestBody()) {
            body.readAllBytes();
            String path = exchange.getRequestURI().getPath();
            requests.add(new RecordedRequest(exchange.getRequestMethod(), path, exchange.getRequestURI().getQuery(),
                    Map.copyOf(exchange.getRequestHeaders()), receivedAt));

            StubResponse response = nextResponse(path);
            if (!response.delay().isZero()) {
                releaseNow.await(response.delay().toMillis(), TimeUnit.MILLISECONDS);
            }
            // Stop counting before answering: once the client has the answer, it may send its next request at once.
            inFlight.decrementAndGet();
            counted = false;
            byte[] bytes = response.body().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(response.status(), bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(bytes);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (counted) {
                inFlight.decrementAndGet();
            }
            exchange.close();
        }
    }

    private StubResponse nextResponse(String path) {
        Deque<StubResponse> queue = responses.get(path);
        if (queue == null) {
            return StubResponse.json(404, """
                    { "error": "No stub response for %s" }""".formatted(path));
        }
        synchronized (queue) {
            return queue.size() > 1 ? queue.removeFirst() : queue.getFirst();
        }
    }

    /**
     * What the server answers.
     *
     * @param body  sent as {@code application/json}; empty for no body
     * @param delay how long the server waits before it answers
     */
    public record StubResponse(int status, String body, Duration delay) {

        public StubResponse {
            Objects.requireNonNull(body, "body");
            Objects.requireNonNull(delay, "delay");
        }

        /** A JSON response without delay. */
        public static StubResponse json(int status, String body) {
            return new StubResponse(status, body, Duration.ZERO);
        }

        /** A response with a small JSON error body, such as {@code 503}. */
        public static StubResponse status(int status) {
            return json(status, """
                    { "error": "status %d" }""".formatted(status));
        }

        /** The same response, sent after a delay. */
        public StubResponse delayedBy(Duration delay) {
            return new StubResponse(status, body, delay);
        }
    }

    /**
     * A request that the server received.
     *
     * @param query           the decoded query string, such as {@code author=Joshua Bloch&limit=5}, or {@code null}
     * @param headers         the request headers; the server writes their names with a capital first letter
     * @param receivedAtNanos when the request arrived, from {@link System#nanoTime()}
     */
    public record RecordedRequest(String method, String path, String query, Map<String, List<String>> headers,
                                  long receivedAtNanos) {

        /** The first value of a header, or {@code null}. Header names are compared without regard to case. */
        public String header(String name) {
            return headers.entrySet().stream()
                    .filter(header -> header.getKey().equalsIgnoreCase(name))
                    .map(header -> header.getValue().getFirst())
                    .findFirst()
                    .orElse(null);
        }
    }
}
