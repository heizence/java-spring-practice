package com.example.bookstore.wallet;

import com.example.bookstore.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * S12-06 · Write tests: security rules.
 *
 * <p>Write tests that pin down the security rules of the wallet API, as the Javadoc of {@link WalletService} lists
 * them. Your tests run against several broken versions of the wallet code; a broken version is <em>caught</em> when
 * at least one of your tests fails on it. Run {@code ./score s12-06} to see how many you catch.
 *
 * <p>Structure your tests like the ones in the other exercises: {@code @Nested} classes for "Basic", "Edge cases"
 * and "Errors", a {@code @DisplayName} on every test, given/when/then inside. Sign requests in with
 * {@link com.example.bookstore.support.TestUsers}: {@code mvc.get().uri("/api/wallet").with(TestUsers.minji())}.
 * Don't open {@code src/mutants} until you've caught them all: it's the answer key.
 */
@IntegrationTest
@DisplayName("S12-06 · Write tests: security rules")
class WalletSecurityTest {

    @Autowired
    private MockMvcTester mvc;

    // TODO: S12-06 — write your tests here.
}
