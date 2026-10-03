package com.example.bookstore.common;

import org.springframework.context.annotation.Configuration;

/**
 * Turns on {@code @Scheduled} methods, unless {@code bookstore.scheduling.enabled} is {@code false}.
 *
 * <p>Your job in S09-03. The class stays without annotations until then, so that the application starts without it.
 * Scheduling must be on when the property is {@code true} or missing, and off when it is {@code false}. The tests
 * set it to {@code false} (in {@code IntegrationTest}), so that no job changes the database while a test checks it.
 */
@Configuration(proxyBeanMethods = false)
public class ScheduledJobsConfiguration {

    // TODO: S09-03
}
