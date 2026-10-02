package com.example.bookstore.notification;

import static com.example.bookstore.support.ContextAssertions.assertNoBean;
import static com.example.bookstore.support.ContextAssertions.assertStartupFailed;
import static com.example.bookstore.support.ContextAssertions.singleBean;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

/**
 * Starts the mail beans the way each environment starts the application: with its environment variables, then
 * {@code application.yml} and the file of the active profile, such as {@code application-prod.yml}.
 */
@DisplayName("S02-02 · Profiles and environment-specific beans")
class MailConfigurationTest {

    private static final URI PROD_URL = URI.create("https://mail-api.example.com/v1/messages");
    private static final String API_KEY = "test-key-7f3a9c";

    /** Scans {@code com.example.bookstore.notification} like {@code @SpringBootApplication} scans the application. */
    @ComponentScan(basePackageClasses = MailConfiguration.class)
    static class NotificationPackage {
    }

    private final ApplicationContextRunner runner = startWith();

    /**
     * A runner that starts like the application on a server: the environment variables (such as
     * {@code SPRING_PROFILES_ACTIVE=prod}) are there first, then {@link ConfigDataApplicationContextInitializer} reads
     * {@code application.yml} and the files of the active profiles, as {@code SpringApplication} does. The order
     * matters: the active profile decides which files are read.
     */
    private static ApplicationContextRunner startWith(String... environmentVariables) {
        return new ApplicationContextRunner()
                .withInitializer(context -> TestPropertyValues.of(environmentVariables)
                        .applyTo(context.getEnvironment(), TestPropertyValues.Type.SYSTEM_ENVIRONMENT))
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withUserConfiguration(NotificationPackage.class);
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("in the local profile, the MailSender is the FakeMailSender")
        void localUsesTheFake() {
            startWith("SPRING_PROFILES_ACTIVE=local")
                    .run(context -> assertThat(singleBean(context, MailSender.class))
                            .isInstanceOf(FakeMailSender.class));
        }

        @Test
        @DisplayName("in the prod profile, the MailSender is the ApiMailSender, with the URL from application-prod.yml")
        void prodUsesTheApi() {
            startWith("SPRING_PROFILES_ACTIVE=prod", "BOOKSTORE_MAIL_API_KEY=" + API_KEY).run(context -> {
                MailSender sender = singleBean(context, MailSender.class);

                assertThat(sender).isInstanceOf(ApiMailSender.class);
                assertThat(((ApiMailSender) sender).endpoint()).isEqualTo(PROD_URL);
            });
        }

        @Test
        @DisplayName("the API key comes from the environment variable BOOKSTORE_MAIL_API_KEY")
        void keyFromTheEnvironment() {
            startWith("SPRING_PROFILES_ACTIVE=prod", "BOOKSTORE_MAIL_API_KEY=" + API_KEY)
                    .run(context -> assertThat(singleBean(context, MailApiProperties.class).key()).isEqualTo(API_KEY));
        }

        @Test
        @DisplayName("application-local.yml logs the bookstore's own code at DEBUG level")
        void localLogsAtDebug() {
            startWith("SPRING_PROFILES_ACTIVE=local")
                    .run(context -> assertThat(context.getEnvironment()
                            .getProperty("logging.level.com.example.bookstore"))
                            .as("logging.level.com.example.bookstore")
                            .isEqualToIgnoringCase("debug"));
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("the local profile needs no API key, and has no API settings at all")
        void localNeedsNoKey() {
            startWith("SPRING_PROFILES_ACTIVE=local").run(context -> {
                singleBean(context, FakeMailSender.class);
                assertNoBean(context, MailApiProperties.class);
            });
        }

        @Test
        @DisplayName("the profile can also come from a command-line argument: --spring.profiles.active=local")
        void profileFromTheCommandLine() {
            runner.withPropertyValues("spring.profiles.active=local")
                    .run(context -> assertThat(singleBean(context, MailSender.class))
                            .isInstanceOf(FakeMailSender.class));
        }

        @Test
        @DisplayName("without any profile, the MailSender is the ApiMailSender too: only local is fake")
        void noProfileUsesTheApi() {
            startWith("BOOKSTORE_MAIL_API_URL=" + PROD_URL, "BOOKSTORE_MAIL_API_KEY=" + API_KEY)
                    .run(context -> assertThat(singleBean(context, MailSender.class))
                            .isInstanceOf(ApiMailSender.class));
        }

        @Test
        @DisplayName("another profile, such as staging, gets the ApiMailSender too")
        void otherProfileUsesTheApi() {
            startWith("SPRING_PROFILES_ACTIVE=staging",
                    "BOOKSTORE_MAIL_API_URL=https://sandbox.mail-api.example.com/v1/messages",
                    "BOOKSTORE_MAIL_API_KEY=" + API_KEY)
                    .run(context -> assertThat(singleBean(context, MailSender.class))
                            .isInstanceOf(ApiMailSender.class));
        }

        @Test
        @DisplayName("toString() of the API settings shows the URL but hides the key")
        void toStringHidesTheKey() {
            var properties = new MailApiProperties(PROD_URL, "s3cr3t-api-key-4d2e");

            assertThat(properties.toString())
                    .contains(PROD_URL.toString())
                    .doesNotContain("s3cr3t-api-key-4d2e");
        }

        @Test
        @DisplayName("application-prod.yml sets the API URL, and no configuration file holds the API key")
        void noKeyInTheFiles() throws IOException {
            assertThat(property("application-prod.yml", "bookstore.mail.api.url"))
                    .as("bookstore.mail.api.url in application-prod.yml")
                    .isEqualTo(PROD_URL.toString());
            for (String file : List.of("application.yml", "application-local.yml", "application-prod.yml")) {
                Object key = property(file, "bookstore.mail.api.key");
                assertThat(key == null || key.toString().startsWith("${"))
                        .as("%s must not contain an API key (it says '%s'): secrets come from the environment",
                                file, key)
                        .isTrue();
            }
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("the prod profile without BOOKSTORE_MAIL_API_KEY doesn't start, and the error names the property")
        void prodWithoutKey() {
            startWith("SPRING_PROFILES_ACTIVE=prod")
                    .run(context -> assertThat(assertStartupFailed(context))
                            .rootCause()
                            .hasMessageContaining("bookstore.mail.api")
                            .hasMessageContaining("'key'")
                            .hasMessageContaining("must not be blank"));
        }

        @Test
        @DisplayName("a blank API key doesn't start either")
        void blankKey() {
            startWith("SPRING_PROFILES_ACTIVE=prod", "BOOKSTORE_MAIL_API_KEY=   ")
                    .run(context -> assertThat(assertStartupFailed(context))
                            .rootCause()
                            .hasMessageContaining("must not be blank"));
        }

        @Test
        @DisplayName("without a profile and without API settings, the application doesn't start: no silent fake")
        void noProfileNoSettings() {
            runner.run(context -> assertThat(assertStartupFailed(context))
                    .rootCause()
                    .hasMessageContaining("bookstore.mail.api"));
        }
    }

    /** A property of one configuration file on the classpath, or {@code null} if the file doesn't set it. */
    private static Object property(String file, String name) throws IOException {
        for (PropertySource<?> source : new YamlPropertySourceLoader().load(file, new ClassPathResource(file))) {
            Object value = source.getProperty(name);
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
