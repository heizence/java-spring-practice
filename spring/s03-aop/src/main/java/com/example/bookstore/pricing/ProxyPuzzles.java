package com.example.bookstore.pricing;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;

/**
 * S03-04 · Predict: proxies. Read this file, then write your predictions in {@link Answers}. Don't run anything yet.
 *
 * <h2>The setup</h2>
 * <p>The tests start a small application context from {@link Setup} and Spring Boot's AOP auto-configuration, the same
 * auto-configuration that the bookstore uses. Then they look at the beans, call them, and compare what really happens
 * with your answers.
 *
 * <p>These classes are not part of the bookstore. They have no {@code @Component} or {@code @Configuration}
 * annotation, so the bookstore application never creates them; only the tests do.
 */
public final class ProxyPuzzles {

    private ProxyPuzzles() {
    }

    // ---- The beans -------------------------------------------------------------------------------------------------

    /** Shipping fees: an interface with one implementation, as in many older code bases. */
    public interface ShippingFees {

        long feeFor(long orderAmount);
    }

    /** Free shipping from 50,000 won; otherwise 3,000 won. */
    public static class FlatShippingFees implements ShippingFees {

        @Override
        public long feeFor(long orderAmount) {
            return orderAmount >= 50_000 ? 0 : 3_000;
        }
    }

    /** Prices of order lines: a class without an interface. */
    public static class PriceCalculator {

        private final int vatPercent;

        public PriceCalculator(int vatPercent) {
            this.vatPercent = vatPercent;
        }

        public long linePrice(long unitPrice, int quantity) {
            return unitPrice * quantity;
        }

        /** Calls {@link #linePrice} on {@code this}. */
        public long orderTotal(long unitPrice, int quantity) {
            return linePrice(unitPrice, quantity);
        }

        /** A {@code final} method: no subclass can override it. */
        public final long withVat(long price) {
            return price + price * vatPercent / 100;
        }

        /** Calls a {@code private} method. */
        public long rounded(long price) {
            return roundDownToHundreds(price);
        }

        private long roundDownToHundreds(long price) {
            return price / 100 * 100;
        }
    }

    /** Tax rates. No pointcut below matches this bean. */
    public static class TaxRates {

        public int standardVatPercent() {
            return 10;
        }
    }

    /** Asks for the class {@link FlatShippingFees}, not for the interface {@link ShippingFees}. */
    public static class ShippingQuotes {

        private final FlatShippingFees shippingFees;

        public ShippingQuotes(FlatShippingFees shippingFees) {
            this.shippingFees = shippingFees;
        }

        public long quote(long orderAmount) {
            return shippingFees.feeFor(orderAmount);
        }
    }

    // ---- The aspects -----------------------------------------------------------------------------------------------

    /** Remembers the name of every method call that its advice sees, in order. */
    @Aspect
    public static class CallRecorder {

        private final List<String> calls = new CopyOnWriteArrayList<>();

        /** {@code bean(name)}: every method of the bean with that name. */
        @Before("bean(priceCalculator) || bean(shippingFees)")
        public void record(JoinPoint call) {
            calls.add(call.getSignature().getName());
        }

        public List<String> calls() {
            return List.copyOf(calls);
        }
    }

    /** Where the next three aspects write what they do, in order. */
    public static class Events {

        private final List<String> events = new CopyOnWriteArrayList<>();

        public void add(String event) {
            events.add(event);
        }

        public List<String> all() {
            return List.copyOf(events);
        }
    }

    /** Around {@code linePrice}, with {@code @Order(2)}. */
    @Aspect
    @Order(2)
    public static class AuditTrail {

        private final Events events;

        public AuditTrail(Events events) {
            this.events = events;
        }

        @Around("bean(priceCalculator) && execution(* linePrice(..))")
        public Object audit(ProceedingJoinPoint call) throws Throwable {
            events.add("AuditTrail starts");
            try {
                return call.proceed();
            } finally {
                events.add("AuditTrail ends");
            }
        }
    }

    /** Around {@code linePrice} too, with {@code @Order(1)}. */
    @Aspect
    @Order(1)
    public static class Metrics {

        private final Events events;

        public Metrics(Events events) {
            this.events = events;
        }

        @Around("bean(priceCalculator) && execution(* linePrice(..))")
        public Object measure(ProceedingJoinPoint call) throws Throwable {
            events.add("Metrics starts");
            try {
                return call.proceed();
            } finally {
                events.add("Metrics ends");
            }
        }
    }

    /** Four kinds of advice in one aspect, all on {@code feeFor}. */
    @Aspect
    public static class Diagnostics {

        private static final String FEE_FOR = "bean(shippingFees) && execution(* feeFor(..))";

        private final Events events;

        public Diagnostics(Events events) {
            this.events = events;
        }

        @Around(FEE_FOR)
        public Object around(ProceedingJoinPoint call) throws Throwable {
            events.add("around starts");
            try {
                return call.proceed();
            } finally {
                events.add("around ends");
            }
        }

        @Before(FEE_FOR)
        public void before() {
            events.add("before");
        }

        @AfterReturning(FEE_FOR)
        public void afterReturning() {
            events.add("afterReturning");
        }

        @After(FEE_FOR)
        public void after() {
            events.add("after");
        }
    }

    // ---- The beans the tests register -----------------------------------------------------------------------------

    /**
     * The beans of the puzzles, with their names. {@link ShippingQuotes} is not here: only the puzzle about it adds
     * it.
     */
    public static class Setup {

        @Bean
        public PriceCalculator priceCalculator() {
            return new PriceCalculator(10);
        }

        @Bean
        public FlatShippingFees shippingFees() {
            return new FlatShippingFees();
        }

        @Bean
        public TaxRates taxRates() {
            return new TaxRates();
        }

        @Bean
        public CallRecorder callRecorder() {
            return new CallRecorder();
        }

        @Bean
        public Events events() {
            return new Events();
        }

        @Bean
        public AuditTrail auditTrail(Events events) {
            return new AuditTrail(events);
        }

        @Bean
        public Metrics metrics(Events events) {
            return new Metrics(events);
        }

        @Bean
        public Diagnostics diagnostics(Events events) {
            return new Diagnostics(events);
        }
    }

    // ---- The words for your answers -------------------------------------------------------------------------------

    /** What kind of object a bean is. */
    public enum ProxyType {
        /** The object itself, not a proxy. */
        NOT_A_PROXY,
        /** A {@code java.lang.reflect.Proxy} that implements the bean's interfaces (J09-03). */
        JDK_DYNAMIC_PROXY,
        /** A subclass of the bean's class, generated at runtime. */
        CGLIB_PROXY
    }

    /** The two aspects around {@code linePrice}. */
    public enum Probe {
        AUDIT_TRAIL,
        METRICS
    }

    /** The kinds of advice in {@link Diagnostics}. */
    public enum AdviceType {
        AROUND,
        BEFORE,
        AFTER_RETURNING,
        AFTER
    }
}
