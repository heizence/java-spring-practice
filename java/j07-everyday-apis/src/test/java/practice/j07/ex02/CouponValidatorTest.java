package practice.j07.ex02;

import org.junit.jupiter.api.DisplayName;

/**
 * J07-02 · Write tests: time-dependent code.
 *
 * <p>Write tests that pin down every rule in the Javadoc of {@link CouponValidator}. Your tests are run against
 * several broken versions of {@code CouponValidator}; a broken version is <em>caught</em> when at least one of
 * your tests fails on it. Run {@code ./score j07-02} to see how many you catch.
 *
 * <p>Give every validator a stopped clock, so each test sees the same "now" on every day it runs:
 * {@code new CouponValidator(Clock.fixed(Instant.parse("2025-03-15T03:00:00Z"), ZoneOffset.UTC))}.
 * Structure your tests like the ones in the other exercises: {@code @Nested} classes for "Basic", "Edge cases"
 * and "Errors", a {@code @DisplayName} on every test, given/when/then inside. Don't open {@code src/mutants} until
 * you've caught them all: it's the answer key.
 */
@DisplayName("J07-02 · Write tests: time-dependent code")
class CouponValidatorTest {

    // TODO: J07-02 — write your tests here.
}
