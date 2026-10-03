package com.example.bookstore.common;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns on {@code @Scheduled} methods, unless {@code bookstore.scheduling.enabled} is {@code false}.
 *
 * <p>Your job in S09-03. The class stays without annotations until then, so that the application starts without it.
 * Scheduling must be on when the property is {@code true} or missing, and off when it is {@code false}. The tests
 * set it to {@code false} (in {@code IntegrationTest}), so that no job changes the database while a test checks it.
 */
// When the condition fails, Spring skips this class and everything it imports, so @EnableScheduling never registers
// the processor that finds @Scheduled methods. The jobs stay ordinary beans that nothing calls on its own. One switch
// for all jobs: it can also turn them off on all but one instance of the application.
@Configuration(proxyBeanMethods = false)
@ConditionalOnBooleanProperty(name = "bookstore.scheduling.enabled", matchIfMissing = true)
@EnableScheduling
public class ScheduledJobsConfiguration {
}
