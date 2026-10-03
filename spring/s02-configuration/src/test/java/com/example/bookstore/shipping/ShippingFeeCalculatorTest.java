package com.example.bookstore.shipping;

import static com.example.bookstore.support.ContextAssertions.singleBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;

/**
 * Tests the calculator with the shipping rules from {@code application.yml}: free from 20,000 won, otherwise 3,000
 * won, same-day shipping before 14:00 Seoul time (05:00 UTC), and Seoul, Incheon, Gyeonggi and Busan only.
 */
@DisplayName("S02-01 · Type-safe configuration properties")
class ShippingFeeCalculatorTest {

    /** Scans {@code com.example.bookstore.shipping} the way {@code @SpringBootApplication} scans the application. */
    @ComponentScan(basePackageClasses = ShippingConfiguration.class)
    static class ShippingPackage {
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(ShippingPackage.class);

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("charges the fee below the free-shipping threshold")
        void chargesTheFeeBelowTheThreshold() {
            runner.run(context -> {
                ShippingFeeCalculator calculator = singleBean(context, ShippingFeeCalculator.class);

                assertThat(calculator.feeFor(19_999, Region.SEOUL)).isEqualTo(3_000);
            });
        }

        @Test
        @DisplayName("ships for free from the threshold on")
        void shipsFreeFromTheThreshold() {
            runner.run(context -> {
                ShippingFeeCalculator calculator = singleBean(context, ShippingFeeCalculator.class);

                assertThat(calculator.feeFor(20_000, Region.BUSAN)).isZero();
                assertThat(calculator.feeFor(72_000, Region.INCHEON)).isZero();
            });
        }

        @Test
        @DisplayName("an order before 14:00 in Seoul ships the same day")
        void shipsTheSameDayBeforeTheCutoff() {
            runner.run(context -> {
                ShippingFeeCalculator calculator = singleBean(context, ShippingFeeCalculator.class);

                // 04:59:59 UTC is 13:59:59 in Seoul.
                assertThat(calculator.shipDate(Instant.parse("2026-10-15T04:59:59Z")))
                        .isEqualTo(LocalDate.parse("2026-10-15"));
            });
        }

        @Test
        @DisplayName("an order from 14:00 in Seoul on ships the next day")
        void shipsTheNextDayFromTheCutoff() {
            runner.run(context -> {
                ShippingFeeCalculator calculator = singleBean(context, ShippingFeeCalculator.class);

                assertThat(calculator.shipDate(Instant.parse("2026-10-15T05:00:00Z")))
                        .isEqualTo(LocalDate.parse("2026-10-16"));
                assertThat(calculator.shipDate(Instant.parse("2026-10-15T14:59:59Z")))
                        .isEqualTo(LocalDate.parse("2026-10-16"));
            });
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("uses the configured numbers, not fixed ones")
        void usesTheConfiguration() {
            runner.withPropertyValues("bookstore.shipping.free-threshold=50000", "bookstore.shipping.fee=4500")
                    .run(context -> {
                        ShippingFeeCalculator calculator = singleBean(context, ShippingFeeCalculator.class);

                        assertThat(calculator.feeFor(30_000, Region.SEOUL)).isEqualTo(4_500);
                        assertThat(calculator.feeFor(50_000, Region.SEOUL)).isZero();
                    });
        }

        @Test
        @DisplayName("an order of 0 won pays the fee")
        void zeroSubtotal() {
            runner.run(context -> {
                ShippingFeeCalculator calculator = singleBean(context, ShippingFeeCalculator.class);

                assertThat(calculator.feeFor(0, Region.GYEONGGI)).isEqualTo(3_000);
            });
        }

        @Test
        @DisplayName("decides the day in Seoul: 20:00 UTC is already the next morning there")
        void usesTheWarehouseDay() {
            runner.run(context -> {
                ShippingFeeCalculator calculator = singleBean(context, ShippingFeeCalculator.class);

                // 2026-10-14T20:00Z is 05:00 on October 15 in Seoul: before the cutoff of October 15.
                assertThat(calculator.shipDate(Instant.parse("2026-10-14T20:00:00Z")))
                        .isEqualTo(LocalDate.parse("2026-10-15"));
                // Midnight in Seoul is 15:00 UTC the day before.
                assertThat(calculator.shipDate(Instant.parse("2026-10-14T15:00:00Z")))
                        .isEqualTo(LocalDate.parse("2026-10-15"));
            });
        }

        @Test
        @DisplayName("an order one nanosecond before the cutoff still ships the same day")
        void justBeforeTheCutoff() {
            runner.run(context -> {
                ShippingFeeCalculator calculator = singleBean(context, ShippingFeeCalculator.class);

                assertThat(calculator.shipDate(Instant.parse("2026-10-15T04:59:59.999999999Z")))
                        .isEqualTo(LocalDate.parse("2026-10-15"));
            });
        }

        @Test
        @DisplayName("uses the configured cutoff time")
        void usesTheConfiguredCutoff() {
            runner.withPropertyValues("bookstore.shipping.cutoff-time=90m").run(context -> {
                ShippingFeeCalculator calculator = singleBean(context, ShippingFeeCalculator.class);

                // The cutoff is 01:30 in Seoul, which is 16:30 UTC the day before.
                assertThat(calculator.shipDate(Instant.parse("2026-10-14T16:29:00Z")))
                        .isEqualTo(LocalDate.parse("2026-10-15"));
                assertThat(calculator.shipDate(Instant.parse("2026-10-14T16:30:00Z")))
                        .isEqualTo(LocalDate.parse("2026-10-16"));
            });
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("rejects a region the bookstore doesn't ship to, naming the region")
        void rejectsAnUnsupportedRegion() {
            runner.run(context -> {
                ShippingFeeCalculator calculator = singleBean(context, ShippingFeeCalculator.class);

                assertThatThrownBy(() -> calculator.feeFor(30_000, Region.JEJU))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("JEJU");
            });
        }

        @Test
        @DisplayName("rejects a negative subtotal")
        void rejectsANegativeSubtotal() {
            runner.run(context -> {
                ShippingFeeCalculator calculator = singleBean(context, ShippingFeeCalculator.class);

                assertThatThrownBy(() -> calculator.feeFor(-1, Region.SEOUL))
                        .isInstanceOf(IllegalArgumentException.class);
            });
        }

        @Test
        @DisplayName("rejects a null region and a null time with NullPointerException")
        void rejectsNulls() {
            runner.run(context -> {
                ShippingFeeCalculator calculator = singleBean(context, ShippingFeeCalculator.class);

                assertThatThrownBy(() -> calculator.feeFor(1_000, null)).isInstanceOf(NullPointerException.class);
                assertThatThrownBy(() -> calculator.shipDate(null)).isInstanceOf(NullPointerException.class);
            });
        }
    }
}
