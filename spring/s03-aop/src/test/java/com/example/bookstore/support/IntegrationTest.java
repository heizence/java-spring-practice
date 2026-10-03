package com.example.bookstore.support;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestExecutionListeners;

/**
 * Marks a test class that runs against the whole application. It combines:
 *
 * <ul>
 *   <li>{@link SpringBootTest @SpringBootTest}: the full application context, with every bean and every aspect,
 *       without a real web server.</li>
 *   <li>{@link IntegrationTestConfiguration}: a {@link TestClock} and a {@link FakeTicker} that replace the
 *       application's clock and ticker.</li>
 *   <li>{@link IntegrationTestListener}: before each test, it puts the clock, the ticker, the in-memory coupons and
 *       the recorders ({@code MethodTimings}, {@code AuditLog}) back into their starting state. S03 has no database,
 *       so this replaces the rolled-back transaction that later modules use.</li>
 * </ul>
 *
 * <p>Every class with exactly this annotation shares one cached application context, so only the first one pays
 * for starting it. Adding {@code @MockitoBean}, {@code @Import} or other configuration to a class creates another
 * context, which costs time; avoid it unless the test needs it.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootTest
@Import(IntegrationTestConfiguration.class)
@TestExecutionListeners(listeners = IntegrationTestListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public @interface IntegrationTest {
}
