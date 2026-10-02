package practice.j03.ex03;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Checks a password against the sign-up rules and reports <em>every</em> rule it breaks.
 *
 * <p><strong>This class is finished and correct.</strong> In exercise J03-03 you write the tests for it.
 *
 * <h2>Rules</h2>
 * <ol>
 *   <li>The length is between {@value #MIN_LENGTH} and {@value #MAX_LENGTH} characters, inclusive.</li>
 *   <li>It contains at least one uppercase letter, one lowercase letter and one digit.</li>
 *   <li>It contains no whitespace: no spaces, tabs or line breaks.</li>
 *   <li>It does not contain the username, ignoring letter case. Usernames shorter than 3 characters are
 *       not checked (too many passwords would contain them by accident).</li>
 * </ol>
 *
 * <p>{@link #check} returns every violation in the order of the {@link Violation} enum, or an empty list for
 * a valid password. A {@code null} username or password throws {@link NullPointerException}.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 64;
    private static final int MIN_CHECKED_USERNAME_LENGTH = 3;

    /**
     * Returns every rule that {@code password} breaks, in the order of the {@link Violation} enum, or an empty list
     * if it breaks none. The list can't be modified.
     */
    public List<Violation> check(String username, String password) {
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(password, "password");

        List<Violation> violations = new ArrayList<>();
        if (password.length() < MIN_LENGTH) {
            violations.add(Violation.TOO_SHORT);
        }
        if (password.length() > MAX_LENGTH) {
            violations.add(Violation.TOO_LONG);
        }
        if (password.chars().noneMatch(Character::isUpperCase)) {
            violations.add(Violation.MISSING_UPPERCASE);
        }
        if (password.chars().noneMatch(Character::isLowerCase)) {
            violations.add(Violation.MISSING_LOWERCASE);
        }
        if (password.chars().noneMatch(Character::isDigit)) {
            violations.add(Violation.MISSING_DIGIT);
        }
        if (password.chars().anyMatch(Character::isWhitespace)) {
            violations.add(Violation.CONTAINS_WHITESPACE);
        }
        if (username.length() >= MIN_CHECKED_USERNAME_LENGTH
                && password.contains(username)) {
            violations.add(Violation.CONTAINS_USERNAME);
        }
        return List.copyOf(violations);
    }

    /** Returns {@code true} if {@code password} breaks no rule: exactly when {@link #check} returns an empty list. */
    public boolean isValid(String username, String password) {
        return check(username, password).isEmpty();
    }
}
