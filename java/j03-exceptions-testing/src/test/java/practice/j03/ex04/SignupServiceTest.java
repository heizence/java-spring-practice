package practice.j03.ex04;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * J03-04 · Write tests: mocking with Mockito.
 *
 * <p>Write tests that pin down every rule in the Javadoc of {@link SignupService}. Your tests are run against
 * several broken versions of {@code SignupService}; a broken version is <em>caught</em> when at least one of
 * your tests fails on it. Run {@code ./score j03-04} to see how many you catch.
 *
 * <p>The mocks are ready: before every test, {@code MockitoExtension} creates a fresh mock for each {@code @Mock}
 * field, and {@code @InjectMocks} passes them to the {@code SignupService} constructor. Structure your tests like
 * the ones in the other exercises: {@code @Nested} classes for "Basic", "Edge cases" and "Errors", a
 * {@code @DisplayName} on every test, given/when/then inside. Don't open {@code src/mutants} until you've caught
 * them all: it's the answer key.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("J03-04 · Write tests: mocking with Mockito")
class SignupServiceTest {

    @Mock
    UserRepository users;

    @Mock
    PasswordHasher passwordHasher;

    @Mock
    WelcomeMailer welcomeMailer;

    @InjectMocks
    SignupService service;

    // TODO: J03-04 — write your tests here.
}
