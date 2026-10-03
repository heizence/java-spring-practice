package com.example.bookstore.notification;

import static com.example.bookstore.support.ContextAssertions.assertNoBean;
import static com.example.bookstore.support.ContextAssertions.assertStartupFailed;
import static com.example.bookstore.support.ContextAssertions.singleBean;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;

/**
 * Starts small application contexts with different properties, the way the application starts in different
 * environments, and checks which beans the conditions let in.
 */
@DisplayName("S01-05 · Conditional beans")
class NotificationConfigurationTest {

    private static final String CHANNEL = "bookstore.notifications.channel";

    /** Scans {@code com.example.bookstore.notification}, which holds one bean source: NotificationConfiguration. */
    @ComponentScan(basePackageClasses = NotificationConfiguration.class)
    static class NotificationPackage {
    }

    /** A MailSender that keeps the mails instead of sending them: the application's "own" MailSender. */
    static class RecordingMailSender implements MailSender {

        final List<Mail> sent = new ArrayList<>();

        @Override
        public void send(Mail mail) {
            sent.add(mail);
        }
    }

    /** A bean that needs a Notifier, as an order service would. */
    record NeedsANotifier(Notifier notifier) {
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(NotificationPackage.class);

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("channel \"email\": the Notifier is an EmailNotifier")
        void emailChannel() {
            runner.withPropertyValues(CHANNEL + "=email")
                    .run(context -> assertThat(singleBean(context, Notifier.class)).isInstanceOf(EmailNotifier.class));
        }

        @Test
        @DisplayName("channel \"none\": the Notifier is a NoOpNotifier")
        void noneChannel() {
            runner.withPropertyValues(CHANNEL + "=none")
                    .run(context -> assertThat(singleBean(context, Notifier.class)).isInstanceOf(NoOpNotifier.class));
        }

        @Test
        @DisplayName("no channel property at all: the Notifier is a NoOpNotifier")
        void noChannelProperty() {
            runner.run(context -> assertThat(singleBean(context, Notifier.class)).isInstanceOf(NoOpNotifier.class));
        }

        @Test
        @DisplayName("the EmailNotifier sends through the application's MailSender bean")
        void emailNotifierUsesTheMailSenderBean() {
            var mailSender = new RecordingMailSender();

            runner.withPropertyValues(CHANNEL + "=email")
                    .withBean(MailSender.class, () -> mailSender)
                    .run(context -> {
                        Notifier notifier = singleBean(context, Notifier.class);

                        notifier.orderPlaced("minji@example.com", 201, 72_000);

                        assertThat(mailSender.sent).singleElement().satisfies(mail -> {
                            assertThat(mail.to()).isEqualTo("minji@example.com");
                            assertThat(mail.subject()).isEqualTo("Your bookstore order 201");
                        });
                    });
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("a MailSender bean of the application's own replaces the default LoggingMailSender")
        void ownMailSenderWins() {
            var mailSender = new RecordingMailSender();

            runner.withPropertyValues(CHANNEL + "=email")
                    .withBean(MailSender.class, () -> mailSender)
                    .run(context -> {
                        assertThat(singleBean(context, Notifier.class)).isInstanceOf(EmailNotifier.class);

                        assertThat(singleBean(context, MailSender.class)).isSameAs(mailSender);
                    });
        }

        @Test
        @DisplayName("without a MailSender of the application's own, the MailSender is a LoggingMailSender")
        void defaultMailSender() {
            runner.withPropertyValues(CHANNEL + "=email")
                    .run(context -> assertThat(singleBean(context, MailSender.class))
                            .isInstanceOf(LoggingMailSender.class));
        }

        @Test
        @DisplayName("accepts the channel in capital letters")
        void channelInCapitals() {
            runner.withPropertyValues(CHANNEL + "=EMAIL")
                    .run(context -> assertThat(singleBean(context, Notifier.class)).isInstanceOf(EmailNotifier.class));
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("an unknown channel registers no Notifier, so a bean that needs one stops the application")
        void unknownChannel() {
            // The configuration is at work (its MailSender is there), but it lets no Notifier in...
            runner.withPropertyValues(CHANNEL + "=emial").run(context -> {
                singleBean(context, MailSender.class);
                assertNoBean(context, Notifier.class);
            });

            // ...so the first bean that needs one stops the application at startup.
            runner.withPropertyValues(CHANNEL + "=emial")
                    .withBean(NeedsANotifier.class)
                    .run(context -> assertThat(assertStartupFailed(context))
                            .hasMessageContaining(Notifier.class.getName()));
        }
    }
}
