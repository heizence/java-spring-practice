package com.example.bookstore.order;

import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * S13-01 · Write tests: a controller slice.
 *
 * <p>Write tests that pin down the HTTP contract of {@link OrderController}, as its Javadoc describes it: statuses,
 * headers, JSON bodies, what reaches the service, and the error responses. Your tests run against several broken
 * versions of the order API; a broken version is <em>caught</em> when at least one of your tests fails on it. Run
 * {@code ./score s13-01} to see how many you catch.
 *
 * <p>This is a web slice: Spring starts only the web layer (the controller, JSON, validation and
 * {@code GlobalExceptionHandler}), without a database. {@link OrderService} is a Mockito mock: stub what it returns,
 * and verify what it receives.
 *
 * <p>Structure your tests like the ones in the other exercises: {@code @Nested} classes for "Basic", "Edge cases" and
 * "Errors", a {@code @DisplayName} on every test, given/when/then inside. Don't open {@code src/mutants} until you've
 * caught them all: it's the answer key.
 */
@WebMvcTest(OrderController.class)
@DisplayName("S13-01 · Write tests: a controller slice")
class OrderControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private OrderService orderService;

    // TODO: S13-01 — write your tests here.
}
