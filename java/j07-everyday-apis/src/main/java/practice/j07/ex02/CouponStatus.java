package practice.j07.ex02;

/** The result of {@link CouponValidator#check}: whether a coupon can be used now, and if not, why. */
public enum CouponStatus {
    /** The coupon can be used. */
    VALID,
    /** Today is before the coupon's first day. */
    NOT_YET_VALID,
    /** Today is after the coupon's last day. */
    EXPIRED,
    /** The coupon works only on weekends, and today is a weekday. */
    WEEKENDS_ONLY
}
