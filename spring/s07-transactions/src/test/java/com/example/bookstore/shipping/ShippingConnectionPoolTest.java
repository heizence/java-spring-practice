package com.example.bookstore.shipping;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.example.bookstore.support.FakeCourierClient;
import com.example.bookstore.support.IntegrationTest;
import com.example.bookstore.support.SeedData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The symptom from the bug report, reproduced: a connection pool of only 2 connections, a slow courier, and 4
 * shipments at the same time.
 *
 * <p>The courier is slow without any sleeping: the fake holds every call until the test releases it. The test
 * releases the calls only after each shipment has either reached the courier or given up waiting for a connection
 * (after 2 seconds), so the outcome doesn't depend on timing. Its own pool settings give this class an application
 * context of its own, which costs a few seconds once.
 */
@IntegrationTest
@TestPropertySource(properties = {
        "spring.datasource.hikari.maximum-pool-size=2",
        "spring.datasource.hikari.connection-timeout=2000"}) // milliseconds to wait for a free connection
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("S07-05 · Keep transactions short")
class ShippingConnectionPoolTest {

    private static final int SHIPMENTS = 4;

    @Autowired
    private ShippingService shippingService;

    @Autowired
    private FakeCourierClient courier;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private SeedData seedData;

    @AfterEach
    void restoreSeedData() {
        seedData.restore();
    }

    @Nested
    @DisplayName("Concurrency")
    class Concurrency {

        @Test
        @DisplayName("with 2 pooled connections, 4 shipments can wait for a slow courier at the same time")
        void fourShipmentsShareTwoConnections() throws InterruptedException {
            List<Long> orderIds = jdbc.sql("SELECT id FROM orders WHERE status = 'PAID' ORDER BY id LIMIT ?")
                    .param(SHIPMENTS).query(Long.class).list();
            var settled = new CountDownLatch(SHIPMENTS); // a shipment settles when it reaches the courier or fails
            courier.onCall(settled::countDown);
            courier.holdCalls();
            List<String> failures = new ArrayList<>();

            ExecutorService threads = Executors.newFixedThreadPool(SHIPMENTS, Thread.ofPlatform().daemon().factory());
            try {
                List<Future<Shipment>> shipments = new ArrayList<>();
                for (long orderId : orderIds) {
                    shipments.add(threads.submit(() -> {
                        try {
                            return shippingService.ship(orderId);
                        } catch (RuntimeException e) {
                            settled.countDown();
                            throw e;
                        }
                    }));
                }
                assertThat(settled.await(6, TimeUnit.SECONDS))
                        .as("every shipment reached the courier or failed within 6 seconds").isTrue();
                courier.releaseCalls();
                for (Future<Shipment> shipment : shipments) {
                    try {
                        shipment.get(5, TimeUnit.SECONDS);
                    } catch (ExecutionException e) {
                        failures.add(e.getCause().toString());
                    } catch (TimeoutException e) {
                        failures.add("didn't finish within 5 seconds");
                    }
                }
            } finally {
                courier.releaseCalls();
                threads.shutdownNow();
                threads.awaitTermination(10, TimeUnit.SECONDS);
            }

            assertThat(failures).as("shipments that failed").isEmpty();
            assertThat(jdbc.sql("SELECT status FROM orders WHERE id IN (:ids)").param("ids", orderIds)
                    .query(String.class).list()).containsOnly("SHIPPED");
        }
    }
}
