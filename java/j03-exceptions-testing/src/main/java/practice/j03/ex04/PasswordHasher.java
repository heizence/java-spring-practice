package practice.j03.ex04;

/**
 * Turns a password into a one-way hash, for example with BCrypt. Hashing is slow on purpose (it makes guessing
 * passwords expensive), so code should only call it when it needs the result.
 */
public interface PasswordHasher {

    /** Returns the hash of {@code rawPassword}. */
    String hash(String rawPassword);
}
