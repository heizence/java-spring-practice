package com.example.bookstore.notification;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import lombok.extern.slf4j.Slf4j;

/**
 * A {@link MailSender} for developers' machines: it keeps every mail in memory and logs its subject, instead of
 * sending it. No real mail leaves a laptop, and developers still see what the application would have sent.
 */
@Slf4j
public class FakeMailSender implements MailSender {

    private final List<Mail> sentMails = new CopyOnWriteArrayList<>();

    @Override
    public void send(Mail mail) {
        sentMails.add(mail);
        log.info("Fake mail, not sent: '{}'", mail.subject());
    }

    /** The mails "sent" so far, oldest first, as an unmodifiable list. */
    public List<Mail> sentMails() {
        return List.copyOf(sentMails);
    }
}
