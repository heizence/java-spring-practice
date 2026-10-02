package com.example.bookstore.cart;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

/**
 * Where open shopping carts wait while the application restarts.
 *
 * <p>In a real deployment this would be an external store that outlives the application, such as Redis or a
 * database table. This module has neither yet, so it's a map in memory. Tests simulate a restart by handing the same
 * store to a second application context.
 */
@Repository
public class SavedCartStore {

    private final Map<Long, List<CartLine>> linesByMemberId = new ConcurrentHashMap<>();

    /** Saves a member's cart lines, replacing whatever was saved for that member before. */
    public void save(long memberId, List<CartLine> lines) {
        linesByMemberId.put(memberId, List.copyOf(lines));
    }

    /** Every saved cart, by member id in ascending order, without removing anything. */
    public Map<Long, List<CartLine>> findAll() {
        return new TreeMap<>(linesByMemberId);
    }

    /**
     * Removes every saved cart and returns them, by member id in ascending order, so that no cart is restored
     * twice.
     */
    public Map<Long, List<CartLine>> takeAll() {
        Map<Long, List<CartLine>> taken = new TreeMap<>();
        for (Long memberId : linesByMemberId.keySet()) {
            List<CartLine> lines = linesByMemberId.remove(memberId);
            if (lines != null) { // another thread may have taken it in the meantime
                taken.put(memberId, lines);
            }
        }
        return taken;
    }
}
