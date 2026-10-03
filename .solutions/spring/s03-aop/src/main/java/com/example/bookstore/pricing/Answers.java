package com.example.bookstore.pricing;

import com.example.bookstore.pricing.ProxyPuzzles.AdviceType;
import com.example.bookstore.pricing.ProxyPuzzles.Probe;
import com.example.bookstore.pricing.ProxyPuzzles.ProxyType;

/**
 * Your answer sheet for S03-04. Read {@link ProxyPuzzles}, replace each {@code null} with your prediction, then run
 * {@code ProxyPuzzlesTest}.
 *
 * <p>Every puzzle starts a new context from {@link ProxyPuzzles.Setup}, so nothing carries over from one puzzle to
 * the next. "The {@code priceCalculator} bean" means the object that {@code context.getBean(PriceCalculator.class)}
 * returns, which is also what Spring injects into other beans. Write enums like
 * {@code ProxyType.NOT_A_PROXY}, booleans as {@code true} or {@code false}, and numbers like {@code 123L}.
 */
final class Answers {

    private Answers() {
    }

    // Which object you get

    /** 1. What is the {@code priceCalculator} bean? */
    // CallRecorder, AuditTrail and Metrics advise it, and the class has no interface: only a subclass can stand in.
    static final ProxyType PRICE_CALCULATOR = ProxyType.CGLIB_PROXY;

    /** 2. What is the {@code shippingFees} bean? Its class implements the interface {@code ShippingFees}. */
    // Spring Boot sets spring.aop.proxy-target-class=true, so it generates a subclass even when there's an interface.
    static final ProxyType SHIPPING_FEES = ProxyType.CGLIB_PROXY;

    /** 3. What is the {@code shippingFees} bean in a context with spring.aop.proxy-target-class=false? */
    // Plain Spring's rule: a bean with interfaces gets a JDK proxy that implements them, as in J09-03.
    static final ProxyType SHIPPING_FEES_WITHOUT_CLASS_PROXIES = ProxyType.JDK_DYNAMIC_PROXY;

    /** 4. What is the {@code taxRates} bean? */
    // Spring wraps a bean only when at least one piece of advice applies to it. No pointcut names taxRates.
    static final ProxyType TAX_RATES = ProxyType.NOT_A_PROXY;

    /** 5. Is {@code priceCalculator.getClass() == PriceCalculator.class} true? */
    // Its class is the generated subclass, named like PriceCalculator$$SpringCGLIB$$0.
    static final Boolean PRICE_CALCULATOR_CLASS_IS_PRICE_CALCULATOR = false;

    /** 6. Is {@code priceCalculator instanceof PriceCalculator} true? */
    // An object of a subclass is an instance of its superclass, so injecting PriceCalculator works.
    static final Boolean PRICE_CALCULATOR_IS_INSTANCE_OF_PRICE_CALCULATOR = true;

    /**
     * 7. With spring.aop.proxy-target-class=false, the setup plus a {@code ShippingQuotes} bean: does the context
     * start?
     */
    // The JDK proxy implements ShippingFees but isn't a FlatShippingFees, so ShippingQuotes can't get one: startup
    // fails. Inject interfaces, or keep Boot's class proxies.
    static final Boolean STARTS_WITH_SHIPPING_QUOTES_WITHOUT_CLASS_PROXIES = false;

    // Which calls the advice sees

    /** 8. After {@code priceCalculator.orderTotal(36_000, 2)}: has the {@code CallRecorder} recorded linePrice? */
    // linePrice(...) inside orderTotal means this.linePrice(...): a plain call inside the target, past the proxy.
    static final Boolean INNER_CALL_RECORDED = false;

    /** 9. After {@code priceCalculator.rounded(12_345)}: has it recorded roundDownToHundreds? */
    // A private method can't be overridden by the proxy, and it's only ever called from inside the target.
    static final Boolean PRIVATE_CALL_RECORDED = false;

    /** 10. After {@code priceCalculator.withVat(10_000)}: has it recorded withVat? */
    // A final method can't be overridden, so the proxy can't put advice around it.
    static final Boolean FINAL_CALL_RECORDED = false;

    /** 11. What does {@code priceCalculator.withVat(10_000)} return? */
    // The final method runs on the proxy object itself. Spring created the proxy without running a constructor, so its
    // copy of vatPercent is 0: the VAT silently disappears. With an object field, this would be a NullPointerException.
    static final Long FINAL_CALL_RESULT = 10_000L;

    // In which order the advice runs

    /** 12. During {@code priceCalculator.linePrice(36_000, 2)}: which aspect's advice starts first? */
    // The lower @Order value has the higher precedence: Metrics (1) is the outer layer, so it starts first...
    static final Probe FIRST_TO_START = Probe.METRICS;

    /** 13. And which one ends first? */
    // ...and ends last. The inner layer, AuditTrail (2), ends first.
    static final Probe FIRST_TO_END = Probe.AUDIT_TRAIL;

    /** 14. During {@code shippingFees.feeFor(30_000)}: which advice of {@code Diagnostics} runs first? */
    // Within one aspect, @Around has the highest precedence, so it starts before @Before.
    static final AdviceType FIRST_ADVICE = AdviceType.AROUND;

    /** 15. And which advice of {@code Diagnostics} runs first after {@code feeFor} has returned? */
    // @After works like a finally block around @AfterReturning, so @AfterReturning runs first; @Around ends last.
    static final AdviceType FIRST_ADVICE_AFTER_THE_METHOD = AdviceType.AFTER_RETURNING;
}
