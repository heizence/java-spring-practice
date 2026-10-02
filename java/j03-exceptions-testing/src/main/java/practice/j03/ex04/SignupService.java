package practice.j03.ex04;

import java.lang.System.Logger.Level;
import java.util.Locale;
import java.util.Objects;

/**
 * Signs up new users.
 *
 * <p><strong>This class is finished and correct.</strong> In exercise J03-04 you write the tests for it, with a
 * Mockito mock for each of its three collaborators.
 *
 * <h2>Rules</h2>
 * <ol>
 *   <li>The email is normalized first: surrounding whitespace is removed ({@link String#strip()}) and it is
 *       converted to lower case ({@link Locale#ROOT}). Every later step uses the normalized email.</li>
 *   <li>If {@link UserRepository#existsByEmail} says the normalized email is taken, {@code signUp} throws
 *       {@link DuplicateEmailException} and does nothing else: no hashing, no saving, no mail.</li>
 *   <li>Otherwise it hashes the password with {@link PasswordHasher#hash} and saves a {@link User} with the
 *       normalized email and the <em>hash</em>. The raw password is never saved.</li>
 *   <li>It sends the welcome mail to the normalized email, and only after the user was saved. If saving throws,
 *       the exception propagates and no mail is sent.</li>
 *   <li>If the mailer throws {@link MailException}, the sign-up still succeeds and the result says
 *       {@code mailSent == false}: the account exists, and the mail can be sent later. Any other exception from
 *       the mailer propagates.</li>
 *   <li>The result holds the id that {@link UserRepository#save} returned, the normalized email, and whether the
 *       welcome mail was sent.</li>
 *   <li>A {@code null} email or password throws {@link NullPointerException} before any collaborator is
 *       called.</li>
 * </ol>
 */
public final class SignupService {

    private static final System.Logger LOG = System.getLogger(SignupService.class.getName());

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final WelcomeMailer welcomeMailer;

    public SignupService(UserRepository users, PasswordHasher passwordHasher, WelcomeMailer welcomeMailer) {
        this.users = Objects.requireNonNull(users, "users");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher");
        this.welcomeMailer = Objects.requireNonNull(welcomeMailer, "welcomeMailer");
    }

    /**
     * Creates an account for {@code email}, following the rules above.
     *
     * @throws DuplicateEmailException if an account with this email already exists
     */
    public SignupResult signUp(String email, String rawPassword) {
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(rawPassword, "rawPassword");

        String normalizedEmail = email.strip().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException(normalizedEmail);
        }

        String passwordHash = passwordHasher.hash(rawPassword);
        long userId = users.save(new User(normalizedEmail, passwordHash));

        boolean mailSent = sendWelcomeMail(normalizedEmail);
        return new SignupResult(userId, normalizedEmail, mailSent);
    }

    private boolean sendWelcomeMail(String email) {
        try {
            welcomeMailer.sendWelcome(email);
            return true;
        } catch (MailException e) {
            // Handled, not swallowed: the account exists, so failing the whole sign-up now would be worse than a
            // late mail. The result tells the caller that the mail is still owed, and the log keeps the reason.
            LOG.log(Level.WARNING, "Welcome mail failed; the sign-up still succeeded", e);
            return false;
        }
    }
}
