package com.example.bookstore.notification;

import java.util.Objects;

/**
 * An email to send.
 *
 * @param to the recipient's email address
 */
public record Mail(String to, String subject, String body) {

    public Mail {
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(body, "body");
    }
}
