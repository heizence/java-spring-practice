package com.example.bookstore;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import com.example.bookstore.book.BookCatalog;
import com.example.bookstore.book.CatalogStartupReport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationHook;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Starts the bookstore the way the {@code java} command does, through {@link BookstoreApplication#main}, and looks
 * inside the application context that {@code main} started. Each test starts and closes its own application.
 *
 * <p>The other S01 exercises don't start the whole application: they test their own beans in small contexts, so
 * they don't depend on this exercise.
 */
@DisplayName("S01-01 · Create a Spring Boot application")
class BookstoreApplicationTest {

    private static final String CATALOG_SUMMARY = "The catalog holds 60 books in 6 categories.";

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("main() starts a Spring application and leaves it running")
        void mainStartsTheApplication() {
            try (var context = startThroughMain()) {
                assertThat(context.isActive()).as("the application context is running").isTrue();
            }
        }

        @Test
        @DisplayName("component scanning finds the beans in com.example.bookstore and its sub-packages")
        void componentScanningFindsTheBeans() {
            try (var context = startThroughMain()) {
                assertThat(context.getBeanNamesForType(BookCatalog.class))
                        .as("BookCatalog beans (component scanning starts at @SpringBootApplication's package)")
                        .hasSize(1);
                assertThat(context.getBeanNamesForType(CatalogStartupReport.class))
                        .as("CatalogStartupReport beans")
                        .hasSize(1);
            }
        }

        @Test
        @DisplayName("auto-configuration adds beans that the bookstore doesn't declare, such as a task executor")
        void autoConfigurationAddsBeans() {
            try (var context = startThroughMain()) {
                assertThat(context.containsBean("applicationTaskExecutor"))
                        .as("Spring Boot auto-configures a bean named 'applicationTaskExecutor' when "
                                + "@EnableAutoConfiguration (part of @SpringBootApplication) is on")
                        .isTrue();
            }
        }

        @Test
        @DisplayName("application.yml names the application \"bookstore\"")
        void namesTheApplication() {
            try (var context = startThroughMain()) {
                assertThat(context.getEnvironment().getProperty("spring.application.name"))
                        .as("spring.application.name")
                        .isEqualTo("bookstore");
            }
        }

        @Test
        @ExtendWith(OutputCaptureExtension.class)
        @DisplayName("logs how many books and categories the catalog holds")
        void logsTheCatalogSummary(CapturedOutput output) {
            try (var context = startThroughMain()) {
                assertThat(output.getOut()).contains(CATALOG_SUMMARY);
            }
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("application.yml makes it a non-web application")
        void isNotAWebApplication() {
            try (var context = startThroughMain()) {
                assertThat(context.getEnvironment().getProperty("spring.main.web-application-type"))
                        .as("spring.main.web-application-type")
                        .isEqualToIgnoringCase("none");
            }
        }

        @Test
        @DisplayName("main() passes its command-line arguments on, so they override application.yml")
        void passesTheCommandLineArguments() {
            try (var context = startThroughMain("--spring.application.name=bookstore-from-the-command-line")) {
                assertThat(context.getEnvironment().getProperty("spring.application.name"))
                        .as("spring.application.name, set by a command-line argument")
                        .isEqualTo("bookstore-from-the-command-line");
            }
        }

        @Test
        @ExtendWith(OutputCaptureExtension.class)
        @DisplayName("logs the summary after the application has started, as an ApplicationRunner")
        void logsTheSummaryAfterStartup(CapturedOutput output) {
            try (var context = startThroughMain()) {
                String log = output.getOut();
                int started = log.indexOf("Started BookstoreApplication");
                int summary = log.indexOf(CATALOG_SUMMARY);

                assertThat(started)
                        .as("position of Spring Boot's \"Started BookstoreApplication\" line")
                        .isNotNegative();
                assertThat(summary)
                        .as("position of the catalog summary: an ApplicationRunner runs after \"Started ...\", "
                                + "a @PostConstruct method before it")
                        .isGreaterThan(started);
            }
        }
    }

    /**
     * Calls {@code BookstoreApplication.main(args)} and returns the application context that it started. A
     * {@link SpringApplicationHook} catches the context, which {@code main} itself doesn't return; it's the mechanism
     * behind {@code @SpringBootTest(useMainMethod = ALWAYS)}. The caller closes the context.
     */
    private static ConfigurableApplicationContext startThroughMain(String... args) {
        var started = new AtomicReference<ConfigurableApplicationContext>();
        SpringApplicationHook catchTheContext = application -> new SpringApplicationRunListener() {
            @Override
            public void started(ConfigurableApplicationContext context, Duration timeTaken) {
                started.set(context);
            }
        };

        SpringApplication.withHook(catchTheContext, () -> BookstoreApplication.main(args));

        assertThat(started.get())
                .as("main() should start the application with SpringApplication.run(BookstoreApplication.class, args)")
                .isNotNull();
        return started.get();
    }
}
