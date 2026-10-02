package practice.j03.ex04;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Reference tests for J03-04.
 *
 * <p>Three habits make these tests strong:
 * <ul>
 *   <li>Stub only what a test needs. {@code MockitoExtension} runs in "strict stubs" mode: a stubbing that the
 *       code never uses fails the test. That keeps every test short and honest.</li>
 *   <li>Check <em>what</em> reached a collaborator, not only <em>that</em> it was called: an
 *       {@link ArgumentCaptor} catches the raw password, and {@link InOrder} catches a mail that goes out too
 *       early.</li>
 *   <li>Check what must <em>not</em> happen: {@code verifyNoInteractions} and {@code never()} catch work that
 *       sneaks into the error paths.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("J03-04 · Write tests: mocking with Mockito")
class SignupServiceTest {

    private static final String EMAIL = "minji@example.com";
    private static final String PASSWORD = "s3cret-Pa55";
    private static final String HASH = "$2a$10$hash-of-the-password";

    @Mock
    UserRepository users;

    @Mock
    PasswordHasher passwordHasher;

    @Mock
    WelcomeMailer welcomeMailer;

    @InjectMocks
    SignupService service;

    @Captor
    ArgumentCaptor<User> savedUser;

    /** The happy path: the email is free, hashing works, and the database hands out id 42. */
    private void givenNewUserCanBeSaved() {
        when(users.existsByEmail(EMAIL)).thenReturn(false);
        when(passwordHasher.hash(PASSWORD)).thenReturn(HASH);
        when(users.save(any(User.class))).thenReturn(42L);
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("saves the user with the password hash, never the raw password")
        void savesHashNotRawPassword() {
            givenNewUserCanBeSaved();

            service.signUp(EMAIL, PASSWORD);

            verify(users).save(savedUser.capture());
            assertThat(savedUser.getValue()).isEqualTo(new User(EMAIL, HASH));
        }

        @Test
        @DisplayName("sends the welcome mail after the user was saved")
        void mailsAfterSaving() {
            givenNewUserCanBeSaved();

            service.signUp(EMAIL, PASSWORD);

            InOrder inOrder = inOrder(users, welcomeMailer);
            inOrder.verify(users).save(any(User.class));
            inOrder.verify(welcomeMailer).sendWelcome(EMAIL);
        }

        @Test
        @DisplayName("returns the new id, the email and mailSent = true")
        void returnsResult() {
            givenNewUserCanBeSaved();

            SignupResult result = service.signUp(EMAIL, PASSWORD);

            assertThat(result).isEqualTo(new SignupResult(42L, EMAIL, true));
        }

        @Test
        @DisplayName("rejects an email that is already taken")
        void rejectsTakenEmail() {
            when(users.existsByEmail(EMAIL)).thenReturn(true);

            DuplicateEmailException thrown =
                    catchThrowableOfType(DuplicateEmailException.class, () -> service.signUp(EMAIL, PASSWORD));

            assertThat(thrown).extracting(DuplicateEmailException::email).isEqualTo(EMAIL);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("normalizes the email before checking for duplicates")
        void normalizesBeforeDuplicateCheck() {
            when(users.existsByEmail(EMAIL)).thenReturn(true);

            assertThatThrownBy(() -> service.signUp("  Minji@Example.COM \t", PASSWORD))
                    .isInstanceOf(DuplicateEmailException.class);
        }

        @Test
        @DisplayName("saves, mails and returns the normalized email")
        void usesNormalizedEmailEverywhere() {
            givenNewUserCanBeSaved();

            SignupResult result = service.signUp("  Minji@Example.COM \t", PASSWORD);

            verify(users).save(savedUser.capture());
            assertThat(savedUser.getValue().email()).isEqualTo(EMAIL);
            verify(welcomeMailer).sendWelcome(EMAIL);
            assertThat(result.email()).isEqualTo(EMAIL);
        }

        @Test
        @DisplayName("still succeeds, with mailSent = false, when the welcome mail fails")
        void mailFailureDoesNotFailSignup() {
            givenNewUserCanBeSaved();
            doThrow(new MailException("mail server is down")).when(welcomeMailer).sendWelcome(EMAIL);

            SignupResult result = service.signUp(EMAIL, PASSWORD);

            assertThat(result).isEqualTo(new SignupResult(42L, EMAIL, false));
        }

        @Test
        @DisplayName("lets any other exception from the mailer propagate")
        void otherMailerFailuresPropagate() {
            givenNewUserCanBeSaved();
            var bug = new IllegalStateException("mail template is missing");
            doThrow(bug).when(welcomeMailer).sendWelcome(EMAIL);

            assertThatThrownBy(() -> service.signUp(EMAIL, PASSWORD)).isSameAs(bug);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("does nothing else for a taken email: no hashing, no saving, no mail")
        void takenEmailDoesNothingElse() {
            when(users.existsByEmail(EMAIL)).thenReturn(true);

            assertThatThrownBy(() -> service.signUp(EMAIL, PASSWORD)).isInstanceOf(DuplicateEmailException.class);

            verify(users, never()).save(any());
            verifyNoInteractions(passwordHasher, welcomeMailer);
        }

        @Test
        @DisplayName("sends no mail when saving fails")
        void noMailWhenSaveFails() {
            when(passwordHasher.hash(PASSWORD)).thenReturn(HASH);
            var databaseDown = new IllegalStateException("database is down");
            when(users.save(any(User.class))).thenThrow(databaseDown);

            assertThatThrownBy(() -> service.signUp(EMAIL, PASSWORD)).isSameAs(databaseDown);

            verifyNoInteractions(welcomeMailer);
        }

        @Test
        @DisplayName("rejects a null email or password before calling any collaborator")
        void rejectsNulls() {
            assertThatThrownBy(() -> service.signUp(null, PASSWORD)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> service.signUp(EMAIL, null)).isInstanceOf(NullPointerException.class);

            verifyNoInteractions(users, passwordHasher, welcomeMailer);
        }
    }
}
