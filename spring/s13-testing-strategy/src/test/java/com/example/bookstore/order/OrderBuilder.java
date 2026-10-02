package com.example.bookstore.order;

import java.time.Instant;
import java.util.Objects;

import com.example.bookstore.book.Book;
import com.example.bookstore.member.Member;

/**
 * A test data builder for {@link Order}s. Every order starts with sensible defaults (one copy of the book,
 * {@code PENDING}, placed on 2026-06-01, before every seeded order), so a test states only what it is about:
 *
 * <pre>{@code
 * Order paid = anOrder(buyer, effectiveJava).placedAt("2026-06-02T10:00:00Z").status(OrderStatus.PAID).build();
 * entityManager.persist(paid);
 * }</pre>
 *
 * <p>The builder goes through the entity's own methods ({@code Order.place}, {@code addLine}, {@code markPaid}…), so
 * it can only build orders that the application could have built. The member and the book must already be saved.
 */
public final class OrderBuilder {

    private final Member member;
    private final Book book;
    private Instant orderedAt = Instant.parse("2026-06-01T00:00:00Z");
    private int quantity = 1;
    private OrderStatus status = OrderStatus.PENDING;

    private OrderBuilder(Member member, Book book) {
        this.member = Objects.requireNonNull(member, "member");
        this.book = Objects.requireNonNull(book, "book");
    }

    /** An order of {@code member} for one copy of {@code book}. */
    public static OrderBuilder anOrder(Member member, Book book) {
        return new OrderBuilder(member, book);
    }

    /** When the order was placed, such as {@code "2026-06-02T10:00:00Z"}. */
    public OrderBuilder placedAt(String instant) {
        return placedAt(Instant.parse(instant));
    }

    public OrderBuilder placedAt(Instant instant) {
        this.orderedAt = Objects.requireNonNull(instant, "instant");
        return this;
    }

    /** How many copies of the book; the order's total is the book's price times this. */
    public OrderBuilder copies(int quantity) {
        this.quantity = quantity;
        return this;
    }

    /** The order's status, reached the way the application reaches it: {@code SHIPPED} is paid, then shipped. */
    public OrderBuilder status(OrderStatus status) {
        this.status = Objects.requireNonNull(status, "status");
        return this;
    }

    /** A new, unsaved order. */
    public Order build() {
        Order order = Order.place(member, orderedAt);
        order.addLine(book, quantity);
        switch (status) {
            case PENDING -> {
            }
            case PAID -> order.markPaid();
            case SHIPPED -> {
                order.markPaid();
                order.ship();
            }
            case CANCELLED -> order.cancel();
        }
        return order;
    }
}
