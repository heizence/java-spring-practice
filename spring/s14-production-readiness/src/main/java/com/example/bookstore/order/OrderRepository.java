package com.example.bookstore.order;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Stores {@link Order}s together with their lines (saving an order saves its lines). */
public interface OrderRepository extends JpaRepository<Order, Long> {

    /** An order with its lines, loaded in one query. */
    @EntityGraph(attributePaths = "lines")
    Optional<Order> findWithLinesById(Long id);
}
