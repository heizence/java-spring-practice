package practice.j03.ex03;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Reference tests for J03-03.
 *
 * <p>Two habits make these tests strong:
 * <ul>
 *   <li>Start from a password that is valid, then break exactly one rule per test. If a test fails, you know
 *       which rule is wrong.</li>
 *   <li>Test the boundaries on both sides (7/8 and 64/65 characters), because off-by-one mistakes hide
 *       exactly there.</li>
 * </ul>
 */
@DisplayName("J03-03 · Write tests: PasswordPolicy")
class PasswordPolicyTest {

    private static final String USERNAME = "minji";
    private static final String VALID = "Secret123"; // 9 characters: upper, lower, digit, no username

    private final PasswordPolicy policy = new PasswordPolicy();

    /** A valid password of exactly {@code length} characters. */
    private static String validPasswordOfLength(int length) {
        return "Aa1" + "x".repeat(length - 3);
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("accepts a password that follows every rule")
        void acceptsValidPassword() {
            assertThat(policy.check(USERNAME, VALID)).isEmpty();
            assertThat(policy.isValid(USERNAME, VALID)).isTrue();
        }

        @Test
        @DisplayName("reports a password without an uppercase letter")
        void missingUppercase() {
            assertThat(policy.check(USERNAME, "secret123")).containsExactly(Violation.MISSING_UPPERCASE);
        }

        @Test
        @DisplayName("reports a password without a lowercase letter")
        void missingLowercase() {
            assertThat(policy.check(USERNAME, "SECRET123")).containsExactly(Violation.MISSING_LOWERCASE);
        }

        @Test
        @DisplayName("reports a password without a digit")
        void missingDigit() {
            assertThat(policy.check(USERNAME, "SecretWord")).containsExactly(Violation.MISSING_DIGIT);
        }

        @Test
        @DisplayName("isValid() is false when any rule is broken")
        void isValidFalseWhenBroken() {
            assertThat(policy.isValid(USERNAME, "secret123")).isFalse();
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("7 characters is too short")
        void sevenCharactersTooShort() {
            assertThat(policy.check(USERNAME, validPasswordOfLength(7))).containsExactly(Violation.TOO_SHORT);
        }

        @Test
        @DisplayName("exactly 8 characters is long enough")
        void eightCharactersAccepted() {
            assertThat(policy.check(USERNAME, validPasswordOfLength(8))).isEmpty();
        }

        @Test
        @DisplayName("exactly 64 characters is still accepted")
        void sixtyFourCharactersAccepted() {
            assertThat(policy.check(USERNAME, validPasswordOfLength(64))).isEmpty();
        }

        @Test
        @DisplayName("65 characters is too long")
        void sixtyFiveCharactersTooLong() {
            assertThat(policy.check(USERNAME, validPasswordOfLength(65))).containsExactly(Violation.TOO_LONG);
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"Secret 123", "Secret\t123", "Secret\n123"})
        @DisplayName("reports any kind of whitespace")
        void anyWhitespace(String password) {
            assertThat(policy.check(USERNAME, password)).containsExactly(Violation.CONTAINS_WHITESPACE);
        }

        @Test
        @DisplayName("reports the username even in different letter case")
        void usernameIgnoringCase() {
            assertThat(policy.check(USERNAME, "xxMINJI99a")).containsExactly(Violation.CONTAINS_USERNAME);
        }

        @Test
        @DisplayName("ignores usernames shorter than 3 characters")
        void shortUsernameNotChecked() {
            assertThat(policy.check("se", VALID)).isEmpty();
        }

        @Test
        @DisplayName("a 3-character username is checked")
        void threeCharacterUsernameChecked() {
            assertThat(policy.check("cre", VALID)).containsExactly(Violation.CONTAINS_USERNAME);
        }

        @Test
        @DisplayName("reports every broken rule, in the order of the Violation enum")
        void reportsAllViolationsInOrder() {
            assertThat(policy.check("abc", "abc"))
                    .containsExactly(Violation.TOO_SHORT, Violation.MISSING_UPPERCASE, Violation.MISSING_DIGIT,
                            Violation.CONTAINS_USERNAME);
        }

        @Test
        @DisplayName("an empty password breaks every rule it can")
        void emptyPassword() {
            assertThat(policy.check(USERNAME, ""))
                    .containsExactly(Violation.TOO_SHORT, Violation.MISSING_UPPERCASE, Violation.MISSING_LOWERCASE,
                            Violation.MISSING_DIGIT);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("rejects a null password")
        void nullPassword() {
            assertThatThrownBy(() -> policy.check(USERNAME, null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("rejects a null username")
        void nullUsername() {
            assertThatThrownBy(() -> policy.check(null, VALID)).isInstanceOf(NullPointerException.class);
        }
    }
}
