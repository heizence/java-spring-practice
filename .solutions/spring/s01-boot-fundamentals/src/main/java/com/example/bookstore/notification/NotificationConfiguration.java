package com.example.bookstore.notification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Chooses how the bookstore notifies members, from the configuration property
 * {@code bookstore.notifications.channel}.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>A {@code @Configuration(proxyBeanMethods = false)} class that component scanning finds. Its {@code @Bean}
 *       methods are named {@code emailNotifier}, {@code noOpNotifier} and {@code mailSender}. The notifier and mail
 *       sender classes have no {@code @Component}: this class decides which of them become beans.</li>
 *   <li>Channel {@code email} (in any letter case): the {@code Notifier} bean is an {@link EmailNotifier} that sends
 *       through the {@link MailSender} bean.</li>
 *   <li>Channel {@code none}, or no channel property at all: the {@code Notifier} bean is a {@link NoOpNotifier}.</li>
 *   <li>Any other channel, such as the typo {@code emial}, registers no {@code Notifier} at all, so that a bean that
 *       needs one stops the application at startup. A silent no-op would hide the typo until customers ask where
 *       their emails are.</li>
 *   <li>The {@code MailSender} bean is a {@link LoggingMailSender}, unless the application has a {@code MailSender}
 *       bean of its own: then this default backs off.</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
public class NotificationConfiguration {

    // havingValue compares without regard to case, so "EMAIL" matches too.
    @Bean
    @ConditionalOnProperty(prefix = "bookstore.notifications", name = "channel", havingValue = "email")
    Notifier emailNotifier(MailSender mailSender) {
        return new EmailNotifier(mailSender);
    }

    // matchIfMissing: no channel property at all counts as "none". An unknown value matches neither method, so there
    // is no Notifier, and the first bean that needs one fails at startup. @ConditionalOnMissingBean(Notifier.class)
    // would look simpler, but it would turn every typo into a silent no-op.
    @Bean
    @ConditionalOnProperty(prefix = "bookstore.notifications", name = "channel", havingValue = "none",
            matchIfMissing = true)
    Notifier noOpNotifier() {
        return new NoOpNotifier();
    }

    // Name the type to look for. Without (MailSender.class), the condition would look for beans of the method's
    // return type. That's the same here, but a return type of LoggingMailSender would back off only for another
    // LoggingMailSender, and a MailSender of your own would then meet a second one.
    //
    // In your own configuration, this condition only sees the beans registered before Spring reaches this method:
    // components found by scanning, beans a test registers, and @Bean methods of configuration classes processed
    // earlier. Auto-configurations run after all of your beans (S01-06), which is where the condition is reliable.
    @Bean
    @ConditionalOnMissingBean(MailSender.class)
    MailSender mailSender() {
        return new LoggingMailSender();
    }
}
