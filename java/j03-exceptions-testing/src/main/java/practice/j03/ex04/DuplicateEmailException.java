package practice.j03.ex04;

/** Thrown by {@link SignupService} when an account with the email already exists. */
public class DuplicateEmailException extends RuntimeException {

    private final String email;

    public DuplicateEmailException(String email) {
        super("An account with this email already exists: " + email);
        this.email = email;
    }

    /** The normalized email that is already taken. */
    public String email() {
        return email;
    }
}
