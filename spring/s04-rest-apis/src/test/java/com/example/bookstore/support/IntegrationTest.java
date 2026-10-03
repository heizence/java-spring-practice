package com.example.bookstore.support;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestExecutionListeners;

/**
 * Marks a test class that runs against the whole application. It combines:
 *
 * <ul>
 *   <li>{@link SpringBootTest @SpringBootTest}: the full application context, without a real web server.</li>
 *   <li>{@link AutoConfigureMockMvc @AutoConfigureMockMvc}: inject {@code MockMvcTester} to send HTTP requests through
 *       the filters, Spring MVC and the controllers.</li>
 *   <li>{@link IntegrationTestConfiguration}: the {@link TestClock} and {@link SeedData} beans.</li>
 *   <li>{@link IntegrationTestListener}: before each test the clock goes back to
 *       {@link TestClock#DEFAULT_INSTANT}; after each test the in-memory data goes back to the seed data. So each test
 *       leaves the data as it found it, whatever it changed.</li>
 * </ul>
 *
 * <p>Every class with exactly this annotation shares one cached application context, so only the first one pays
 * for starting it. Adding {@code @MockitoBean}, {@code @TestPropertySource}, {@code @Import} or other configuration
 * to a class creates another context; avoid it unless the test needs it.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootTest
@AutoConfigureMockMvc
@Import(IntegrationTestConfiguration.class)
@TestExecutionListeners(listeners = IntegrationTestListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public @interface IntegrationTest {
}
