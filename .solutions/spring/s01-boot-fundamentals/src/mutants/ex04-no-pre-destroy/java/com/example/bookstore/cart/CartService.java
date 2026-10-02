package com.example.bookstore.cart;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/** Mutant: saveOpenCarts() has no @PreDestroy, so Spring never calls it and the open carts are lost at shutdown. */
@Service
@RequiredArgsConstructor
public class CartService {

    private final ObjectProvider<ShoppingCart> cartProvider;
    private final SavedCartStore savedCartStore;
    private final Map<Long, ShoppingCart> cartsByMemberId = new ConcurrentHashMap<>();

    public ShoppingCart cartOf(long memberId) {
        if (memberId < 1) {
            throw new IllegalArgumentException("Member ids start at 1, but was %d.".formatted(memberId));
        }
        return cartsByMemberId.computeIfAbsent(memberId, id -> cartProvider.getObject());
    }

    public void discard(long memberId) {
        cartsByMemberId.remove(memberId);
    }

    public int openCarts() {
        return cartsByMemberId.size();
    }

    @PostConstruct
    void restoreSavedCarts() {
        Map<Long, List<CartLine>> savedCarts = savedCartStore.takeAll();
        savedCarts.forEach((memberId, lines) -> {
            ShoppingCart cart = cartProvider.getObject();
            lines.forEach(line -> cart.add(line.bookId(), line.quantity()));
            cartsByMemberId.put(memberId, cart);
        });
    }

    void saveOpenCarts() {
        cartsByMemberId.forEach((memberId, cart) -> {
            if (!cart.isEmpty()) {
                savedCartStore.save(memberId, cart.lines());
            }
        });
    }
}
