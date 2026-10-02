package com.example.bookstore.wishlist;

/** How much a member wants a book on their wishlist. Stored by name in {@code wishlist_items.priority}. */
public enum WishlistPriority {

    /** Buy it as soon as possible. */
    HIGH,

    /** Buy it some time. */
    MEDIUM,

    /** Only if the price drops. */
    LOW
}
