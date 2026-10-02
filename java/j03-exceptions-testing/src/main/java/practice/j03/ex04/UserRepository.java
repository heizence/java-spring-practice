package practice.j03.ex04;

/** Stores users. In a real application this talks to the database; in the tests it's a Mockito mock. */
public interface UserRepository {

    /** Whether a user with exactly this email exists. */
    boolean existsByEmail(String email);

    /** Stores {@code user} and returns the id the database generated for it. */
    long save(User user);
}
