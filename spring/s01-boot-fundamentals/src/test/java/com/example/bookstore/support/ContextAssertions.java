package com.example.bookstore.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.core.NestedExceptionUtils;

/**
 * Checks on the application context that an {@code ApplicationContextRunner} started.
 *
 * <p>They check the same as AssertJ's {@code assertThat(context).hasNotFailed()} and {@code hasSingleBean(...)}, which
 * you'll see in most Spring projects. The difference is the failure message: AssertJ's starts with a long description
 * of the context, while these say in one line what went wrong, so {@code ./score} can show it.
 */
public final class ContextAssertions {

    private ContextAssertions() {
    }

    /** Fails if the context didn't start, with the exception that stopped it, in one line. */
    public static void assertStarted(AssertableApplicationContext context) {
        Throwable failure = context.getStartupFailure();
        if (failure != null) {
            // The most specific cause usually says it best: "No qualifying bean of type 'java.time.Clock' available".
            Throwable cause = NestedExceptionUtils.getMostSpecificCause(failure);
            throw new AssertionError("The application context didn't start. %s: %s"
                    .formatted(cause.getClass().getSimpleName(), cause.getMessage()), failure);
        }
    }

    /** Fails if the context started; otherwise returns the exception that stopped it. */
    public static Throwable assertStartupFailed(AssertableApplicationContext context) {
        assertThat(context.getStartupFailure())
                .as("The application context started, but it should have failed")
                .isNotNull();
        return context.getStartupFailure();
    }

    /** Fails unless the context started and holds exactly one bean of this type; returns that bean. */
    public static <T> T singleBean(AssertableApplicationContext context, Class<T> type) {
        assertStarted(context);
        assertThat(context.getBeanNamesForType(type)).as("beans of type %s", type.getSimpleName()).hasSize(1);
        return context.getBean(type);
    }

    /** Fails unless the context started and holds no bean of this type. */
    public static void assertNoBean(AssertableApplicationContext context, Class<?> type) {
        assertStarted(context);
        assertThat(context.getBeanNamesForType(type)).as("beans of type %s", type.getSimpleName()).isEmpty();
    }
}
