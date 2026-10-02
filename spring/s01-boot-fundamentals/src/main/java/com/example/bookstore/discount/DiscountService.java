package com.example.bookstore.discount;

import com.example.bookstore.member.MemberGrade;

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
public class DiscountService {

    /** The discount in won for a member of this grade, on an order of {@code amount} won. */
    public long discountFor(MemberGrade grade, long amount) {
        // TODO: S01-03
        throw new UnsupportedOperationException("TODO: S01-03");
    }

    /** The discount in won for a customer without an account: the default policy's. */
    public long guestDiscount(long amount) {
        // TODO: S01-03
        throw new UnsupportedOperationException("TODO: S01-03");
    }

    /** The discount in won for one of the bookstore's employees: always the staff policy's. */
    public long staffDiscount(long amount) {
        // TODO: S01-03
        throw new UnsupportedOperationException("TODO: S01-03");
    }
}
