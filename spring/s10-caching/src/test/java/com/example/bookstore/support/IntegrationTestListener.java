package com.example.bookstore.support;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * Puts the shared test helpers back into their starting state around each test, so that tests stay independent
 * although they share one application context. Registered by {@link IntegrationTest}.
 *
 * <ul>
 *   <li>Before each test (before its {@code @BeforeEach} methods), the {@link TestClock} goes back to
 *       {@link TestClock#DEFAULT_INSTANT}, the {@link FakeTicker} goes back to 0, and every cache is emptied. Caches
 *       live outside the database, so the rollback at the end of a test doesn't undo what a test put into them: a
 *       price that a test changed would otherwise stay in the cache for the next test.</li>
 *   <li>Right before each test method (after its {@code @BeforeEach} methods), the {@link SqlStatementCounter}
 *       forgets earlier statements.</li>
 * </ul>
 */
public class IntegrationTestListener extends AbstractTestExecutionListener {

    @Override
    public void beforeTestMethod(TestContext testContext) {
        ApplicationContext context = applicationContext(testContext);
        context.getBeanProvider(TestClock.class).ifAvailable(TestClock::reset);
        context.getBeanProvider(FakeTicker.class).ifAvailable(FakeTicker::reset);
        context.getBeanProvider(CacheManager.class).ifAvailable(IntegrationTestListener::clearCaches);
    }

    @Override
    public void beforeTestExecution(TestContext testContext) {
        applicationContext(testContext).getBeanProvider(SqlStatementCounter.class)
                .ifAvailable(SqlStatementCounter::reset);
    }

    private static void clearCaches(CacheManager cacheManager) {
        for (String name : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        }
    }

    private static ApplicationContext applicationContext(TestContext testContext) {
        return testContext.getApplicationContext();
    }
}
