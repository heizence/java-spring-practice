package com.example.bookstore.support;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.transaction.annotation.Transactional;

/**
 * Marks a test class that runs against the whole application and a real MySQL database. It combines:
 *
 * <ul>
 *   <li>{@link SpringBootTest @SpringBootTest}: the full application context, without a real web server.</li>
 *   <li>{@link AutoConfigureMockMvc @AutoConfigureMockMvc}: inject {@code MockMvcTester} to send HTTP requests to
 *       the controllers.</li>
 *   <li>{@link TestcontainersConfiguration}: MySQL 9.7 in Docker, one container for the whole test JVM.</li>
 *   <li>{@link IntegrationTestConfiguration}: {@link TestClock}, {@link SqlStatementCounter} and {@link SeedData}
 *       beans, put back into their starting state for each test by {@link IntegrationTestListener}.</li>
 *   <li>{@link Transactional @Transactional}: each test runs in a transaction that is rolled back at its end, so it
 *       leaves the database as it found it. MockMvc requests run in that transaction too, because MockMvc calls the
 *       controller on the test's thread ({@link TestThreadGuard} checks that the test methods run on that
 *       thread).</li>
 * </ul>
 *
 * <p>Every class with exactly this annotation shares one cached application context, so only the first one pays
 * for starting it. Adding {@code @MockitoBean}, {@code @TestPropertySource} or other configuration to a class
 * creates another context, which costs seconds; avoid it unless the test needs it.
 *
 * <p>A test that must commit (concurrent requests, after-commit listeners) opts out of the rollback with
 * {@code @Transactional(propagation = Propagation.NOT_SUPPORTED)} on the class or method, and restores the data it
 * changed: see {@link SeedData}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, IntegrationTestConfiguration.class})
@TestExecutionListeners(listeners = IntegrationTestListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@Transactional
@ExtendWith(TestThreadGuard.class)
public @interface IntegrationTest {
}
