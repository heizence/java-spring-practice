package com.example.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.List;

import com.example.bookstore.common.ScheduledJobsConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.config.FixedDelayTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;

/**
 * The schedule of {@link PendingOrderCleanupJob}, checked in small application contexts of their own: only the
 * scheduling configuration, the job, and a mock in place of the cleanup service. No database needed.
 *
 * <p>The tests don't wait for the job to run. They read the task that Spring registered, then run it by hand.
 */
@DisplayName("S09-03 · Scheduled jobs")
class PendingOrderCleanupScheduleTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ScheduledJobsConfiguration.class, PendingOrderCleanupJob.class)
            .withBean(PendingOrderCleanupService.class, () -> mock(PendingOrderCleanupService.class))
            .withPropertyValues("bookstore.orders.pending-cleanup.interval=10m",
                    "bookstore.orders.pending-cleanup.max-age=72h");

    @Nested
    @DisplayName("Scheduling")
    class Scheduling {

        @Test
        @DisplayName("runs the cleanup with a fixed delay of 10 minutes, the first time 10 minutes after startup")
        void schedulesTheCleanup() {
            contextRunner.withPropertyValues("bookstore.scheduling.enabled=true").run(context -> {
                List<ScheduledTask> tasks = scheduledTasks(context);
                assertThat(tasks).hasSize(1);
                assertThat(tasks.getFirst().getTask()).isInstanceOfSatisfying(FixedDelayTask.class, task -> {
                    assertThat(task.getIntervalDuration()).isEqualTo(Duration.ofMinutes(10));
                    assertThat(task.getInitialDelayDuration()).isEqualTo(Duration.ofMinutes(10));
                });

                tasks.getFirst().getTask().getRunnable().run();

                verify(context.getBean(PendingOrderCleanupService.class)).cancelExpiredOrders();
            });
        }

        @Test
        @DisplayName("scheduling is on when bookstore.scheduling.enabled isn't set")
        void onByDefault() {
            contextRunner.run(context -> assertThat(scheduledTasks(context)).hasSize(1));
        }

        @Test
        @DisplayName("with bookstore.scheduling.enabled=false, nothing is scheduled")
        void offWhenDisabled() {
            contextRunner.withPropertyValues("bookstore.scheduling.enabled=false")
                    .run(context -> assertThat(scheduledTasks(context)).isEmpty());
        }
    }

    private static List<ScheduledTask> scheduledTasks(ApplicationContext context) {
        return context.getBeanProvider(ScheduledTaskHolder.class).stream()
                .flatMap(holder -> holder.getScheduledTasks().stream())
                .toList();
    }
}
