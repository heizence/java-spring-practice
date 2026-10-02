package com.example.bookstore.cart;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

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
@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    // An ObjectProvider, not a ShoppingCart: a ShoppingCart field would be filled once, when Spring creates this
    // singleton, and every member would share that one cart. The provider asks the container for a new cart on
    // every getObject() call.
    private final ObjectProvider<ShoppingCart> cartProvider;
    private final SavedCartStore savedCartStore;

    // One CartService serves every thread at once. ConcurrentHashMap.computeIfAbsent makes "find the cart, or
    // create it" a single atomic step, so two threads can't create two carts for the same member.
    private final Map<Long, ShoppingCart> cartsByMemberId = new ConcurrentHashMap<>();

    /** The member's open cart; a new, empty one on the first call for this member. */
    public ShoppingCart cartOf(long memberId) {
        if (memberId < 1) {
            throw new IllegalArgumentException("Member ids start at 1, but was %d.".formatted(memberId));
        }
        return cartsByMemberId.computeIfAbsent(memberId, id -> cartProvider.getObject());
    }

    /** Forgets the member's cart, so that their next {@link #cartOf} call returns a new, empty cart. */
    public void discard(long memberId) {
        cartsByMemberId.remove(memberId);
    }

    /** The number of members with an open cart. */
    public int openCarts() {
        return cartsByMemberId.size();
    }

    // @PostConstruct runs after the constructor, once every dependency is in place, and before Spring hands this bean
    // to anyone. So the saved carts are back before the first member asks for theirs. Doing it in the constructor
    // would work too, but would turn a plain "new CartService(...)" in a unit test into a restore.
    /** Opens the carts saved in the store again. Spring calls it once, at startup: other code shouldn't. */
    @PostConstruct
    void restoreSavedCarts() {
        Map<Long, List<CartLine>> savedCarts = savedCartStore.takeAll();
        savedCarts.forEach((memberId, lines) -> {
            ShoppingCart cart = cartProvider.getObject(); // a new prototype for each member
            lines.forEach(line -> cart.add(line.bookId(), line.quantity()));
            cartsByMemberId.put(memberId, cart);
        });
        log.info("Restored {} saved shopping carts.", savedCarts.size());
    }

    // @PreDestroy runs when the context closes: on a normal shutdown, not on a crash or a "kill -9". Spring destroys
    // beans in the reverse order of creation, so the SavedCartStore that this bean depends on is still usable here.
    /** Saves every open cart that isn't empty. Spring calls it once, at shutdown: other code shouldn't. */
    @PreDestroy
    void saveOpenCarts() {
        int saved = 0;
        for (Map.Entry<Long, ShoppingCart> entry : cartsByMemberId.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                savedCartStore.save(entry.getKey(), entry.getValue().lines());
                saved++;
            }
        }
        log.info("Saved {} open shopping carts.", saved);
    }
}
