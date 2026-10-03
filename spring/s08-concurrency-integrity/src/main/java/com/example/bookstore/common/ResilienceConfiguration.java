package com.example.bookstore.common;

import org.springframework.context.annotation.Configuration;

/**
 * Switches on Spring's resilience annotations, {@code @Retryable} and {@code @ConcurrencyLimit}. Without that switch,
 * Spring ignores them: an annotated method runs once, without retries and without a limit.
 *
 * <p>A separate class for the same reason as {@link JpaAuditingConfiguration}: settings like this one don't belong on
 * the application class.
 */
// TODO: S08-02
@Configuration(proxyBeanMethods = false)
public class ResilienceConfiguration {
}
