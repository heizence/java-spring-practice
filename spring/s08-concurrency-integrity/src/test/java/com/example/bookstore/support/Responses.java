package com.example.bookstore.support;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.jayway.jsonpath.JsonPath;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import practice.testkit.Concurrently;

/**
 * The responses to requests that many clients sent at the same moment.
 *
 * <pre>{@code
 * Responses responses = Responses.sendAtOnce(40, i -> mvc.post().uri("/api/orders")…exchange());
 *
 * assertThat(responses.countByOutcome()).isEqualTo(Map.of("201", 10L, "409 OUT_OF_STOCK", 30L));
 * }</pre>
 *
 * @param all the responses, in the order of the requests' numbers
 */
public record Responses(List<MvcTestResult> all) {

    /**
     * Sends {@code count} requests from {@code count} threads that start at the same moment, and waits until all of
     * them have answered. The threads are daemon threads, and the whole burst fails after 8 seconds (see
     * {@link Concurrently}).
     *
     * @param request sends request number {@code i} (0 to {@code count - 1}) and returns its result
     */
    public static Responses sendAtOnce(int count, IntFunction<MvcTestResult> request) {
        var results = new AtomicReferenceArray<MvcTestResult>(count);
        Concurrently.run(count, i -> results.set(i, request.apply(i)));
        return new Responses(IntStream.range(0, count).mapToObj(results::get).toList());
    }

    /**
     * How many responses had each outcome: the status, followed by the problem detail's {@code code} for errors. For
     * example {@code {201=10, 409 OUT_OF_STOCK=30}}.
     */
    public Map<String, Long> countByOutcome() {
        return all.stream()
                .collect(Collectors.groupingBy(Responses::outcome, TreeMap::new, Collectors.counting()));
    }

    /** The responses with this status, such as {@code 201}. */
    public List<MvcTestResult> withStatus(int status) {
        return all.stream().filter(result -> result.getResponse().getStatus() == status).toList();
    }

    /** {@code "201"} for a success, {@code "409 OUT_OF_STOCK"} for a problem detail. */
    public static String outcome(MvcTestResult result) {
        if (result.getUnresolvedException() != null) {
            return "exception " + result.getUnresolvedException();
        }
        int status = result.getResponse().getStatus();
        if (status < 400) {
            return String.valueOf(status);
        }
        try {
            return status + " " + JsonPath.read(body(result), "$.code");
        } catch (RuntimeException notAProblemDetail) {
            return status + " " + body(result);
        }
    }

    /** The response body as text. */
    public static String body(MvcTestResult result) {
        return new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
    }
}
