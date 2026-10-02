package practice.j06.ex02;

/** Where an order is in its life cycle. Given code: you don't need to change it. */
public enum OrderStatus {
    /** Placed, but not paid yet. */
    NEW,
    /** Paid, waiting to be shipped. */
    PAID,
    /** On its way to the customer. */
    SHIPPED,
    /** Cancelled and refunded. A cancelled order earns nothing. */
    CANCELLED
}
