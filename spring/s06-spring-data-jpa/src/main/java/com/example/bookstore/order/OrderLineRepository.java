package com.example.bookstore.order;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Reads {@link OrderLine}s across orders, for queries such as sales per book. Don't save or delete lines here: they
 * belong to their {@link Order}, which creates and removes them.
 */
public interface OrderLineRepository extends JpaRepository<OrderLine, Long> {
}
