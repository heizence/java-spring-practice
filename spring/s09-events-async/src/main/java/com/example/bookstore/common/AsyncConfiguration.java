package com.example.bookstore.common;

import org.springframework.context.annotation.Configuration;

/**
 * Turns on {@code @Async} methods and defines the executor that sales reports run on.
 *
 * <p>Your job in S09-02. The class stays empty until then, so that the application starts without it. It needs:
 * <ul>
 *   <li>{@code @Async} support switched on;</li>
 *   <li>a bean named {@code reportExecutor}: a {@code ThreadPoolTaskExecutor} with exactly 2 threads, a queue for 10
 *       waiting reports, thread names that start with {@code report-}, and an {@link MdcTaskDecorator}.</li>
 * </ul>
 *
 * <p>Defining an executor bean normally switches off Spring Boot's own executor, {@code applicationTaskExecutor},
 * which runs every other async task. {@code spring.task.execution.mode: force} in {@code application.yml} keeps it.
 */
@Configuration(proxyBeanMethods = false)
public class AsyncConfiguration {

    // TODO: S09-02
}
