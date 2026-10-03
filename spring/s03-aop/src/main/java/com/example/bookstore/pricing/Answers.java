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
    static final ProxyType PRICE_CALCULATOR = null;

    /** 2. What is the {@code shippingFees} bean? Its class implements the interface {@code ShippingFees}. */
    static final ProxyType SHIPPING_FEES = null;

    /** 3. What is the {@code shippingFees} bean in a context with spring.aop.proxy-target-class=false? */
    static final ProxyType SHIPPING_FEES_WITHOUT_CLASS_PROXIES = null;

    /** 4. What is the {@code taxRates} bean? */
    static final ProxyType TAX_RATES = null;

    /** 5. Is {@code priceCalculator.getClass() == PriceCalculator.class} true? */
    static final Boolean PRICE_CALCULATOR_CLASS_IS_PRICE_CALCULATOR = null;

    /** 6. Is {@code priceCalculator instanceof PriceCalculator} true? */
    static final Boolean PRICE_CALCULATOR_IS_INSTANCE_OF_PRICE_CALCULATOR = null;

    /**
     * 7. With spring.aop.proxy-target-class=false, the setup plus a {@code ShippingQuotes} bean: does the context
     * start?
     */
    static final Boolean STARTS_WITH_SHIPPING_QUOTES_WITHOUT_CLASS_PROXIES = null;

    // Which calls the advice sees

    /** 8. After {@code priceCalculator.orderTotal(36_000, 2)}: has the {@code CallRecorder} recorded linePrice? */
    static final Boolean INNER_CALL_RECORDED = null;

    /** 9. After {@code priceCalculator.rounded(12_345)}: has it recorded roundDownToHundreds? */
    static final Boolean PRIVATE_CALL_RECORDED = null;

    /** 10. After {@code priceCalculator.withVat(10_000)}: has it recorded withVat? */
    static final Boolean FINAL_CALL_RECORDED = null;

    /** 11. What does {@code priceCalculator.withVat(10_000)} return? */
    static final Long FINAL_CALL_RESULT = null;

    // In which order the advice runs

    /** 12. During {@code priceCalculator.linePrice(36_000, 2)}: which aspect's advice starts first? */
    static final Probe FIRST_TO_START = null;

    /** 13. And which one ends first? */
    static final Probe FIRST_TO_END = null;

    /** 14. During {@code shippingFees.feeFor(30_000)}: which advice of {@code Diagnostics} runs first? */
    static final AdviceType FIRST_ADVICE = null;

    /** 15. And which advice of {@code Diagnostics} runs first after {@code feeFor} has returned? */
    static final AdviceType FIRST_ADVICE_AFTER_THE_METHOD = null;
}
