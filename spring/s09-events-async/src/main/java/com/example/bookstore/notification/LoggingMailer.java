package com.example.bookstore.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Stands in for a real email provider while you run the application locally: it only logs each email. Tests replace
 * it with a fake that records the emails.
 */
@Slf4j
@Component
class LoggingMailer implements Mailer {

    @Override
    public void send(Email email) {
        log.info("Email to {}: {}", email.to(), email.subject());
    }
}
