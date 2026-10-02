package practice.j09.ex04.sample.shop;

import java.util.ArrayList;
import java.util.List;

import practice.j09.ex04.Component;

/** Remembers the charges instead of taking real money. */
@Component
public class FakePaymentGateway implements PaymentGateway {

    private final List<Long> charges = new ArrayList<>();

    @Override
    public String charge(long cents) {
        charges.add(cents);
        return "payment-" + charges.size();
    }

    public List<Long> charges() {
        return List.copyOf(charges);
    }
}
