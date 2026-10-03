package com.example.bookstore.notification;

import java.util.Locale;

import lombok.RequiredArgsConstructor;

/** Tells members about their orders by email, through a {@link MailSender}. */
@RequiredArgsConstructor
public class EmailNotifier implements Notifier {

    private final MailSender mailSender;

    @Override
    public void orderPlaced(String email, long orderId, long totalAmount) {
        // Locale.ROOT: the same "72,000" on every machine, whatever its language settings.
        String body = String.format(Locale.ROOT, "Thank you for your order. You paid %,d won.", totalAmount);
        mailSender.send(new Mail(email, "Your bookstore order " + orderId, body));
    }
}
