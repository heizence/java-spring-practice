package com.example.bookstore.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * The test helpers that {@link IntegrationTest} adds to the application context. They are beans, so each
 * application context has its own and tests get them with {@code @Autowired}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class IntegrationTestConfiguration {

    /** Replaces the application's clock wherever a {@code Clock} is injected. */
    @Bean
    @Primary
    TestClock testClock() {
        return new TestClock();
    }

    /** Replaces the application's ticker wherever a {@code Ticker} is injected. */
    @Bean
    @Primary
    FakeTicker fakeTicker() {
        return new FakeTicker();
    }
}
