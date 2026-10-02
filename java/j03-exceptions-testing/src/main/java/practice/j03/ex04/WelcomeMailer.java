package practice.j03.ex04;

/** Sends the welcome mail to new users. In a real application this talks to a mail server. */
public interface WelcomeMailer {

    /**
     * Sends the welcome mail to {@code email}.
     *
     * @throws MailException if the mail can't be sent, for example because the mail server is down
     */
    void sendWelcome(String email);
}
