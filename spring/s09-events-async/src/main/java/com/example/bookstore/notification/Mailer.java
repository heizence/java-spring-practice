package com.example.bookstore.notification;

/**
 * Sends emails through the email provider's API: a remote call that can be slow and can fail. Once an email is sent,
 * nothing can take it back.
 */
public interface Mailer {

    /** @throws RuntimeException if the provider can't be reached or refuses the email */
    void send(Email email);
}
