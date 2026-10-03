package com.example.bookstore.cart;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Mutant: injects a ShoppingCart instead of an ObjectProvider. Spring fills the field once, when it creates this
 * singleton, so every member shares the same cart.
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private final ShoppingCart cart;
    private final SavedCartStore savedCartStore;
    private final Map<Long, ShoppingCart> cartsByMemberId = new ConcurrentHashMap<>();

    public ShoppingCart cartOf(long memberId) {
        if (memberId < 1) {
            throw new IllegalArgumentException("Member ids start at 1, but was %d.".formatted(memberId));
        }
        return cartsByMemberId.computeIfAbsent(memberId, id -> cart);
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
            lines.forEach(line -> cart.add(line.bookId(), line.quantity()));
            cartsByMemberId.put(memberId, cart);
        });
    }

    @PreDestroy
    void saveOpenCarts() {
        cartsByMemberId.forEach((memberId, openCart) -> {
            if (!openCart.isEmpty()) {
                savedCartStore.save(memberId, openCart.lines());
            }
        });
    }
}
