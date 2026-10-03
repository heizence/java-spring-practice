package com.example.bookstore.support;

import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * Puts the shared state back into its starting state around each test, so that tests stay independent although they
 * share one application context. Registered by {@link IntegrationTest}.
 *
 * <ul>
 *   <li>Before each test (before its {@code @BeforeEach} methods), the {@link TestClock} goes back to
 *       {@link TestClock#DEFAULT_INSTANT}.</li>
 *   <li>After each test (after its {@code @AfterEach} methods), {@link SeedData#restore()} undoes every change to the
 *       in-memory data: books a test created, changed or deleted, stock, covers.</li>
 * </ul>
 */
public class IntegrationTestListener extends AbstractTestExecutionListener {

    @Override
    public void beforeTestMethod(TestContext testContext) {
        testContext.getApplicationContext().getBeanProvider(TestClock.class).ifAvailable(TestClock::reset);
    }

    @Override
    public void afterTestMethod(TestContext testContext) {
        testContext.getApplicationContext().getBeanProvider(SeedData.class).ifAvailable(SeedData::restore);
    }
}
