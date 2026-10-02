package com.example.bookstore.notification;

import java.net.URI;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Sends mail through the mail provider's HTTP API, authenticated with the API key from {@link MailApiProperties}.
 *
 * <p>This module has no HTTP client yet; S11 adds the real call. Until then, {@link #send} logs the request it would
 * make, without the key.
 */
@Slf4j
@RequiredArgsConstructor
public class ApiMailSender implements MailSender {

    private final MailApiProperties properties;

    @Override
    public void send(Mail mail) {
        log.info("POST {} for mail '{}'", properties.url(), mail.subject());
    }

    /** Where this sender sends its mail. */
    public URI endpoint() {
        return properties.url();
    }
}
