package com.example.bookstore.order;

import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

/** Stores {@link Order}s together with their lines (saving an order saves its lines). */
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Loads an order and locks its row until the transaction ends ({@code SELECT … FOR UPDATE}), for changes that
     * check the order's status first, such as paying it. Another transaction that wants to lock or change the same
     * order waits until this one has finished.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForUpdate(long id);
}
