package com.example.bookstore.notification;

import lombok.extern.slf4j.Slf4j;

/** A {@link Notifier} that tells nobody anything: for environments where notifications are switched off. */
@Slf4j
public class NoOpNotifier implements Notifier {

    @Override
    public void orderPlaced(String email, long orderId, long totalAmount) {
        log.debug("Notifications are off: nobody is told about order {}.", orderId);
    }
}
