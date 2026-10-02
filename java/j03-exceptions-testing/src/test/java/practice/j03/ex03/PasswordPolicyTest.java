package practice.j03.ex03;

import org.junit.jupiter.api.DisplayName;

/**
 * J03-03 · Write tests: PasswordPolicy.
 *
 * <p>Write tests that pin down every rule in the Javadoc of {@link PasswordPolicy}. Your tests are run
 * against several broken versions of {@code PasswordPolicy}; a broken version is <em>caught</em> when at
 * least one of your tests fails on it. Run {@code ./score j03-03} to see how many you catch.
 *
 * <p>Structure your tests like the ones in the other exercises: {@code @Nested} classes for
 * "Basic", "Edge cases" and "Errors", a {@code @DisplayName} on every test, given/when/then inside.
 * Don't open {@code src/mutants} until you've caught them all: it's the answer key.
 */
@DisplayName("J03-03 · Write tests: PasswordPolicy")
class PasswordPolicyTest {

    private final PasswordPolicy policy = new PasswordPolicy();

    // TODO: J03-03 — write your tests here.
}
