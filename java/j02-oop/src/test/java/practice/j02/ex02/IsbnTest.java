package practice.j02.ex02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("J02-02 · equals, hashCode and toString")
class IsbnTest {

    private static final String PLAIN = "9780306406157";
    private static final String WITH_HYPHENS = "978-0-306-40615-7";
    private static final String WITH_SPACES = "978 0 306 40615 7";
    private static final String ANOTHER = "978-0-13-468599-1";
    private static final String CHECK_DIGIT_ZERO = "978-89-102-3757-0";

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("accepts a valid ISBN-13 and returns its 13 digits")
        void acceptsValidIsbn() {
            var isbn = new Isbn(PLAIN);

            assertThat(isbn.digits()).isEqualTo(PLAIN);
        }

        @ParameterizedTest(name = "\"{0}\"")
        @ValueSource(strings = {
                WITH_HYPHENS, WITH_SPACES, "978-0 306-40615 7", "  9780306406157  ", "978--0306406157-"})
        @DisplayName("removes hyphens and spaces")
        void removesHyphensAndSpaces(String text) {
            var isbn = new Isbn(text);

            assertThat(isbn.digits()).isEqualTo(PLAIN);
        }

        @Test
        @DisplayName("ISBNs written in different ways are equal")
        void differentSpellingsAreEqual() {
            var withHyphens = new Isbn(WITH_HYPHENS);
            var withSpaces = new Isbn(WITH_SPACES);

            assertThat(withHyphens).isEqualTo(withSpaces);
        }

        @Test
        @DisplayName("equal ISBNs have the same hash code")
        void equalIsbnsHaveEqualHashCodes() {
            var withHyphens = new Isbn(WITH_HYPHENS);
            var withSpaces = new Isbn(WITH_SPACES);

            assertThat(withHyphens.hashCode()).isEqualTo(withSpaces.hashCode());
        }

        @Test
        @DisplayName("different ISBNs are not equal")
        void differentIsbnsAreNotEqual() {
            var first = new Isbn(PLAIN);
            var second = new Isbn(ANOTHER);

            assertThat(first).isNotEqualTo(second);
        }

        @Test
        @DisplayName("toString() shows \"ISBN \" and the 13 digits, however the input was written")
        void toStringShowsDigits() {
            var isbn = new Isbn(WITH_HYPHENS);

            assertThat(isbn).hasToString("ISBN 9780306406157");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("accepts an ISBN whose check digit is 0")
        void checkDigitZero() {
            var isbn = new Isbn(CHECK_DIGIT_ZERO);

            assertThat(isbn.digits()).isEqualTo("9788910237570");
        }

        @Test
        @DisplayName("equals() is reflexive: an ISBN is equal to itself")
        void reflexive() {
            var isbn = new Isbn(PLAIN);

            assertThat(isbn.equals(isbn)).isTrue();
        }

        @Test
        @DisplayName("equals() is symmetric: a.equals(b) and b.equals(a) give the same answer")
        void symmetric() {
            var a = new Isbn(WITH_HYPHENS);
            var b = new Isbn(WITH_SPACES);
            var other = new Isbn(ANOTHER);

            assertThat(a.equals(b)).isTrue();
            assertThat(b.equals(a)).isTrue();
            assertThat(a.equals(other)).isFalse();
            assertThat(other.equals(a)).isFalse();
        }

        @Test
        @DisplayName("equals() is transitive: if a equals b and b equals c, then a equals c")
        void transitive() {
            var a = new Isbn(PLAIN);
            var b = new Isbn(WITH_HYPHENS);
            var c = new Isbn(WITH_SPACES);

            assertThat(a.equals(b)).isTrue();
            assertThat(b.equals(c)).isTrue();
            assertThat(a.equals(c)).isTrue();
        }

        @Test
        @DisplayName("equals() and hashCode() give the same answers every time")
        void consistent() {
            var a = new Isbn(PLAIN);
            var b = new Isbn(WITH_HYPHENS);

            assertThat(a.equals(b)).isTrue();
            assertThat(a.equals(b)).isTrue();
            assertThat(a.hashCode()).isEqualTo(a.hashCode());
        }

        @Test
        @DisplayName("equals(null) is false and doesn't throw")
        void notEqualToNull() {
            var isbn = new Isbn(PLAIN);

            assertThat(isbn.equals(null)).isFalse();
        }

        @Test
        @DisplayName("an ISBN is not equal to a String with the same digits")
        void notEqualToOtherType() {
            var isbn = new Isbn(PLAIN);

            assertThat(isbn.equals(PLAIN)).isFalse();
            assertThat(isbn.equals(new Object())).isFalse();
        }

        @Test
        @DisplayName("a HashSet keeps one element for two spellings of the same ISBN")
        void hashSetKeepsOne() {
            Set<Isbn> isbns = new HashSet<>();

            isbns.add(new Isbn(WITH_HYPHENS));
            isbns.add(new Isbn(WITH_SPACES));

            assertThat(isbns).hasSize(1);
        }

        @Test
        @DisplayName("a HashSet finds an ISBN through another spelling")
        void hashSetContains() {
            Set<Isbn> isbns = new HashSet<>();
            isbns.add(new Isbn(WITH_HYPHENS));

            assertThat(isbns.contains(new Isbn(PLAIN))).isTrue();
            assertThat(isbns.contains(new Isbn(ANOTHER))).isFalse();
        }

        @Test
        @DisplayName("a HashMap finds a value through another spelling of its key")
        void hashMapKey() {
            Map<Isbn, Integer> prices = new HashMap<>();
            prices.put(new Isbn(WITH_HYPHENS), 25_000);

            prices.put(new Isbn(WITH_SPACES), 23_000);

            assertThat(prices).hasSize(1);
            assertThat(prices.get(new Isbn(PLAIN))).isEqualTo(23_000);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("rejects null")
        void rejectsNull() {
            assertThatThrownBy(() -> new Isbn(null)).isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest(name = "\"{0}\"")
        @ValueSource(strings = {"", " ", "---", "978030640615", "97803064061570", "978-0-306-40615", "0-306-40615-2"})
        @DisplayName("rejects input that doesn't have exactly 13 digits")
        void rejectsWrongLength(String text) {
            assertThatThrownBy(() -> new Isbn(text)).isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest(name = "\"{0}\"")
        @ValueSource(strings = {
                "978030640615X", "978.0.306.40615.7", "978_0306406157", "978\t0306406157", "978/0306406157"})
        @DisplayName("rejects characters other than digits, hyphens and spaces")
        void rejectsOtherCharacters(String text) {
            assertThatThrownBy(() -> new Isbn(text)).isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest(name = "\"{0}\"")
        @ValueSource(strings = {"9780306406158", "9780306406150", "9780306406175"})
        @DisplayName("rejects a wrong check digit")
        void rejectsWrongCheckDigit(String text) {
            assertThatThrownBy(() -> new Isbn(text)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects digits from other scripts, such as fullwidth digits")
        void rejectsNonAsciiDigits() {
            // "９7８0３0６4０6157": five fullwidth digits. Wrong code that accepts them also gets a valid checksum.
            var text = "９7８0３0６4０6157";

            assertThatThrownBy(() -> new Isbn(text)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("puts the rejected input in the exception message")
        void messageContainsInput() {
            assertThatThrownBy(() -> new Isbn("978-0-306-40615-8"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("978-0-306-40615-8");
        }
    }

    @Nested
    @DisplayName("Randomized")
    class Randomized {

        @Test
        @DisplayName("1,000 random ISBNs equal themselves in random spellings, with the same hash code")
        void randomSpellingsAreEqual() {
            var random = new Random(42); // fixed seed: a failure is reproducible

            for (int step = 0; step < 1_000; step++) {
                String digits = randomValidIsbn(random);
                var first = new Isbn(randomSpelling(digits, random));
                var second = new Isbn(randomSpelling(digits, random));

                assertThat(first.digits()).as("digits at step %d", step).isEqualTo(digits);
                assertThat(first).as("equals at step %d", step).isEqualTo(second);
                assertThat(first.hashCode()).as("hashCode at step %d", step).isEqualTo(second.hashCode());
                assertThat(first).as("toString at step %d", step).hasToString("ISBN " + digits);
            }
        }

        @Test
        @DisplayName("rejects every single-digit typo in 1,000 random ISBNs")
        void detectsSingleDigitTypos() {
            var random = new Random(42);

            for (int step = 0; step < 1_000; step++) {
                char[] digits = randomValidIsbn(random).toCharArray();
                int position = random.nextInt(13);
                char original = digits[position];
                digits[position] = (char) ('0' + (original - '0' + 1 + random.nextInt(9)) % 10); // any other digit
                String typo = new String(digits);

                assertThatThrownBy(() -> new Isbn(typo))
                        .as("typo %s at step %d", typo, step)
                        .isInstanceOf(IllegalArgumentException.class);
            }
        }

        /** 12 random digits plus the check digit that makes the weighted sum a multiple of 10. */
        private static String randomValidIsbn(Random random) {
            var digits = new StringBuilder(random.nextBoolean() ? "978" : "979");
            while (digits.length() < 12) {
                digits.append(random.nextInt(10));
            }
            int sum = 0;
            for (int i = 0; i < 12; i++) {
                int digit = digits.charAt(i) - '0';
                sum += i % 2 == 0 ? digit : 3 * digit;
            }
            return digits.append((10 - sum % 10) % 10).toString();
        }

        /** Puts hyphens and spaces in random places. */
        private static String randomSpelling(String digits, Random random) {
            var text = new StringBuilder();
            for (char digit : digits.toCharArray()) {
                switch (random.nextInt(6)) {
                    case 0 -> text.append('-');
                    case 1 -> text.append(' ');
                    default -> { }
                }
                text.append(digit);
            }
            return text.toString();
        }
    }
}
