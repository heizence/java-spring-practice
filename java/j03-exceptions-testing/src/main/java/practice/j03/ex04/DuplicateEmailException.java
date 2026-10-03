package practice.j03.ex04;

/** Thrown by {@link SignupService} when an account with the email already exists. */
public class DuplicateEmailException extends RuntimeException {

    private final String email;

    public DuplicateEmailException(String email) {
        // The message leaves the address out: it's personal data, and messages end up in logs. Code that needs the
        // address calls email().
        super("An account with this email already exists");
        this.email = email;
    }

    /** The normalized email that is already taken. */
    public String email() {
        return email;
    }
}
