package com.example.bookstore.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Switches on {@code @Async} methods. They run on the task executor that Spring Boot configures
 * ({@code spring.task.execution.*}), which applies the application's {@code TaskDecorator} bean to every task.
 *
 * <p>A separate class, like {@link JpaAuditingConfiguration}, so that the application class stays free of features.
 */
@Configuration(proxyBeanMethods = false)
@EnableAsync
public class AsyncConfiguration {
}
