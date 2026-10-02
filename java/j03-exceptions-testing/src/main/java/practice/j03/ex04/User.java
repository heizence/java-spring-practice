package practice.j03.ex04;

import java.util.Objects;

/**
 * A user as {@link UserRepository} stores it.
 *
 * @param email        the normalized email address
 * @param passwordHash the hash of the password, never the password itself
 */
public record User(String email, String passwordHash) {

    public User {
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(passwordHash, "passwordHash");
    }
}
