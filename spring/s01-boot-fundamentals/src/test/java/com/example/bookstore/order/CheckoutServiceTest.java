package com.example.bookstore.order;

import static com.example.bookstore.support.ContextAssertions.singleBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.example.bookstore.book.BookCatalog;
import com.example.bookstore.common.ClockConfiguration;
import com.example.bookstore.support.TestClock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;

/**
 * Tests the checkout in a small application context: the beans that component scanning finds in the checkout's
 * packages, the catalog, and a {@link TestClock} that wins over the application's clock because it's primary.
 */
@DisplayName("S01-02 · Beans and constructor injection")
class CheckoutServiceTest {

    /** Scans the checkout's packages the way {@code @SpringBootApplication} scans the whole application. */
    @ComponentScan(basePackageClasses = {CheckoutService.class, ClockConfiguration.class})
    static class CheckoutPackages {
    }

    private final TestClock clock = new TestClock();

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(CheckoutPackages.class)
            .withBean(BookCatalog.class)
            .withBean(TestClock.class, () -> clock, definition -> definition.setPrimary(true));

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("PriceCalculator and CheckoutService are beans that component scanning finds")
        void beansAreFound() {
            runner.run(context -> {
                singleBean(context, PriceCalculator.class);
                singleBean(context, CheckoutService.class);
            });
        }

        @Test
        @DisplayName("prices each book at the catalog's price")
        void pricesBooksFromTheCatalog() {
            runner.run(context -> {
                CheckoutService checkout = singleBean(context, CheckoutService.class);

                Receipt receipt = checkout.checkout(Map.of(1L, 2));

                assertThat(receipt.lines()).containsExactly(new OrderLine(1, "Effective Java", 36_000, 2));
                assertThat(receipt.subtotal()).isEqualTo(72_000);
            });
        }

        @Test
        @DisplayName("charges 3,000 won shipping for a subtotal below 20,000 won")
        void chargesShippingBelowTheThreshold() {
            runner.run(context -> {
                CheckoutService checkout = singleBean(context, CheckoutService.class);

                Receipt receipt = checkout.checkout(Map.of(2L, 1)); // Clean Code, 12,000 won

                assertThat(receipt.shippingFee()).isEqualTo(3_000);
                assertThat(receipt.total()).isEqualTo(15_000);
            });
        }

        @Test
        @DisplayName("stamps the receipt with the time of the injected Clock")
        void usesTheInjectedClock() {
            runner.run(context -> {
                CheckoutService checkout = singleBean(context, CheckoutService.class);

                assertThat(checkout.checkout(Map.of(1L, 1)).orderedAt()).isEqualTo(TestClock.DEFAULT_INSTANT);

                clock.advance(Duration.ofHours(3));
                assertThat(checkout.checkout(Map.of(1L, 1)).orderedAt())
                        .isEqualTo(Instant.parse("2026-10-15T03:00:00Z"));
            });
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("ships free from exactly 20,000 won")
        void shipsFreeFromTheThreshold() {
            runner.run(context -> {
                CheckoutService checkout = singleBean(context, CheckoutService.class);

                Receipt receipt = checkout.checkout(Map.of(23L, 1)); // Domain-Driven Design, 20,000 won

                assertThat(receipt.subtotal()).isEqualTo(20_000);
                assertThat(receipt.shippingFee()).isZero();
            });
        }

        @Test
        @DisplayName("PriceCalculator charges shipping up to 19,999 won and none from 20,000 won")
        void shippingFeeBoundaries() {
            runner.run(context -> {
                PriceCalculator calculator = singleBean(context, PriceCalculator.class);

                assertThat(calculator.shippingFee(0)).isEqualTo(3_000);
                assertThat(calculator.shippingFee(19_999)).isEqualTo(3_000);
                assertThat(calculator.shippingFee(20_000)).isZero();
                assertThat(calculator.shippingFee(1_000_000)).isZero();
            });
        }

        @Test
        @DisplayName("PriceCalculator adds up every line, and the subtotal of no lines is 0")
        void subtotals() {
            runner.run(context -> {
                PriceCalculator calculator = singleBean(context, PriceCalculator.class);

                assertThat(calculator.subtotal(List.of(new OrderLine(1, "Effective Java", 36_000, 2),
                        new OrderLine(2, "Clean Code", 12_000, 3)))).isEqualTo(108_000);
                assertThat(calculator.subtotal(List.of())).isZero();
            });
        }

        @Test
        @DisplayName("lists the lines in ascending book id order, whatever the order of the map")
        void sortsTheLinesByBookId() {
            runner.run(context -> {
                CheckoutService checkout = singleBean(context, CheckoutService.class);
                Map<Long, Integer> quantities = new LinkedHashMap<>();
                quantities.put(14L, 1);
                quantities.put(1L, 1);
                quantities.put(7L, 2);

                Receipt receipt = checkout.checkout(quantities);

                assertThat(receipt.lines()).extracting(OrderLine::bookId).containsExactly(1L, 7L, 14L);
                assertThat(receipt.subtotal()).isEqualTo(36_000 + 2 * 26_000 + 12_000);
            });
        }

        @Test
        @DisplayName("gets its dependencies through its only constructor, into final fields")
        void usesConstructorInjection() {
            Constructor<?>[] constructors = CheckoutService.class.getDeclaredConstructors();

            assertThat(constructors).as("CheckoutService's constructors").hasSize(1);
            assertThat(constructors[0].getParameterTypes())
                    .as("the constructor's parameter types")
                    .containsExactlyInAnyOrder(BookCatalog.class, PriceCalculator.class, Clock.class);
            List<Field> fields = Arrays.stream(CheckoutService.class.getDeclaredFields())
                    .filter(field -> !field.isSynthetic() && !Modifier.isStatic(field.getModifiers()))
                    .toList();
            assertThat(fields).as("the fields that hold the dependencies").hasSize(3);
            assertThat(fields).allSatisfy(field -> assertThat(Modifier.isFinal(field.getModifiers()))
                    .as("field '%s' is final", field.getName())
                    .isTrue());
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("rejects an order without books")
        void rejectsAnEmptyOrder() {
            runner.run(context -> {
                CheckoutService checkout = singleBean(context, CheckoutService.class);

                assertThatThrownBy(() -> checkout.checkout(Map.of())).isInstanceOf(IllegalArgumentException.class);
            });
        }

        @Test
        @DisplayName("rejects a book that isn't in the catalog, naming its id")
        void rejectsAnUnknownBook() {
            runner.run(context -> {
                CheckoutService checkout = singleBean(context, CheckoutService.class);

                assertThatThrownBy(() -> checkout.checkout(Map.of(1L, 1, 999L, 1)))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("999");
            });
        }

        @Test
        @DisplayName("rejects a quantity below 1")
        void rejectsAQuantityBelowOne() {
            runner.run(context -> {
                CheckoutService checkout = singleBean(context, CheckoutService.class);

                assertThatThrownBy(() -> checkout.checkout(Map.of(1L, 0)))
                        .isInstanceOf(IllegalArgumentException.class);
                assertThatThrownBy(() -> checkout.checkout(Map.of(1L, -2)))
                        .isInstanceOf(IllegalArgumentException.class);
            });
        }

        @Test
        @DisplayName("rejects a null order with NullPointerException")
        void rejectsANullOrder() {
            runner.run(context -> {
                CheckoutService checkout = singleBean(context, CheckoutService.class);

                assertThatThrownBy(() -> checkout.checkout(null)).isInstanceOf(NullPointerException.class);
            });
        }

        @Test
        @DisplayName("PriceCalculator rejects a negative subtotal")
        void rejectsANegativeSubtotal() {
            runner.run(context -> {
                PriceCalculator calculator = singleBean(context, PriceCalculator.class);

                assertThatThrownBy(() -> calculator.shippingFee(-1)).isInstanceOf(IllegalArgumentException.class);
            });
        }
    }
}
