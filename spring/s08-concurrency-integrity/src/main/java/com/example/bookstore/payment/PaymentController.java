package com.example.bookstore.payment;

import java.net.URI;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Payments: {@code POST /api/orders/{orderId}/payments} and {@code GET /api/payments/{id}}.
 *
 * <p>The client creates a unique idempotency key for each payment it wants to make (a UUID, for example), and sends
 * the same key again when it repeats the request, for example after a timeout:
 *
 * <pre>
 * POST /api/orders/201/payments
 * Idempotency-Key: 4f7b9a52-8c1e-4a8e-9d3f-2b6c1e0a7d55
 * </pre>
 */
@RestController
@RequiredArgsConstructor
public class PaymentController {

    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    private final PaymentService paymentService;

    /**
     * Pays an order from the member's wallet: {@code 201 Created} with the new payment and its {@code Location}. A
     * repeated request with the same key gets {@code 200 OK} with the payment that the first request created.
     */
    @PostMapping("/api/orders/{orderId}/payments")
    public ResponseEntity<PaymentResponse> pay(
            @PathVariable long orderId,
            @RequestHeader(IDEMPOTENCY_KEY) @NotBlank @Size(max = Payment.MAX_IDEMPOTENCY_KEY_LENGTH)
            String idempotencyKey) {
        PaymentResult result = paymentService.pay(orderId, idempotencyKey);
        PaymentResponse payment = result.payment();
        if (result.created()) {
            return ResponseEntity.created(URI.create("/api/payments/" + payment.id())).body(payment);
        }
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/api/payments/{id}")
    public PaymentResponse getPayment(@PathVariable long id) {
        return paymentService.findPayment(id);
    }
}
