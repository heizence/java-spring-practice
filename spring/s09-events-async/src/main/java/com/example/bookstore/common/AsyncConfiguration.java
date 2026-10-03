package com.example.bookstore.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

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
// @EnableAsync sits here, not on BookstoreApplication: test slices start from the application class, and they
// shouldn't have to deal with async proxies.
@Configuration(proxyBeanMethods = false)
@EnableAsync
public class AsyncConfiguration {

    /**
     * A small pool of its own, so that a burst of heavy reports can't take the threads that other async work needs.
     * The bean's name is what {@code @Async("reportExecutor")} refers to.
     */
    @Bean
    public ThreadPoolTaskExecutor reportExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        // A bounded queue: when 2 reports run and 10 wait, the next submission fails at once
        // (TaskRejectedException) instead of piling up work that nobody may wait for any more.
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("report-");
        executor.setTaskDecorator(new MdcTaskDecorator());
        // On shutdown, let running reports finish (for at most 30 seconds) instead of interrupting them.
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }
}
