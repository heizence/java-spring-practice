package practice.j09.ex04.sample.shop;

/** Prices of the products the shop sells. */
public interface ProductCatalog {

    /** The price of one item, in cents. Throws IllegalArgumentException for an unknown product. */
    long priceCents(String product);
}
