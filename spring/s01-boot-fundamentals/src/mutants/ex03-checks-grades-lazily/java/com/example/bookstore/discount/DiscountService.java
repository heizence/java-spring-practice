package com.example.bookstore.discount;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.example.bookstore.member.MemberGrade;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/** Mutant: notices a missing policy only when a member of that grade checks out, not at startup. */
@Service
public class DiscountService {

    private final Map<String, DiscountPolicy> policiesByBeanName;
    private final DiscountPolicy defaultPolicy;
    private final DiscountPolicy staffPolicy;

    public DiscountService(Map<String, DiscountPolicy> policiesByBeanName,
                           DiscountPolicy defaultPolicy,
                           @Qualifier("staffDiscount") DiscountPolicy staffPolicy) {
        this.policiesByBeanName = policiesByBeanName;
        this.defaultPolicy = defaultPolicy;
        this.staffPolicy = staffPolicy;
    }

    public long discountFor(MemberGrade grade, long amount) {
        Objects.requireNonNull(grade, "grade");
        String beanName = grade.name().toLowerCase(Locale.ROOT) + "Discount";
        DiscountPolicy policy = policiesByBeanName.get(beanName);
        if (policy == null) {
            throw new IllegalStateException("No DiscountPolicy bean named '%s' for grade %s".formatted(beanName, grade));
        }
        return policy.discountFor(checkAmount(amount));
    }

    public long guestDiscount(long amount) {
        return defaultPolicy.discountFor(checkAmount(amount));
    }

    public long staffDiscount(long amount) {
        return staffPolicy.discountFor(checkAmount(amount));
    }

    private static long checkAmount(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("The amount must not be negative, but was %d.".formatted(amount));
        }
        return amount;
    }
}
