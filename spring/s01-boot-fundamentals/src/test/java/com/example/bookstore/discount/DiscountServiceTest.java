package com.example.bookstore.discount;

import static com.example.bookstore.support.ContextAssertions.assertStartupFailed;
import static com.example.bookstore.support.ContextAssertions.singleBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.bookstore.member.MemberGrade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;

/** Tests the discount beans in a context that holds what component scanning finds in their package. */
@DisplayName("S01-03 · Choosing between implementations")
class DiscountServiceTest {

    /** Scans {@code com.example.bookstore.discount}: the four policies and {@link DiscountService}. */
    @ComponentScan(basePackageClasses = DiscountService.class)
    static class DiscountPackage {
    }

    /** A bean that asks for "a DiscountPolicy" without saying which one, as most code does. */
    record NeedsAPolicy(DiscountPolicy policy) {
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(DiscountPackage.class);

    @Nested
    @DisplayName("Basic")
    class Basic {

        @ParameterizedTest(name = "a {0} member gets {1} won off 100,000 won", quoteTextArguments = false)
        @CsvSource({"BASIC, 0", "SILVER, 5000", "GOLD, 10000"})
        @DisplayName("each grade gets the policy named after it")
        void eachGradeGetsItsPolicy(MemberGrade grade, long expectedDiscount) {
            runner.run(context -> {
                DiscountService discounts = singleBean(context, DiscountService.class);

                assertThat(discounts.discountFor(grade, 100_000)).isEqualTo(expectedDiscount);
            });
        }

        @Test
        @DisplayName("a guest gets the default policy: no discount")
        void guestsGetTheDefaultPolicy() {
            runner.run(context -> {
                DiscountService discounts = singleBean(context, DiscountService.class);

                assertThat(discounts.guestDiscount(100_000)).isZero();
            });
        }

        @Test
        @DisplayName("a staff purchase gets the policy qualified \"staffDiscount\", although BasicDiscount is primary")
        void staffGetTheQualifiedPolicy() {
            runner.run(context -> {
                DiscountService discounts = singleBean(context, DiscountService.class);

                assertThat(discounts.staffDiscount(100_000)).isEqualTo(20_000);
            });
        }

        @Test
        @DisplayName("a bean that doesn't say which DiscountPolicy it wants gets BasicDiscount, the primary one")
        void plainInjectionGetsThePrimaryPolicy() {
            runner.withBean(NeedsAPolicy.class).run(context -> {
                NeedsAPolicy bean = singleBean(context, NeedsAPolicy.class);

                assertThat(bean.policy()).isInstanceOf(BasicDiscount.class);
            });
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("gives no discount on an amount of 0")
        void zeroAmount() {
            runner.run(context -> {
                DiscountService discounts = singleBean(context, DiscountService.class);

                for (MemberGrade grade : MemberGrade.values()) {
                    assertThat(discounts.discountFor(grade, 0)).as("discount for %s", grade).isZero();
                }
                assertThat(discounts.guestDiscount(0)).isZero();
                assertThat(discounts.staffDiscount(0)).isZero();
            });
        }

        @Test
        @DisplayName("passes the whole amount to the policy, which rounds down to whole won")
        void passesTheAmountUnchanged() {
            runner.run(context -> {
                DiscountService discounts = singleBean(context, DiscountService.class);

                assertThat(discounts.discountFor(MemberGrade.GOLD, 99_999)).isEqualTo(9_999);
                assertThat(discounts.discountFor(MemberGrade.SILVER, 99_999)).isEqualTo(4_999);
                assertThat(discounts.staffDiscount(99_999)).isEqualTo(19_999);
            });
        }

        @Test
        @DisplayName("a new policy bean that matches no grade changes nothing")
        void anExtraPolicyChangesNothing() {
            DiscountPolicy everythingFree = amount -> amount;

            runner.withBean("platinumDiscount", DiscountPolicy.class, () -> everythingFree).run(context -> {
                DiscountService discounts = singleBean(context, DiscountService.class);

                assertThat(discounts.discountFor(MemberGrade.GOLD, 100_000)).isEqualTo(10_000);
                assertThat(discounts.guestDiscount(100_000)).isZero();
                assertThat(discounts.staffDiscount(100_000)).isEqualTo(20_000);
            });
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("fails at startup when a grade has no policy, naming the grade and the missing bean")
        void failsAtStartupWithoutAPolicyForAGrade() {
            // No GoldDiscount in this context: the application must not start.
            new ApplicationContextRunner()
                    .withUserConfiguration(BasicDiscount.class, SilverDiscount.class, StaffDiscount.class,
                            DiscountService.class)
                    .run(context -> assertThat(assertStartupFailed(context))
                            .rootCause()
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("goldDiscount")
                            .hasMessageContaining("GOLD"));
        }

        @Test
        @DisplayName("rejects a negative amount")
        void rejectsANegativeAmount() {
            runner.run(context -> {
                DiscountService discounts = singleBean(context, DiscountService.class);

                assertThatThrownBy(() -> discounts.discountFor(MemberGrade.GOLD, -1))
                        .isInstanceOf(IllegalArgumentException.class);
                assertThatThrownBy(() -> discounts.guestDiscount(-1)).isInstanceOf(IllegalArgumentException.class);
                assertThatThrownBy(() -> discounts.staffDiscount(-1)).isInstanceOf(IllegalArgumentException.class);
            });
        }

        @Test
        @DisplayName("rejects a null grade with NullPointerException")
        void rejectsANullGrade() {
            runner.run(context -> {
                DiscountService discounts = singleBean(context, DiscountService.class);

                assertThatThrownBy(() -> discounts.discountFor(null, 100)).isInstanceOf(NullPointerException.class);
            });
        }
    }
}
