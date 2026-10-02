package practice.j02.ex05;

import java.util.Locale;

/**
 * An immutable temperature.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>Create temperatures with {@link #ofCelsius} or {@link #ofFahrenheit}; there is no public constructor.
 *       Fahrenheit converts to Celsius as {@code (fahrenheit - 32) * 5 / 9}.</li>
 *   <li>Every temperature is rounded to the nearest hundredth of a degree Celsius (0.01 °C) with
 *       {@link Math#round(double)}, so {@code ofCelsius(21.456)} is 21.46 °C. That makes equality reliable even
 *       though {@code double} math isn't exact: {@code ofFahrenheit(39.2)} equals {@code ofCelsius(4)}, although
 *       {@code (39.2 - 32) * 5 / 9} is 4.000000000000002 in {@code double}.</li>
 *   <li>After rounding, a temperature must be between {@value #ABSOLUTE_ZERO_CELSIUS} °C (absolute zero) and
 *       {@value #MAX_CELSIUS} °C, inclusive. NaN and infinity aren't allowed either. Every factory and
 *       {@link #plus} throws {@link IllegalArgumentException} with the value in the message.</li>
 *   <li>{@link #plus} returns a new temperature; the temperature you call it on never changes.</li>
 *   <li>{@link #equals} and {@link #hashCode} compare the rounded value. {@link #compareTo} orders from colder to
 *       warmer and returns 0 exactly when {@code equals} is true. {@link #isWarmerThan} and {@link #compareTo}
 *       throw {@link NullPointerException} for {@code null}.</li>
 *   <li>{@link #toString()} shows degrees Celsius with 2 decimal places and a dot, such as {@code "21.50 °C"} or
 *       {@code "-0.50 °C"}.</li>
 * </ul>
 */
public final class Temperature implements Comparable<Temperature> {

    /** The lowest possible temperature: absolute zero. */
    public static final double ABSOLUTE_ZERO_CELSIUS = -273.15;

    /** The highest temperature this class accepts, far above anything an application measures. */
    public static final double MAX_CELSIUS = 1_000_000;

    // Whole hundredths of a degree: 2150 means 21.50 °C. A long compares exactly, unlike a double, so equals,
    // hashCode and compareTo can't disagree. The limits are hundredths too.
    private static final long MIN_HUNDREDTHS = -27_315;
    private static final long MAX_HUNDREDTHS = 100_000_000;

    private final long hundredths;

    // Private: the static factories are the only way in. Their names say which unit the number is in, which two
    // constructors couldn't do: Temperature(double) can exist only once.
    private Temperature(long hundredths) {
        this.hundredths = hundredths;
    }

    /** A temperature of {@code celsius} degrees Celsius. */
    public static Temperature ofCelsius(double celsius) {
        // Check NaN first: Math.round(NaN) is 0, so NaN would silently become 0 °C.
        if (!Double.isFinite(celsius)) {
            throw new IllegalArgumentException("temperature must be a finite number, but was " + celsius);
        }
        if (celsius < ABSOLUTE_ZERO_CELSIUS || celsius > MAX_CELSIUS) {
            throw new IllegalArgumentException("temperature must be between " + ABSOLUTE_ZERO_CELSIUS + " and "
                    + MAX_CELSIUS + " °C, but was " + celsius + " °C");
        }
        long rounded = Math.round(celsius * 100);
        return new Temperature(rounded);
    }

    /** A temperature of {@code fahrenheit} degrees Fahrenheit. */
    public static Temperature ofFahrenheit(double fahrenheit) {
        return ofCelsius((fahrenheit - 32) * 5 / 9);
    }

    /** This temperature in degrees Celsius. */
    public double celsius() {
        return hundredths / 100.0;
    }

    /** This temperature in degrees Fahrenheit. */
    public double fahrenheit() {
        return celsius() * 9 / 5 + 32;
    }

    /** A new temperature that is {@code celsiusDelta} degrees Celsius warmer (or colder, if negative). */
    public Temperature plus(double celsiusDelta) {
        // An immutable class "changes" by returning a new object. Reusing ofCelsius applies the same rounding and
        // limits; a huge celsiusDelta can't overflow, because the sum is a double and ofCelsius checks it.
        return ofCelsius(celsius() + celsiusDelta);
    }

    /** {@code true} if this temperature is warmer than {@code other}. */
    public boolean isWarmerThan(Temperature other) {
        return compareTo(other) > 0;
    }

    @Override
    public int compareTo(Temperature other) {
        return Long.compare(hundredths, other.hundredths);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Temperature that && hundredths == that.hundredths;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(hundredths);
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%.2f °C", celsius());
    }
}
