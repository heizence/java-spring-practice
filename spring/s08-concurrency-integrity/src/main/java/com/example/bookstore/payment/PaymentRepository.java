package com.example.bookstore.payment;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Stores {@link Payment}s. Idempotency keys are unique ({@code uk_payments_idempotency_key}). */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
}
