package com.example.bookstore.support;

import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * Puts the shared test helpers back into their starting state around each test, so that tests stay independent
 * although they share one application context. Registered by {@link IntegrationTest}.
 *
 * <ul>
 *   <li>Before each test (before its {@code @BeforeEach} methods), the {@link TestClock} goes back to
 *       {@link TestClock#DEFAULT_INSTANT}.</li>
 *   <li>Right before each test method (after its {@code @BeforeEach} methods), the {@link SqlStatementCounter}
 *       forgets earlier statements.</li>
 * </ul>
 */
public class IntegrationTestListener extends AbstractTestExecutionListener {

    @Override
    public void beforeTestMethod(TestContext testContext) {
        applicationContext(testContext).getBeanProvider(TestClock.class).ifAvailable(TestClock::reset);
    }

    @Override
    public void beforeTestExecution(TestContext testContext) {
        applicationContext(testContext).getBeanProvider(SqlStatementCounter.class)
                .ifAvailable(SqlStatementCounter::reset);
    }

    private static ApplicationContext applicationContext(TestContext testContext) {
        return testContext.getApplicationContext();
    }
}
