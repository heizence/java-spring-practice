package com.example.bookstore.notification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Mutant: a bare @ConditionalOnMissingBean on a method that returns LoggingMailSender looks for LoggingMailSender
 * beans only, so the default doesn't back off for another kind of MailSender.
 */
@Configuration(proxyBeanMethods = false)
public class NotificationConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "bookstore.notifications", name = "channel", havingValue = "email")
    Notifier emailNotifier(MailSender mailSender) {
        return new EmailNotifier(mailSender);
    }

    @Bean
    @ConditionalOnProperty(prefix = "bookstore.notifications", name = "channel", havingValue = "none",
            matchIfMissing = true)
    Notifier noOpNotifier() {
        return new NoOpNotifier();
    }

    @Bean
    @ConditionalOnMissingBean
    LoggingMailSender mailSender() {
        return new LoggingMailSender();
    }
}
