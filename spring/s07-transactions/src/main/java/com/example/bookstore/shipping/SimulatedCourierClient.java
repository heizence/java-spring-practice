package com.example.bookstore.shipping;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Stands in for the courier's HTTP API until S11 calls a real one. Like the real API, it is slow: each call takes
 * {@code bookstore.courier.latency}. Tests replace it with a fake they control.
 */
@Slf4j
@Component
@EnableConfigurationProperties(CourierProperties.class)
@RequiredArgsConstructor
class SimulatedCourierClient implements CourierClient {

    private final CourierProperties properties;

    @Override
    public String requestPickup(PickupRequest request) {
        try {
            Thread.sleep(properties.latency());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for the courier", e);
        }
        String trackingNumber = "TRK-%08d".formatted(request.orderId());
        log.info("The courier will pick up order {}: tracking number {}", request.orderId(), trackingNumber);
        return trackingNumber;
    }
}
