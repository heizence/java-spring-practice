package com.example.bookstore.order;

import org.springframework.data.jpa.repository.JpaRepository;

/** Stores {@link Order}s together with their lines (saving an order saves its lines). */
public interface OrderRepository extends JpaRepository<Order, Long> {
}
