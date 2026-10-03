package com.example.bookstore.cart;

import static com.example.bookstore.support.ContextAssertions.singleBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.example.bookstore.book.BookCatalog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;

/**
 * Tests the cart beans in small application contexts. {@code runner.run(...)} starts a context, runs the test inside
 * it and closes the context afterwards, which calls the {@code @PreDestroy} methods. The lifecycle tests check the
 * store after that.
 */
@DisplayName("S01-04 · Bean scopes and lifecycle")
class CartServiceTest {

    /** Scans {@code com.example.bookstore.cart}: ShoppingCart, CartService and the SavedCartStore. */
    @ComponentScan(basePackageClasses = CartService.class)
    static class CartPackage {
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(CartPackage.class)
            .withBean(BookCatalog.class);

    /**
     * The cart beans with a store that the test holds on to, like Redis or a database table that outlives the
     * application. Starting a second context with the same store simulates a restart.
     */
    private static ApplicationContextRunner runnerWith(SavedCartStore store) {
        return new ApplicationContextRunner()
                .withUserConfiguration(ShoppingCart.class, CartService.class)
                .withBean(BookCatalog.class)
                .withBean(SavedCartStore.class, () -> store);
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("CartService is a singleton: the context hands out the same service every time")
        void cartServiceIsASingleton() {
            runner.run(context -> {
                CartService first = singleBean(context, CartService.class);

                assertThat(context.getBean(CartService.class)).isSameAs(first);
            });
        }

        @Test
        @DisplayName("ShoppingCart is a prototype: the context creates a new cart every time")
        void shoppingCartIsAPrototype() {
            runner.run(context -> {
                ShoppingCart first = singleBean(context, ShoppingCart.class);

                assertThat(context.getBean(ShoppingCart.class)).isNotSameAs(first);
            });
        }

        @Test
        @DisplayName("a member gets the same cart on every call")
        void sameCartForTheSameMember() {
            runner.run(context -> {
                CartService carts = singleBean(context, CartService.class);

                assertThat(carts.cartOf(1)).isSameAs(carts.cartOf(1));
            });
        }

        @Test
        @DisplayName("two members never share a cart")
        void membersDontShareCarts() {
            runner.run(context -> {
                CartService carts = singleBean(context, CartService.class);

                carts.cartOf(1).add(1, 2);

                assertThat(carts.cartOf(2)).isNotSameAs(carts.cartOf(1));
                assertThat(carts.cartOf(2).isEmpty()).as("member 2's cart is empty").isTrue();
                assertThat(carts.openCarts()).isEqualTo(2);
            });
        }

        @Test
        @DisplayName("saves the open carts in the store when the application stops")
        void savesOpenCartsAtShutdown() {
            var store = new SavedCartStore();

            runnerWith(store).run(context -> {
                CartService carts = singleBean(context, CartService.class);
                carts.cartOf(1).add(1, 2);
                carts.cartOf(1).add(23, 1);
            });

            assertThat(store.findAll()).isEqualTo(Map.of(1L, List.of(new CartLine(1, 2), new CartLine(23, 1))));
        }

        @Test
        @DisplayName("restores the saved carts when the application starts")
        void restoresSavedCartsAtStartup() {
            var store = new SavedCartStore();
            store.save(7, List.of(new CartLine(1, 2), new CartLine(23, 1)));

            runnerWith(store).run(context -> {
                CartService carts = singleBean(context, CartService.class);

                assertThat(carts.cartOf(7).lines()).containsExactly(new CartLine(1, 2), new CartLine(23, 1));
            });
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("restores the carts during startup, before anyone asks for one, and empties the store")
        void restoresBeforeFirstUse() {
            var store = new SavedCartStore();
            store.save(7, List.of(new CartLine(1, 2)));
            store.save(8, List.of(new CartLine(2, 1)));

            runnerWith(store).run(context -> {
                CartService carts = singleBean(context, CartService.class);

                assertThat(carts.openCarts()).as("open carts right after startup").isEqualTo(2);
                assertThat(store.findAll()).as("saved carts once they're open again").isEmpty();
            });
        }

        @Test
        @DisplayName("saves the carts only when the application stops, not while it runs")
        void savesOnlyAtShutdown() {
            var store = new SavedCartStore();

            runnerWith(store).run(context -> {
                CartService carts = singleBean(context, CartService.class);
                carts.cartOf(1).add(1, 1);

                assertThat(store.findAll()).as("saved carts while the application runs").isEmpty();
            });

            assertThat(store.findAll()).as("saved carts after the application stopped").containsOnlyKeys(1L);
        }

        @Test
        @DisplayName("each cart gets the catalog injected, restored carts included")
        void cartsGetTheirDependencies() {
            var store = new SavedCartStore();
            store.save(7, List.of(new CartLine(1, 2), new CartLine(23, 1)));

            runnerWith(store).run(context -> {
                CartService carts = singleBean(context, CartService.class);
                carts.cartOf(1).add(2, 1);

                assertThat(carts.cartOf(1).totalAmount()).isEqualTo(12_000);
                assertThat(carts.cartOf(7).totalAmount()).isEqualTo(2 * 36_000 + 20_000);
            });
        }

        @Test
        @DisplayName("carts survive a restart: the second application gets new cart objects with the same books")
        void cartsSurviveARestart() {
            var store = new SavedCartStore();
            var before = new AtomicReference<ShoppingCart>();
            runnerWith(store).run(context -> {
                before.set(singleBean(context, CartService.class).cartOf(3));
                before.get().add(41, 1);
            });

            runnerWith(store).run(context -> {
                ShoppingCart after = singleBean(context, CartService.class).cartOf(3);

                assertThat(after).isNotSameAs(before.get());
                assertThat(after.lines()).containsExactly(new CartLine(41, 1));
            });
        }

        @Test
        @DisplayName("doesn't save empty carts")
        void doesNotSaveEmptyCarts() {
            var store = new SavedCartStore();

            runnerWith(store).run(context -> {
                CartService carts = singleBean(context, CartService.class);
                carts.cartOf(1);
                carts.cartOf(2).add(1, 1);
            });

            assertThat(store.findAll()).containsOnlyKeys(2L);
        }

        @Test
        @DisplayName("after discard(), the member's next cart is a new, empty cart")
        void discardStartsANewCart() {
            runner.run(context -> {
                CartService carts = singleBean(context, CartService.class);
                ShoppingCart first = carts.cartOf(1);
                first.add(1, 1);

                carts.discard(1);

                assertThat(carts.openCarts()).isZero();
                assertThat(carts.cartOf(1)).isNotSameAs(first);
                assertThat(carts.cartOf(1).isEmpty()).as("the new cart is empty").isTrue();
            });
        }

        @Test
        @DisplayName("discard() does nothing for a member without a cart")
        void discardWithoutACart() {
            runner.run(context -> {
                CartService carts = singleBean(context, CartService.class);
                carts.cartOf(1);

                carts.discard(2);

                assertThat(carts.openCarts()).isEqualTo(1);
            });
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("cartOf() rejects a member id below 1")
        void rejectsAnInvalidMemberId() {
            runner.run(context -> {
                CartService carts = singleBean(context, CartService.class);

                assertThatThrownBy(() -> carts.cartOf(0)).isInstanceOf(IllegalArgumentException.class);
                assertThatThrownBy(() -> carts.cartOf(-1)).isInstanceOf(IllegalArgumentException.class);
                assertThat(carts.openCarts()).isZero();
            });
        }
    }
}
