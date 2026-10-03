package com.example.bookstore.notification;

import lombok.extern.slf4j.Slf4j;

/**
 * The default {@link MailSender} until the bookstore connects to a real mail provider: it writes each mail's subject
 * to the log instead of sending the mail. Handy on a developer's machine; useless in production.
 */
@Slf4j
public class LoggingMailSender implements MailSender {

    @Override
    public void send(Mail mail) {
        // The subject only: an email address is personal data, which doesn't belong in logs.
        log.info("Not sending mail '{}': LoggingMailSender only logs mails.", mail.subject());
    }
}
