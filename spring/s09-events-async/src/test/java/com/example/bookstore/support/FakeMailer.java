package com.example.bookstore.support;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import com.example.bookstore.notification.Email;
import com.example.bookstore.notification.Mailer;

/**
 * The email provider for tests: it records the emails instead of sending them, and can be told to fail.
 *
 * <p>{@link IntegrationTestConfiguration} registers it as the primary {@link Mailer}, and
 * {@link IntegrationTestListener} resets it before each test. It is safe to call from several threads at once.
 */
public final class FakeMailer implements Mailer {

    private final List<Email> sent = new CopyOnWriteArrayList<>();
    private volatile RuntimeException failure;

    @Override
    public void send(Email email) {
        RuntimeException plannedFailure = failure;
        if (plannedFailure != null) {
            throw plannedFailure;
        }
        sent.add(email);
    }

    /** The emails sent so far, oldest first. */
    public List<Email> sent() {
        return List.copyOf(sent);
    }

    /** From now on, every call throws this exception and sends nothing. */
    public void failWith(RuntimeException exception) {
        failure = Objects.requireNonNull(exception, "exception");
    }

    /** Forgets the emails and stops failing. */
    public void reset() {
        sent.clear();
        failure = null;
    }
}
