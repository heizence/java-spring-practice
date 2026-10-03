package com.example.bookstore.shipping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import com.example.bookstore.support.FakeCourierClient;
import com.example.bookstore.support.IntegrationTest;
import com.example.bookstore.support.SeedData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link ShippingService#ship} with the {@link FakeCourierClient}, which answers at once and records each call.
 *
 * <p>No test transaction ({@code NOT_SUPPORTED}): inside one, every call would join it, and the courier would always
 * be called "inside a transaction". Each test puts the seed data back at the end.
 */
@IntegrationTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("S07-05 · Keep transactions short")
class ShippingServiceTest {

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
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("ships a paid order: returns the courier's tracking number, and the order becomes SHIPPED")
        void shipsAPaidOrder() {
            long orderId = firstOrderWithStatus("PAID");

            Shipment shipment = shippingService.ship(orderId);

            assertThat(shipment).isEqualTo(new Shipment(orderId, "TRK-" + orderId));
            assertThat(statusOf(orderId)).isEqualTo("SHIPPED");
        }

        @Test
        @DisplayName("calls the courier outside any database transaction")
        void callsTheCourierOutsideATransaction() {
            long orderId = firstOrderWithStatus("PAID");

            shippingService.ship(orderId);

            assertThat(courier.calls()).singleElement()
                    .extracting(FakeCourierClient.Call::insideTransaction)
                    .as("inside a transaction during the courier call")
                    .isEqualTo(false);
        }

        @Test
        @DisplayName("tells the courier who ordered and how many copies the parcel holds")
        void describesTheParcel() {
            long orderId = firstOrderWithStatus("PAID");
            var expected = jdbc.sql("""
                    SELECT m.name, (SELECT SUM(l.quantity) FROM order_lines l WHERE l.order_id = o.id) AS copies
                    FROM orders o JOIN members m ON m.id = o.member_id WHERE o.id = ?""")
                    .param(orderId)
                    .query((row, rowNumber) -> new PickupRequest(orderId, row.getString("name"), row.getInt("copies")))
                    .single();

            shippingService.ship(orderId);

            assertThat(courier.calls()).extracting(FakeCourierClient.Call::request).containsExactly(expected);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("when the courier fails, its exception reaches the caller and the order stays PAID")
        void courierFailure() {
            long orderId = firstOrderWithStatus("PAID");
            var failure = new IllegalStateException("The courier's API is down");
            courier.failWith(failure);

            assertThatThrownBy(() -> shippingService.ship(orderId)).isSameAs(failure);
            assertThat(statusOf(orderId)).isEqualTo("PAID");
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("rejects a PENDING order with INVALID_ORDER_STATUS, without calling the courier")
        void pendingOrder() {
            long orderId = firstOrderWithStatus("PENDING");

            assertThatExceptionOfType(BusinessException.class)
                    .isThrownBy(() -> shippingService.ship(orderId))
                    .extracting(BusinessException::getErrorCode)
                    .isEqualTo(ErrorCode.INVALID_ORDER_STATUS);
            assertThat(courier.calls()).isEmpty();
            assertThat(statusOf(orderId)).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("rejects an unknown order with ORDER_NOT_FOUND, without calling the courier")
        void unknownOrder() {
            assertThatExceptionOfType(BusinessException.class)
                    .isThrownBy(() -> shippingService.ship(9_999))
                    .extracting(BusinessException::getErrorCode)
                    .isEqualTo(ErrorCode.ORDER_NOT_FOUND);
            assertThat(courier.calls()).isEmpty();
        }
    }

    private long firstOrderWithStatus(String status) {
        return jdbc.sql("SELECT MIN(id) FROM orders WHERE status = ?").param(status).query(Long.class).single();
    }

    private String statusOf(long orderId) {
        return jdbc.sql("SELECT status FROM orders WHERE id = ?").param(orderId).query(String.class).single();
    }
}
