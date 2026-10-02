package com.example.bookstore.notification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Mutant: no matchIfMissing, so without a channel property there is no Notifier at all. */
@Configuration(proxyBeanMethods = false)
public class NotificationConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "bookstore.notifications", name = "channel", havingValue = "email")
    Notifier emailNotifier(MailSender mailSender) {
        return new EmailNotifier(mailSender);
    }

    @Bean
    @ConditionalOnProperty(prefix = "bookstore.notifications", name = "channel", havingValue = "none")
    Notifier noOpNotifier() {
        return new NoOpNotifier();
    }

    @Bean
    @ConditionalOnMissingBean(MailSender.class)
    MailSender mailSender() {
        return new LoggingMailSender();
    }
}
