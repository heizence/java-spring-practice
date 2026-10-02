package practice.j03.ex04;

/**
 * What {@link SignupService#signUp} reports back.
 *
 * @param userId   the id that {@link UserRepository#save} returned
 * @param email    the normalized email
 * @param mailSent whether the welcome mail was sent; {@code false} means it still has to be sent
 */
public record SignupResult(long userId, String email, boolean mailSent) {
}
