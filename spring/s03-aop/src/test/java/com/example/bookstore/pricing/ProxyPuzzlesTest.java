package com.example.bookstore.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import com.example.bookstore.pricing.ProxyPuzzles.AdviceType;
import com.example.bookstore.pricing.ProxyPuzzles.CallRecorder;
import com.example.bookstore.pricing.ProxyPuzzles.Events;
import com.example.bookstore.pricing.ProxyPuzzles.PriceCalculator;
import com.example.bookstore.pricing.ProxyPuzzles.Probe;
import com.example.bookstore.pricing.ProxyPuzzles.ProxyType;
import com.example.bookstore.pricing.ProxyPuzzles.ShippingFees;
import com.example.bookstore.pricing.ProxyPuzzles.ShippingQuotes;
import com.example.bookstore.pricing.ProxyPuzzles.TaxRates;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import practice.testkit.Predict;

/**
 * Runs each puzzle of {@link ProxyPuzzles} in a fresh application context and compares what really happens with
 * {@link Answers}. The context has Spring Boot's AOP auto-configuration, as the bookstore application does.
 */
@DisplayName("S03-04 · Predict: proxies")
class ProxyPuzzlesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AopAutoConfiguration.class))
            .withUserConfiguration(ProxyPuzzles.Setup.class);

    private final ApplicationContextRunner withoutClassProxies =
            contextRunner.withPropertyValues("spring.aop.proxy-target-class=false");

    @Nested
    @DisplayName("Which object you get")
    class WhichObject {

        @Test
        @DisplayName("1. the priceCalculator bean")
        void priceCalculator() {
            contextRunner.run(context ->
                    Predict.check(Answers.PRICE_CALCULATOR, proxyTypeOf(context.getBean(PriceCalculator.class))));
        }

        @Test
        @DisplayName("2. the shippingFees bean, whose class implements an interface")
        void shippingFees() {
            contextRunner.run(context ->
                    Predict.check(Answers.SHIPPING_FEES, proxyTypeOf(context.getBean(ShippingFees.class))));
        }

        @Test
        @DisplayName("3. the shippingFees bean with spring.aop.proxy-target-class=false")
        void shippingFeesWithoutClassProxies() {
            withoutClassProxies.run(context -> Predict.check(Answers.SHIPPING_FEES_WITHOUT_CLASS_PROXIES,
                    proxyTypeOf(context.getBean(ShippingFees.class))));
        }

        @Test
        @DisplayName("4. the taxRates bean, which no pointcut matches")
        void taxRates() {
            contextRunner.run(context ->
                    Predict.check(Answers.TAX_RATES, proxyTypeOf(context.getBean(TaxRates.class))));
        }

        @Test
        @DisplayName("5. priceCalculator.getClass() == PriceCalculator.class")
        void classOfPriceCalculator() {
            contextRunner.run(context -> Predict.check(Answers.PRICE_CALCULATOR_CLASS_IS_PRICE_CALCULATOR,
                    context.getBean(PriceCalculator.class).getClass() == PriceCalculator.class));
        }

        @Test
        @DisplayName("6. priceCalculator instanceof PriceCalculator")
        void priceCalculatorIsInstanceOfPriceCalculator() {
            contextRunner.run(context -> {
                Object priceCalculator = context.getBean("priceCalculator");

                Predict.check(Answers.PRICE_CALCULATOR_IS_INSTANCE_OF_PRICE_CALCULATOR,
                        priceCalculator instanceof PriceCalculator);
            });
        }

        @Test
        @DisplayName("7. a bean that needs the class FlatShippingFees, with spring.aop.proxy-target-class=false")
        void injectingTheClassWithoutClassProxies() {
            withoutClassProxies.withBean(ShippingQuotes.class).run(context ->
                    Predict.check(Answers.STARTS_WITH_SHIPPING_QUOTES_WITHOUT_CLASS_PROXIES,
                            context.getStartupFailure() == null));
        }
    }

    @Nested
    @DisplayName("Which calls the advice sees")
    class WhichCalls {

        @Test
        @DisplayName("8. orderTotal() calls linePrice() on this")
        void innerCall() {
            contextRunner.run(context -> {
                context.getBean(PriceCalculator.class).orderTotal(36_000, 2);

                Predict.check(Answers.INNER_CALL_RECORDED, recordedCalls(context).contains("linePrice"));
            });
        }

        @Test
        @DisplayName("9. rounded() calls a private method")
        void privateCall() {
            contextRunner.run(context -> {
                context.getBean(PriceCalculator.class).rounded(12_345);

                Predict.check(Answers.PRIVATE_CALL_RECORDED, recordedCalls(context).contains("roundDownToHundreds"));
            });
        }

        @Test
        @DisplayName("10. calling the final method withVat()")
        void finalCall() {
            contextRunner.run(context -> {
                context.getBean(PriceCalculator.class).withVat(10_000);

                Predict.check(Answers.FINAL_CALL_RECORDED, recordedCalls(context).contains("withVat"));
            });
        }

        @Test
        @DisplayName("11. the result of the final method withVat(10_000)")
        void finalCallResult() {
            contextRunner.run(context ->
                    Predict.check(Answers.FINAL_CALL_RESULT, context.getBean(PriceCalculator.class).withVat(10_000)));
        }
    }

    @Nested
    @DisplayName("In which order the advice runs")
    class AdviceOrder {

        private static final Map<String, Probe> STARTS = Map.of(
                "AuditTrail starts", Probe.AUDIT_TRAIL, "Metrics starts", Probe.METRICS);
        private static final Map<String, Probe> ENDS = Map.of(
                "AuditTrail ends", Probe.AUDIT_TRAIL, "Metrics ends", Probe.METRICS);
        private static final Map<String, AdviceType> ADVICE = Map.of(
                "around starts", AdviceType.AROUND, "before", AdviceType.BEFORE);
        private static final Map<String, AdviceType> ADVICE_AFTER_THE_METHOD = Map.of(
                "around ends", AdviceType.AROUND, "afterReturning", AdviceType.AFTER_RETURNING,
                "after", AdviceType.AFTER);

        @Test
        @DisplayName("12. which aspect's advice around linePrice() starts first")
        void firstToStart() {
            contextRunner.run(context -> {
                context.getBean(PriceCalculator.class).linePrice(36_000, 2);

                Predict.check(Answers.FIRST_TO_START, first(events(context), STARTS));
            });
        }

        @Test
        @DisplayName("13. which aspect's advice around linePrice() ends first")
        void firstToEnd() {
            contextRunner.run(context -> {
                context.getBean(PriceCalculator.class).linePrice(36_000, 2);

                Predict.check(Answers.FIRST_TO_END, first(events(context), ENDS));
            });
        }

        @Test
        @DisplayName("14. which advice of Diagnostics runs first")
        void firstAdvice() {
            contextRunner.run(context -> {
                context.getBean(ShippingFees.class).feeFor(30_000);

                Predict.check(Answers.FIRST_ADVICE, first(events(context), ADVICE));
            });
        }

        @Test
        @DisplayName("15. which advice of Diagnostics runs first after feeFor() has returned")
        void firstAdviceAfterTheMethod() {
            contextRunner.run(context -> {
                context.getBean(ShippingFees.class).feeFor(30_000);

                Predict.check(Answers.FIRST_ADVICE_AFTER_THE_METHOD, first(events(context), ADVICE_AFTER_THE_METHOD));
            });
        }

        /** The first event that the map knows, translated; {@code null} if there is none. */
        private static <T> T first(List<String> events, Map<String, T> known) {
            return events.stream()
                    .filter(known::containsKey)
                    .map(known::get)
                    .findFirst()
                    .orElse(null);
        }
    }

    private static ProxyType proxyTypeOf(Object bean) {
        if (AopUtils.isJdkDynamicProxy(bean)) {
            return ProxyType.JDK_DYNAMIC_PROXY;
        }
        if (AopUtils.isCglibProxy(bean)) {
            return ProxyType.CGLIB_PROXY;
        }
        return ProxyType.NOT_A_PROXY;
    }

    private static List<String> recordedCalls(ApplicationContext context) {
        return context.getBean(CallRecorder.class).calls();
    }

    private static List<String> events(ApplicationContext context) {
        return context.getBean(Events.class).all();
    }
}
