package practice.j09.ex04.sample.shop;

import practice.j09.ex04.Service;

/** Needs a class and an interface: the calculator, and whichever component implements PaymentGateway. */
@Service
public class CheckoutService {

    private final PriceCalculator calculator;
    private final PaymentGateway payments;

    public CheckoutService(PriceCalculator calculator, PaymentGateway payments) {
        this.calculator = calculator;
        this.payments = payments;
    }

    public Receipt checkout(String product, int quantity) {
        long total = calculator.totalCents(product, quantity);
        return new Receipt(product, quantity, total, payments.charge(total));
    }

    public PriceCalculator calculator() {
        return calculator;
    }

    public PaymentGateway payments() {
        return payments;
    }
}
