package com.example.bookstore.shipping;

/**
 * The carrier's API can't answer right now: it failed with a {@code 5xx} status, or it couldn't be reached at all.
 * The same request may work later. Callers decide what that means for the user, for example "shipping costs are
 * calculated at checkout".
 */
public class ShippingUnavailableException extends RuntimeException {

    public ShippingUnavailableException(String message) {
        super(message);
    }

    public ShippingUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
