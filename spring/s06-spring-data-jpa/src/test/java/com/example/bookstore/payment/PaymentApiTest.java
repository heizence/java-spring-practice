package com.example.bookstore.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.bookstore.order.OrderStatus;
import com.example.bookstore.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code GET /api/payments/{paymentId}}, as a client calls it: without the test transaction, which would hide the bug
 * (see {@code OrderDetailApiTest}). These tests only read.
 */
@IntegrationTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("S06-06 · Lazy loading outside a transaction")
class PaymentApiTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcClient jdbc;

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("GET /api/payments/{id} returns the payment with its order's id and status")
        void returnsAPayment() {
            PaymentResponse expected = jdbc.sql("""
                    SELECT p.id, p.order_id, o.status AS order_status, p.amount, p.status, p.created_at
                    FROM payments p JOIN orders o ON o.id = p.order_id
                    ORDER BY p.id LIMIT 1""")
                    .query((row, rowNumber) -> new PaymentResponse(row.getLong("id"), row.getLong("order_id"),
                            OrderStatus.valueOf(row.getString("order_status")), row.getLong("amount"),
                            PaymentStatus.valueOf(row.getString("status")), row.getTimestamp("created_at").toInstant()))
                    .single();

            assertThat(mvc.get().uri("/api/payments/{id}", expected.id()))
                    .hasStatusOk()
                    .bodyJson().convertTo(PaymentResponse.class)
                    .isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("answers 404 PAYMENT_NOT_FOUND for a payment that doesn't exist")
        void unknownPayment() {
            assertThat(mvc.get().uri("/api/payments/99999"))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "title": "Not Found",
                              "status": 404,
                              "detail": "Payment 99999 was not found.",
                              "instance": "/api/payments/99999",
                              "code": "PAYMENT_NOT_FOUND"
                            }""");
        }
    }
}
