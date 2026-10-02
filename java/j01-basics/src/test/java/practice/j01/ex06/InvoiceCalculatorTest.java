package practice.j01.ex06;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("J01-06 · Debugging and stack traces")
class InvoiceCalculatorTest {

    private static final InvoiceLine CLEAN_CODE = new InvoiceLine("Clean Code", 2, 3_000);      // 60.00
    private static final InvoiceLine REFACTORING = new InvoiceLine("Refactoring", 1, 4_500);    // 45.00

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("charges every line of the invoice")
        void chargesEveryLine() {
            InvoiceLine[] lines = {CLEAN_CODE, REFACTORING};

            assertThat(InvoiceCalculator.subtotalCents(lines)).isEqualTo(10_500);
        }

        @Test
        @DisplayName("charges a one-line invoice for that line")
        void chargesSingleLine() {
            InvoiceLine[] lines = {REFACTORING};

            assertThat(InvoiceCalculator.subtotalCents(lines)).isEqualTo(4_500);
        }

        @Test
        @DisplayName("takes 10 % off for WELCOME10")
        void welcomeCoupon() {
            InvoiceLine[] lines = {CLEAN_CODE, REFACTORING};

            assertThat(InvoiceCalculator.totalCents(lines, "WELCOME10")).isEqualTo(9_450);
        }

        @Test
        @DisplayName("lets a customer without a coupon check out")
        void checkoutWithoutCoupon() {
            InvoiceLine[] lines = {REFACTORING};

            assertThat(Checkout.confirm("A-1001", lines, null)).isEqualTo("Order A-1001: total 45.00");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("an invoice without lines costs nothing")
        void emptyInvoice() {
            assertThat(InvoiceCalculator.totalCents(new InvoiceLine[0], "WELCOME10")).isZero();
        }

        @Test
        @DisplayName("10 % of 1995 cents is 199 cents: the discount is rounded down")
        void discountRoundsDown() {
            assertThat(InvoiceCalculator.discountCents(1_995, 10)).isEqualTo(199);
        }

        @Test
        @DisplayName("takes 25 % off for BOOKWORM25")
        void bookwormCoupon() {
            assertThat(InvoiceCalculator.discountPercent("BOOKWORM25")).isEqualTo(25);
            assertThat(InvoiceCalculator.discountCents(10_000, 25)).isEqualTo(2_500);
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"welcome10", " WELCOME10 ", "Welcome10\t"})
        @DisplayName("ignores letter case and spaces around a coupon code")
        void normalizesCouponCode(String code) {
            assertThat(InvoiceCalculator.discountPercent(code)).isEqualTo(10);
        }

        @Test
        @DisplayName("a null coupon code means no discount")
        void nullCoupon() {
            assertThat(InvoiceCalculator.discountPercent(null)).isZero();
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"", "   "})
        @DisplayName("a blank coupon code means no discount")
        void blankCoupon(String code) {
            assertThat(InvoiceCalculator.discountPercent(code)).isZero();
        }

        @Test
        @DisplayName("an unknown coupon code gives no discount")
        void unknownCoupon() {
            assertThat(InvoiceCalculator.discountPercent("SUMMER99")).isZero();
        }

        @Test
        @DisplayName("the total is the subtotal minus the rounded-down discount")
        void totalWithRounding() {
            InvoiceLine[] lines = {new InvoiceLine("Pocket guide", 1, 1_995)};

            assertThat(InvoiceCalculator.totalCents(lines, "welcome10")).isEqualTo(1_796);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("subtotalCents() rejects a null array of lines")
        void nullLines() {
            assertThatThrownBy(() -> InvoiceCalculator.subtotalCents(null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Checkout names the order and keeps the original exception as the cause")
        void checkoutWrapsFailures() {
            assertThatThrownBy(() -> Checkout.confirm("A-1002", null, "WELCOME10"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Checkout failed for order A-1002")
                    .hasCauseInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("an invoice line rejects a quantity below 1 and a negative price")
        void invalidLine() {
            assertThatThrownBy(() -> new InvoiceLine("Clean Code", 0, 3_000))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new InvoiceLine("Clean Code", 1, -1))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
