package practice.j06.ex04;

import java.util.Objects;
import java.util.Optional;

/**
 * A customer of the shop. Some customers sign up by phone, so they have no email address.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>The id and the name must not be {@code null}.</li>
 *   <li>The email address is optional. A {@code null} email passed to the constructor means "no email".</li>
 *   <li>{@link #email()} returns the address, or an empty {@link Optional} for a customer without one. It never
 *       returns {@code null}.</li>
 *   <li>{@link Optional} is meant for return values only: no field of this class has the type {@code Optional}.</li>
 * </ul>
 */
public final class Customer {

    private final String id;
    private final String name;
    private final Optional<String> email;

    /** A customer without an email address. */
    public Customer(String id, String name) {
        this(id, name, null);
    }

    /** A customer with an email address, or without one if {@code email} is {@code null}. */
    public Customer(String id, String name, String email) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.email = Optional.ofNullable(email);
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    /** The email address, or empty if the customer has none. */
    public Optional<String> email() {
        return email;
    }
}
