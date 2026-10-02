package practice.j02.ex05;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("J02-05 · Immutability, static factories and builders")
class TemperatureTest {

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("ofCelsius() creates a temperature in degrees Celsius")
        void ofCelsius() {
            var temperature = Temperature.ofCelsius(21.5);

            assertThat(temperature.celsius()).isEqualTo(21.5);
        }

        @Test
        @DisplayName("ofFahrenheit() converts to degrees Celsius")
        void ofFahrenheit() {
            assertThat(Temperature.ofFahrenheit(212).celsius()).isEqualTo(100.0);
            assertThat(Temperature.ofFahrenheit(32).celsius()).isEqualTo(0.0);
            assertThat(Temperature.ofFahrenheit(-40).celsius()).isEqualTo(-40.0);
        }

        @Test
        @DisplayName("fahrenheit() converts back to degrees Fahrenheit")
        void fahrenheit() {
            assertThat(Temperature.ofCelsius(100).fahrenheit()).isCloseTo(212.0, within(1e-9));
            assertThat(Temperature.ofCelsius(37).fahrenheit()).isCloseTo(98.6, within(1e-9));
        }

        @Test
        @DisplayName("plus() returns a warmer or a colder temperature")
        void plus() {
            var temperature = Temperature.ofCelsius(20);

            assertThat(temperature.plus(1.5)).isEqualTo(Temperature.ofCelsius(21.5));
            assertThat(temperature.plus(-25)).isEqualTo(Temperature.ofCelsius(-5));
        }

        @Test
        @DisplayName("plus() leaves the temperature it was called on unchanged")
        void plusDoesNotChangeThis() {
            var temperature = Temperature.ofCelsius(20);

            var warmer = temperature.plus(5);

            assertThat(temperature.celsius()).isEqualTo(20.0);
            assertThat(warmer).isNotSameAs(temperature);
        }

        @Test
        @DisplayName("temperatures with the same value are equal and have the same hash code")
        void equality() {
            var a = Temperature.ofCelsius(21.5);
            var b = Temperature.ofCelsius(21.5);

            assertThat(a).isEqualTo(b);
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
            assertThat(a).isNotEqualTo(Temperature.ofCelsius(21.51));
        }

        @Test
        @DisplayName("compareTo() sorts temperatures from colder to warmer")
        void sorting() {
            var hot = Temperature.ofCelsius(35);
            var cold = Temperature.ofCelsius(-10);
            var mild = Temperature.ofFahrenheit(68); // 20 °C
            List<Temperature> temperatures = new ArrayList<>(List.of(hot, cold, mild));

            Collections.sort(temperatures);

            assertThat(temperatures).containsExactly(cold, mild, hot);
        }

        @Test
        @DisplayName("isWarmerThan() compares two temperatures")
        void isWarmerThan() {
            var warm = Temperature.ofCelsius(25);
            var cool = Temperature.ofCelsius(15);

            assertThat(warm.isWarmerThan(cool)).isTrue();
            assertThat(cool.isWarmerThan(warm)).isFalse();
            assertThat(warm.isWarmerThan(Temperature.ofCelsius(25))).isFalse();
        }

        @Test
        @DisplayName("toString() shows degrees Celsius with 2 decimal places")
        void toStringFormat() {
            assertThat(Temperature.ofCelsius(21.5)).hasToString("21.50 °C");
            assertThat(Temperature.ofCelsius(-0.5)).hasToString("-0.50 °C");
            assertThat(Temperature.ofCelsius(-273.15)).hasToString("-273.15 °C");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("rounds to the nearest hundredth of a degree")
        void roundsToHundredths() {
            assertThat(Temperature.ofCelsius(21.456).celsius()).isEqualTo(21.46);
            assertThat(Temperature.ofCelsius(21.454).celsius()).isEqualTo(21.45);
            assertThat(Temperature.ofCelsius(21.456)).isEqualTo(Temperature.ofCelsius(21.46));
        }

        @Test
        @DisplayName("ofFahrenheit(39.2) equals ofCelsius(4), although the double math gives 4.000000000000002")
        void inexactConversionIsRounded() {
            assertThat(Temperature.ofFahrenheit(39.2)).isEqualTo(Temperature.ofCelsius(4));
            assertThat(Temperature.ofFahrenheit(33.8)).isEqualTo(Temperature.ofCelsius(1));
        }

        @Test
        @DisplayName("0.1 °C plus 0.2 °C equals 0.3 °C, although 0.1 + 0.2 is 0.30000000000000004 in double math")
        void plusIsRounded() {
            var sum = Temperature.ofCelsius(0.1).plus(0.2);

            assertThat(sum).isEqualTo(Temperature.ofCelsius(0.3));
            assertThat(sum.celsius()).isEqualTo(0.3);
        }

        @Test
        @DisplayName("compareTo() returns 0 exactly when equals() is true")
        void compareToConsistentWithEquals() {
            var fromFahrenheit = Temperature.ofFahrenheit(39.2);
            var fromCelsius = Temperature.ofCelsius(4);

            assertThat(fromFahrenheit.compareTo(fromCelsius)).isZero();
            assertThat(fromFahrenheit.compareTo(Temperature.ofCelsius(4.01))).isNegative();
        }

        @Test
        @DisplayName("accepts absolute zero, in Celsius and in Fahrenheit")
        void absoluteZero() {
            assertThat(Temperature.ofCelsius(-273.15).celsius()).isEqualTo(-273.15);
            assertThat(Temperature.ofFahrenheit(-459.67)).isEqualTo(Temperature.ofCelsius(-273.15));
        }

        @Test
        @DisplayName("accepts a value that rounds to absolute zero, because the limit applies after rounding")
        void roundsBeforeCheckingTheLimit() {
            var temperature = Temperature.ofCelsius(-273.154);

            assertThat(temperature).isEqualTo(Temperature.ofCelsius(-273.15));
        }

        @Test
        @DisplayName("accepts the maximum temperature exactly")
        void maximum() {
            var temperature = Temperature.ofCelsius(Temperature.MAX_CELSIUS);

            assertThat(temperature.celsius()).isEqualTo(1_000_000.0);
        }

        @Test
        @DisplayName("0 °C and -0 °C are the same temperature")
        void negativeZero() {
            var zero = Temperature.ofCelsius(0.0);
            var negativeZero = Temperature.ofCelsius(-0.0);

            assertThat(negativeZero).isEqualTo(zero);
            assertThat(negativeZero.hashCode()).isEqualTo(zero.hashCode());
            assertThat(negativeZero).hasToString("0.00 °C");
        }

        @Test
        @DisplayName("is not equal to null or to an object of another type")
        void notEqualToOtherThings() {
            var temperature = Temperature.ofCelsius(21.5);

            assertThat(temperature.equals(null)).isFalse();
            assertThat(temperature.equals("21.50 °C")).isFalse();
            assertThat(temperature.equals(21.5)).isFalse();
        }

        @Test
        @DisplayName("toString() uses a dot, even when the default locale uses a comma")
        void toStringIgnoresDefaultLocale() {
            Locale original = Locale.getDefault();
            try {
                Locale.setDefault(Locale.GERMANY);

                String text = Temperature.ofCelsius(21.5).toString();

                assertThat(text).isEqualTo("21.50 °C");
            } finally {
                Locale.setDefault(original);
            }
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @ParameterizedTest(name = "{0}")
        @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
        @DisplayName("rejects NaN and infinity")
        void notFinite(double value) {
            assertThatThrownBy(() -> Temperature.ofCelsius(value)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> Temperature.ofFahrenheit(value)).isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest(name = "{0} °C")
        @ValueSource(doubles = {-273.16, -300, -1e300, -Double.MAX_VALUE})
        @DisplayName("rejects temperatures below absolute zero")
        void belowAbsoluteZero(double celsius) {
            assertThatThrownBy(() -> Temperature.ofCelsius(celsius)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("puts the rejected value in the exception message")
        void messageContainsValue() {
            assertThatThrownBy(() -> Temperature.ofCelsius(-273.16)).hasMessageContaining("-273.16");
            assertThatThrownBy(() -> Temperature.ofCelsius(2_000_000)).hasMessageContaining("2000000");
        }

        @Test
        @DisplayName("rejects Fahrenheit temperatures below absolute zero")
        void belowAbsoluteZeroInFahrenheit() {
            assertThatThrownBy(() -> Temperature.ofFahrenheit(-460)).isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest(name = "{0} °C")
        @ValueSource(doubles = {1_000_000.01, 2_000_000, 1e300, Double.MAX_VALUE})
        @DisplayName("rejects temperatures above the maximum")
        void aboveMaximum(double celsius) {
            assertThatThrownBy(() -> Temperature.ofCelsius(celsius)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("plus() rejects a result outside the limits and leaves the temperature unchanged")
        void plusOutOfRange() {
            var temperature = Temperature.ofCelsius(-273);

            assertThatThrownBy(() -> temperature.plus(-1)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> temperature.plus(Double.MAX_VALUE)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> temperature.plus(Double.NaN)).isInstanceOf(IllegalArgumentException.class);
            assertThat(temperature.celsius()).isEqualTo(-273.0);
        }

        @Test
        @DisplayName("isWarmerThan(null) and compareTo(null) throw NullPointerException")
        void nullComparisons() {
            var temperature = Temperature.ofCelsius(21.5);

            assertThatThrownBy(() -> temperature.isWarmerThan(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> temperature.compareTo(null)).isInstanceOf(NullPointerException.class);
        }
    }
}
