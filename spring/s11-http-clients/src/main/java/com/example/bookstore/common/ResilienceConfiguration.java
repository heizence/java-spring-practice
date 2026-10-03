package com.example.bookstore.common;

import org.springframework.context.annotation.Configuration;

/**
 * Switches on Spring's resilience annotations, {@code @Retryable} and {@code @ConcurrencyLimit}. Without that switch,
 * Spring ignores them: an annotated method runs once, without retries and without a limit.
 *
 * <p>A separate class, because settings like this one don't belong on the application class (see
 * {@code BookstoreApplication}).
 */
// TODO: S11-03
@Configuration(proxyBeanMethods = false)
public class ResilienceConfiguration {
}
