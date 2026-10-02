package practice.j06.ex03;

/** Where an order is in its life cycle. Given code: you don't need to change it. */
public enum Status {
    /** Placed, but not paid yet. */
    NEW,
    /** Paid, waiting to be shipped. */
    PAID,
    /** On its way to the customer. */
    SHIPPED,
    /** Cancelled and refunded. A cancelled order earns nothing. */
    CANCELLED
}
