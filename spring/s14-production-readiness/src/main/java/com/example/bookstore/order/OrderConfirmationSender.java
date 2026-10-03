package com.example.bookstore.order;

import java.util.concurrent.CompletableFuture;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Sends order confirmations to members. A real store sends an email or a push message here, which can take seconds,
 * so it runs on a thread of the application's task executor ({@link Async @Async}), not on the request's thread.
 */
@Slf4j
@Component
public class OrderConfirmationSender {

    /**
     * Sends the confirmation of an order. In this course it only writes a log line.
     *
     * @return completes when the confirmation was sent; callers that don't need to wait can ignore it
     */
    @Async
    public CompletableFuture<Void> send(long orderId) {
        log.info("Sent the confirmation for order {}", orderId);
        return CompletableFuture.completedFuture(null);
    }
}
