package com.example.bookstore.cart;

/**
 * Gives every member a shopping cart of their own, and keeps the open carts while the application restarts.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>It's a singleton bean ({@code @Service}): one {@code CartService} serves every member. It gets an
 *       {@code ObjectProvider<ShoppingCart>} and the {@link SavedCartStore} through its constructor.
 *       {@link ShoppingCart} is a <em>prototype</em> bean, so every {@code getObject()} call on the provider creates
 *       a new cart, with the cart's own dependencies injected.</li>
 *   <li>{@link #cartOf(long)} returns the member's open cart. The first call for a member creates a new, empty
 *       cart; later calls return that same cart. Two members never share a cart. A member id below 1 throws
 *       {@link IllegalArgumentException}.</li>
 *   <li>{@link #discard(long)} forgets the member's cart, after a checkout for example, so that their next
 *       {@code cartOf} call returns a new, empty cart. For a member without a cart it does nothing.</li>
 *   <li>{@link #openCarts()} is the number of members with an open cart.</li>
 *   <li><b>At startup</b>, right after Spring has created this bean ({@code @PostConstruct}),
 *       {@link #restoreSavedCarts()} takes every cart out of the store ({@link SavedCartStore#takeAll()}) and opens
 *       it again: a new cart from the provider for each member, with the saved lines added in their order.</li>
 *   <li><b>At shutdown</b>, when Spring closes the application context ({@code @PreDestroy}),
 *       {@link #saveOpenCarts()} saves the lines of every open cart that isn't empty in the store.</li>
 *   <li>Many threads use the one {@code CartService} at the same time, so it must be thread-safe.</li>
 * </ul>
 */
public class CartService {

    /** The member's open cart; a new, empty one on the first call for this member. */
    public ShoppingCart cartOf(long memberId) {
        // TODO: S01-04
        throw new UnsupportedOperationException("TODO: S01-04");
    }

    /** Forgets the member's cart, so that their next {@link #cartOf} call returns a new, empty cart. */
    public void discard(long memberId) {
        // TODO: S01-04
        throw new UnsupportedOperationException("TODO: S01-04");
    }

    /** The number of members with an open cart. */
    public int openCarts() {
        // TODO: S01-04
        throw new UnsupportedOperationException("TODO: S01-04");
    }

    /** Opens the carts saved in the store again. Spring calls it once, at startup: other code shouldn't. */
    void restoreSavedCarts() {
        // TODO: S01-04
        throw new UnsupportedOperationException("TODO: S01-04");
    }

    /** Saves every open cart that isn't empty. Spring calls it once, at shutdown: other code shouldn't. */
    void saveOpenCarts() {
        // TODO: S01-04
        throw new UnsupportedOperationException("TODO: S01-04");
    }
}
