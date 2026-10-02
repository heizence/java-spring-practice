package com.example.bookstore.discount;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.example.bookstore.member.MemberGrade;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Mutant: @Qualifier on a field with Lombok's @RequiredArgsConstructor. Lombok doesn't copy the qualifier to the
 * constructor parameter, so Spring injects the @Primary policy as the staff policy.
 */
@Service
@RequiredArgsConstructor
public class DiscountService {

    private final Map<String, DiscountPolicy> policiesByBeanName;
    private final DiscountPolicy defaultPolicy;

    @Qualifier("staffDiscount")
    private final DiscountPolicy staffPolicy;

    @PostConstruct
    void checkEveryGradeHasAPolicy() {
        for (MemberGrade grade : MemberGrade.values()) {
            if (!policiesByBeanName.containsKey(beanNameOf(grade))) {
                throw new IllegalStateException(
                        "No DiscountPolicy bean named '%s' for grade %s".formatted(beanNameOf(grade), grade));
            }
        }
    }

    public long discountFor(MemberGrade grade, long amount) {
        Objects.requireNonNull(grade, "grade");
        return policiesByBeanName.get(beanNameOf(grade)).discountFor(checkAmount(amount));
    }

    public long guestDiscount(long amount) {
        return defaultPolicy.discountFor(checkAmount(amount));
    }

    public long staffDiscount(long amount) {
        return staffPolicy.discountFor(checkAmount(amount));
    }

    private static String beanNameOf(MemberGrade grade) {
        return grade.name().toLowerCase(Locale.ROOT) + "Discount";
    }

    private static long checkAmount(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("The amount must not be negative, but was %d.".formatted(amount));
        }
        return amount;
    }
}
