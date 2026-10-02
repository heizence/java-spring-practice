package practice.j03.ex02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.entry;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("J03-02 · Designing exceptions")
class OrderValidatorTest {

    /** A fake inventory: three known products, and NoSuchElementException for every other SKU. */
    private static final Inventory INVENTORY = sku -> switch (sku) {
        case "BOOK-1" -> 5;
        case "BOOK-SOLD-OUT" -> 0;
        case "BOOK-PLENTY" -> 1_000;
        default -> throw new NoSuchElementException("no product row for " + sku);
    };

    private static OrderValidator validator() {
        return new OrderValidator(INVENTORY);
    }

    /** A direct subclass, so that tests of the base class can keep a reference to the details map they pass in. */
    private static final class TestException extends BusinessException {

        TestException(ErrorCode code, Map<String, Object> details) {
            super(code, details);
        }
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("accepts an order line that is in stock")
        void acceptsValidLine() {
            assertThatCode(() -> validator().validate("BOOK-1", 2)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("rejects a quantity of 0 with INVALID_QUANTITY, naming the bad value")
        void rejectsZeroQuantity() {
            var thrown = catchThrowableOfType(InvalidQuantityException.class, () -> validator().validate("BOOK-1", 0));

            assertThat(thrown).hasMessage("Quantity must be between 1 and 100, but was 0");
            assertThat(thrown.code()).isEqualTo(ErrorCode.INVALID_QUANTITY);
            assertThat(thrown.details()).containsOnly(entry("quantity", 0), entry("min", 1), entry("max", 100));
        }

        @Test
        @DisplayName("rejects an unknown SKU with PRODUCT_NOT_FOUND, naming the SKU")
        void rejectsUnknownSku() {
            var thrown = catchThrowableOfType(ProductNotFoundException.class,
                    () -> validator().validate("BOOK-404", 1));

            assertThat(thrown).hasMessage("No product with SKU BOOK-404");
            assertThat(thrown.code()).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
            assertThat(thrown.details()).containsOnly(entry("sku", "BOOK-404"));
        }

        @Test
        @DisplayName("rejects an order larger than the stock with OUT_OF_STOCK, naming every value")
        void rejectsTooLargeOrder() {
            var thrown = catchThrowableOfType(OutOfStockException.class, () -> validator().validate("BOOK-1", 6));

            assertThat(thrown).hasMessage("Not enough stock for BOOK-1: requested 6, available 5");
            assertThat(thrown.code()).isEqualTo(ErrorCode.OUT_OF_STOCK);
            assertThat(thrown.details())
                    .containsOnly(entry("sku", "BOOK-1"), entry("requested", 6), entry("available", 5));
        }

        @Test
        @DisplayName("turns an exception into an error response with its status, code, message and details")
        void buildsErrorResponse() {
            var exception = new OutOfStockException("BOOK-1", 6, 5);

            ErrorResponse response = ErrorResponse.from(exception);

            assertThat(response.status()).isEqualTo(409);
            assertThat(response.code()).isEqualTo("ORDER-001");
            assertThat(response.message()).isEqualTo("Not enough stock for BOOK-1: requested 6, available 5");
            assertThat(response.details())
                    .containsOnly(entry("sku", "BOOK-1"), entry("requested", 6), entry("available", 5));
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @ParameterizedTest(name = "quantity {0}")
        @ValueSource(ints = {1, 100})
        @DisplayName("accepts the smallest and the largest quantity")
        void acceptsQuantityLimits(int quantity) {
            assertThatCode(() -> validator().validate("BOOK-PLENTY", quantity)).doesNotThrowAnyException();
        }

        @ParameterizedTest(name = "quantity {0}")
        @ValueSource(ints = {0, 101, -1, Integer.MIN_VALUE, Integer.MAX_VALUE})
        @DisplayName("rejects quantities outside 1 to 100")
        void rejectsQuantitiesOutsideLimits(int quantity) {
            assertThatThrownBy(() -> validator().validate("BOOK-PLENTY", quantity))
                    .isInstanceOf(InvalidQuantityException.class)
                    .hasMessageEndingWith("but was " + quantity);
        }

        @Test
        @DisplayName("accepts an order for exactly the units that are left")
        void acceptsExactlyRemainingStock() {
            assertThatCode(() -> validator().validate("BOOK-1", 5)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("reports a product with no stock as out of stock, not as missing")
        void soldOutIsOutOfStock() {
            assertThatThrownBy(() -> validator().validate("BOOK-SOLD-OUT", 1))
                    .isInstanceOf(OutOfStockException.class)
                    .hasMessage("Not enough stock for BOOK-SOLD-OUT: requested 1, available 0");
        }

        @Test
        @DisplayName("checks the quantity before looking up the stock")
        void checksQuantityBeforeLookup() {
            var validator = new OrderValidator(sku -> {
                throw new AssertionError("the inventory must not be asked when the quantity is invalid");
            });

            assertThatThrownBy(() -> validator.validate("BOOK-1", 0)).isInstanceOf(InvalidQuantityException.class);
        }

        @Test
        @DisplayName("maps each error code to its own HTTP status")
        void statusPerErrorCode() {
            assertThat(ErrorResponse.from(new InvalidQuantityException(0, 1, 100)).status()).isEqualTo(400);
            assertThat(ErrorResponse.from(new ProductNotFoundException("BOOK-404")).status()).isEqualTo(404);
            assertThat(ErrorResponse.from(new OutOfStockException("BOOK-1", 6, 5)).status()).isEqualTo(409);
        }

        @Test
        @DisplayName("details() can't be modified")
        void detailsAreUnmodifiable() {
            var exception = new ProductNotFoundException("BOOK-404");

            assertThatThrownBy(() -> exception.details().put("sku", "OTHER"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("details() is a copy: changing the map passed in later doesn't change the exception")
        void detailsAreCopied() {
            Map<String, Object> details = new HashMap<>();
            details.put("sku", "BOOK-1");
            var exception = new TestException(ErrorCode.PRODUCT_NOT_FOUND, details);

            details.put("sku", "CHANGED");

            assertThat(exception.details()).containsOnly(entry("sku", "BOOK-1"));
            assertThat(exception).hasMessage("No product with SKU BOOK-1");
        }

        @Test
        @DisplayName("ProductNotFoundException keeps a cause that is passed to it")
        void productNotFoundKeepsCause() {
            var cause = new NoSuchElementException("no row");

            var exception = new ProductNotFoundException("BOOK-404", cause);

            assertThat(exception).hasMessage("No product with SKU BOOK-404").cause().isSameAs(cause);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("keeps the inventory's NoSuchElementException as the cause of ProductNotFoundException")
        void translatesNotFoundAndKeepsCause() {
            var notFound = new NoSuchElementException("no product row for BOOK-404");
            var validator = new OrderValidator(sku -> {
                throw notFound;
            });

            assertThatThrownBy(() -> validator.validate("BOOK-404", 1))
                    .isInstanceOf(ProductNotFoundException.class)
                    .cause().isSameAs(notFound);
        }

        @Test
        @DisplayName("lets any other inventory failure propagate unchanged")
        void otherFailuresPropagate() {
            var outage = new IllegalStateException("inventory database is down");
            var validator = new OrderValidator(sku -> {
                throw outage;
            });

            assertThatThrownBy(() -> validator.validate("BOOK-1", 1)).isSameAs(outage);
        }

        @Test
        @DisplayName("rejects a null SKU with NullPointerException")
        void rejectsNullSku() {
            assertThatThrownBy(() -> validator().validate(null, 1)).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("rejects a null inventory")
        void rejectsNullInventory() {
            assertThatThrownBy(() -> new OrderValidator(null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("rejects a null code, a null details map and null values in it")
        void rejectsNullsInBaseClass() {
            Map<String, Object> withNullValue = new HashMap<>();
            withNullValue.put("sku", null);

            assertThatThrownBy(() -> new TestException(null, Map.of("sku", "BOOK-1")))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new TestException(ErrorCode.PRODUCT_NOT_FOUND, null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new TestException(ErrorCode.PRODUCT_NOT_FOUND, withNullValue))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
