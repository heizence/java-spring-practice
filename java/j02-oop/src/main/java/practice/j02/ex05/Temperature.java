package practice.j02.ex05;

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

    // Private, so that the static factories are the only way to create a Temperature.
    // Give it the parameters and the body you need.
    private Temperature() {
        // TODO: J02-05
        throw new UnsupportedOperationException("TODO: J02-05");
    }

    /** A temperature of {@code celsius} degrees Celsius. */
    public static Temperature ofCelsius(double celsius) {
        // TODO: J02-05
        throw new UnsupportedOperationException("TODO: J02-05");
    }

    /** A temperature of {@code fahrenheit} degrees Fahrenheit. */
    public static Temperature ofFahrenheit(double fahrenheit) {
        // TODO: J02-05
        throw new UnsupportedOperationException("TODO: J02-05");
    }

    /** This temperature in degrees Celsius. */
    public double celsius() {
        // TODO: J02-05
        throw new UnsupportedOperationException("TODO: J02-05");
    }

    /** This temperature in degrees Fahrenheit. */
    public double fahrenheit() {
        // TODO: J02-05
        throw new UnsupportedOperationException("TODO: J02-05");
    }

    /** A new temperature that is {@code celsiusDelta} degrees Celsius warmer (or colder, if negative). */
    public Temperature plus(double celsiusDelta) {
        // TODO: J02-05
        throw new UnsupportedOperationException("TODO: J02-05");
    }

    /** {@code true} if this temperature is warmer than {@code other}. */
    public boolean isWarmerThan(Temperature other) {
        // TODO: J02-05
        throw new UnsupportedOperationException("TODO: J02-05");
    }

    @Override
    public int compareTo(Temperature other) {
        // TODO: J02-05
        throw new UnsupportedOperationException("TODO: J02-05");
    }

    @Override
    public boolean equals(Object other) {
        // TODO: J02-05
        throw new UnsupportedOperationException("TODO: J02-05");
    }

    @Override
    public int hashCode() {
        // TODO: J02-05
        throw new UnsupportedOperationException("TODO: J02-05");
    }

    @Override
    public String toString() {
        // TODO: J02-05
        throw new UnsupportedOperationException("TODO: J02-05");
    }
}
