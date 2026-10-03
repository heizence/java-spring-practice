package com.example.bookstore.support;

import com.example.bookstore.common.audit.AuditLog;
import com.example.bookstore.common.timing.MethodTimings;
import com.example.bookstore.coupon.CouponRepository;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * Puts the application back into its starting state around each test, so that tests stay independent although they
 * share one application context. Registered by {@link IntegrationTest}.
 *
 * <ul>
 *   <li>Before each test (before its {@code @BeforeEach} methods): the {@link TestClock} goes back to
 *       {@link TestClock#DEFAULT_INSTANT}, the {@link FakeTicker} to 0, and the coupons to the seed data.</li>
 *   <li>Right before each test method (after its {@code @BeforeEach} methods): {@link MethodTimings} and
 *       {@link AuditLog} forget what they recorded, so a test sees only what its own method did.</li>
 * </ul>
 */
public class IntegrationTestListener extends AbstractTestExecutionListener {

    @Override
    public void beforeTestMethod(TestContext testContext) {
        ApplicationContext context = testContext.getApplicationContext();
        context.getBeanProvider(TestClock.class).ifAvailable(TestClock::reset);
        context.getBeanProvider(FakeTicker.class).ifAvailable(FakeTicker::reset);
        context.getBeanProvider(CouponRepository.class).ifAvailable(CouponRepository::restoreSeedData);
    }

    @Override
    public void beforeTestExecution(TestContext testContext) {
        ApplicationContext context = testContext.getApplicationContext();
        context.getBeanProvider(MethodTimings.class).ifAvailable(MethodTimings::clear);
        context.getBeanProvider(AuditLog.class).ifAvailable(AuditLog::clear);
    }
}
