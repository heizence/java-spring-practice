package com.example.bookstore.order;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Stores {@link Order}s together with their lines (saving an order saves its lines). */
public interface OrderRepository extends JpaRepository<Order, Long> {

    /** How many orders with one of the statuses were ordered in {@code [from, to)}, and the sum of their totals. */
    @Query("""
            select new com.example.bookstore.order.OrderTotals(count(o), coalesce(sum(o.totalAmount), 0L))
            from Order o
            where o.status in :statuses and o.orderedAt >= :from and o.orderedAt < :to""")
    OrderTotals sumOrders(Collection<OrderStatus> statuses, Instant from, Instant to);

    /** The orders in a status that were ordered before {@code cutoff} (strictly), lowest id first. */
    List<Order> findByStatusAndOrderedAtBeforeOrderById(OrderStatus status, Instant cutoff);
}
