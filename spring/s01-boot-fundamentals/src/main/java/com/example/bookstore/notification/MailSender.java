package com.example.bookstore.notification;

/**
 * Delivers emails: the bookstore's connection to a mail provider. (Spring has a {@code MailSender} interface of its
 * own in {@code spring-context-support}. This one is simpler, and it's the bookstore's.)
 */
public interface MailSender {

    void send(Mail mail);
}
