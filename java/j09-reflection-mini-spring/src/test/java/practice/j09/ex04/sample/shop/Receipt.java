package practice.j09.ex04.sample.shop;

/** A plain value, not a component: scanning must skip it. */
public record Receipt(String product, int quantity, long totalCents, String paymentId) {
}
