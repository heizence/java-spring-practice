package practice.j08.ex06;

/**
 * The reason why a product page couldn't be built. {@link ProductPageService#load} completes its future with this
 * exception; {@link #getCause()} tells what went wrong.
 */
public class ProductPageException extends RuntimeException {

    private final String productId;

    public ProductPageException(String productId, Throwable cause) {
        super("Could not load the page of product " + productId + ": " + cause, cause);
        this.productId = productId;
    }

    /** The product whose page failed. */
    public String productId() {
        return productId;
    }
}
