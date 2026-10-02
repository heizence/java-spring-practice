package practice.j09.ex04.sample.shop;

/** Takes payments. */
public interface PaymentGateway {

    /** Charges the amount and returns a payment id. */
    String charge(long cents);
}
