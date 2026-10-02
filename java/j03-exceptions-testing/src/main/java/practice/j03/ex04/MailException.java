package practice.j03.ex04;

/** Thrown by a {@link WelcomeMailer} when a mail can't be sent. */
public class MailException extends RuntimeException {

    public MailException(String message) {
        super(message);
    }

    public MailException(String message, Throwable cause) {
        super(message, cause);
    }
}
