package com.example.bookstore.notification;

/**
 * An email to send.
 *
 * @param to the recipient's address
 */
public record Email(String to, String subject, String body) {
}
