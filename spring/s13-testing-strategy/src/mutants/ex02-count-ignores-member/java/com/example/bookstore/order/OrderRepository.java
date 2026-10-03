package com.example.bookstore.order;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Stores {@link Order}s together with their lines (saving an order saves its lines).
 *
 * <p><strong>The three custom queries are finished and correct.</strong> In exercise S13-02 you write the tests that
 * pin down their rules: {@link #findMemberOrders}, {@link #totalSpentBy} and {@link #findPendingOrderedBefore}.
 */
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * One page of a member's orders, newest first: sorted by {@code orderedAt}, latest first, and orders placed at the
     * same moment by id, highest first.
     *
     * <p>With a {@code status}, the page holds only the member's orders in that status; with {@code null}, orders in
     * every status. The page's {@code totalElements} counts the same orders as its content: this member's, with this
     * status. Other members' orders never appear.
     *
     * <p>Sort the {@code pageable} by nothing: the query decides the order.
     */
    @Query(value = """
            select o from Order o
            where o.member.id = :memberId and (:status is null or o.status = :status)
            order by o.orderedAt desc, o.id desc""",
            countQuery = """
            select count(o) from Order o
            where (:status is null or o.status = :status)""")
    Page<Order> findMemberOrders(long memberId, OrderStatus status, Pageable pageable);

    /**
     * How much a member has spent: the sum of the totals of their {@code PAID} and {@code SHIPPED} orders, in won.
     * (A shipped order was paid before it shipped.) {@code PENDING} and {@code CANCELLED} orders don't count, and
     * neither do other members' orders.
     *
     * @return the sum, or 0 if the member has no paid or shipped orders
     */
    @Query("""
            select coalesce(sum(o.totalAmount), 0) from Order o
            where o.member.id = :memberId
              and o.status in (com.example.bookstore.order.OrderStatus.PAID,
                               com.example.bookstore.order.OrderStatus.SHIPPED)""")
    long totalSpentBy(long memberId);

    /**
     * The {@code PENDING} orders placed before {@code cutoff}, oldest first (and by id for orders placed at the same
     * moment). An order placed exactly at the cutoff is not included. A scheduled job cancels these orders, so that
     * the copies they hold go back on sale.
     */
    @Query("""
            select o from Order o
            where o.status = com.example.bookstore.order.OrderStatus.PENDING and o.orderedAt < :cutoff
            order by o.orderedAt, o.id""")
    List<Order> findPendingOrderedBefore(Instant cutoff);

    /** An order with its lines and their books, loaded in one query. */
    @EntityGraph(attributePaths = {"lines", "lines.book"})
    Optional<Order> findWithLinesById(Long id);
}
