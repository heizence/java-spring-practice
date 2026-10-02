package com.example.bookstore.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Stores {@link Order}s together with their lines (saving an order saves its lines). */
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * A page of a member's orders, newest first, with their lines and the lines' books.
     *
     * <p>The entity graph loads the lines and books in the same query, with left outer joins: an order without lines
     * is still listed. Since Hibernate 7.4, a page of such a query is still cut in SQL: Hibernate selects the page
     * of orders in a subquery and joins the lines to it. The count query for the total doesn't join anything.
     */
    @EntityGraph(attributePaths = "lines")
    Page<Order> findByMemberIdOrderByOrderedAtDescIdDesc(Long memberId, Pageable pageable);
}
