package com.example.bookstore.discount;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.example.bookstore.member.MemberGrade;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Decides which {@link DiscountPolicy} applies to a purchase. This is the strategy pattern, with Spring collecting
 * the strategies: adding a policy bean needs no change here.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>It's a bean ({@code @Service}). Its constructor receives:
 *     <ul>
 *       <li>every {@code DiscountPolicy} bean, as a {@code Map<String, DiscountPolicy>} from bean name to bean;</li>
 *       <li>the default policy, through a {@code DiscountPolicy} parameter without a qualifier. For that to work,
 *           mark {@link BasicDiscount} with {@code @Primary};</li>
 *       <li>the staff policy, through a {@code DiscountPolicy} parameter with {@code @Qualifier("staffDiscount")}.</li>
 *     </ul>
 *   </li>
 *   <li>{@link #discountFor(MemberGrade, long)} uses the policy whose bean name is the grade's name in lower case
 *       followed by {@code Discount}: {@code basicDiscount}, {@code silverDiscount} or {@code goldDiscount}.</li>
 *   <li>{@link #guestDiscount(long)} uses the default policy, and {@link #staffDiscount(long)} the staff policy.</li>
 *   <li><b>Fail at startup, not at checkout.</b> When a grade has no policy bean, the constructor throws
 *       {@link IllegalStateException} with a message that names the grade and the missing bean name, such as
 *       {@code No DiscountPolicy bean named 'goldDiscount' for grade GOLD}. The application then doesn't start.</li>
 *   <li>A negative amount throws {@link IllegalArgumentException}; a {@code null} grade throws
 *       {@link NullPointerException}.</li>
 * </ul>
 */
@Service
public class DiscountService {

    private final Map<MemberGrade, DiscountPolicy> policiesByGrade = new EnumMap<>(MemberGrade.class);
    private final DiscountPolicy defaultPolicy;
    private final DiscountPolicy staffPolicy;

    // A hand-written constructor instead of @RequiredArgsConstructor, for two reasons. It checks the policies while
    // Spring creates the bean, so a missing one stops the application at startup. And Lombok wouldn't copy a
    // @Qualifier from a field to the constructor parameter it generates: the qualifier would be lost, and Spring
    // would inject the @Primary policy instead of the staff policy.
    public DiscountService(Map<String, DiscountPolicy> policiesByBeanName,
                           DiscountPolicy defaultPolicy,
                           @Qualifier("staffDiscount") DiscountPolicy staffPolicy) {
        for (MemberGrade grade : MemberGrade.values()) {
            // Locale.ROOT: on a machine set to Turkish, "SILVER".toLowerCase() gives "sılver", with a dotless ı.
            String beanName = grade.name().toLowerCase(Locale.ROOT) + "Discount";
            DiscountPolicy policy = policiesByBeanName.get(beanName);
            if (policy == null) {
                throw new IllegalStateException(
                        "No DiscountPolicy bean named '%s' for grade %s".formatted(beanName, grade));
            }
            policiesByGrade.put(grade, policy);
        }
        this.defaultPolicy = defaultPolicy;
        this.staffPolicy = staffPolicy;
    }

    /** The discount in won for a member of this grade, on an order of {@code amount} won. */
    public long discountFor(MemberGrade grade, long amount) {
        Objects.requireNonNull(grade, "grade");
        return policiesByGrade.get(grade).discountFor(checkAmount(amount));
    }

    /** The discount in won for a customer without an account: the default policy's. */
    public long guestDiscount(long amount) {
        return defaultPolicy.discountFor(checkAmount(amount));
    }

    /** The discount in won for one of the bookstore's employees: always the staff policy's. */
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
