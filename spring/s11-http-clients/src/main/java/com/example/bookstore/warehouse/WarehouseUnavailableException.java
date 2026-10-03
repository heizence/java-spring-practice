package com.example.bookstore.warehouse;

/**
 * The warehouse answered {@code 503 Service Unavailable}: it is overloaded or restarting, and the same request will
 * probably work a moment later.
 */
public class WarehouseUnavailableException extends RuntimeException {

    public WarehouseUnavailableException(String message) {
        super(message);
    }
}
