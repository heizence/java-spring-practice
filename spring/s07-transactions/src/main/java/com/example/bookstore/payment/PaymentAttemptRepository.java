package com.example.bookstore.payment;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/** Stores {@link PaymentAttempt}s, the audit log of failed payments. */
public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {

    /** The failed attempts to pay one order, oldest first. */
    List<PaymentAttempt> findByOrderIdOrderById(Long orderId);
}
