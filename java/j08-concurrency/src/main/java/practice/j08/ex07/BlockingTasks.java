package practice.j08.ex07;

import java.util.function.IntConsumer;

/**
 * Runs many blocking tasks at once, one virtual thread per task.
 *
 * <p>A <em>blocking</em> task spends most of its time waiting: for a database, another service or a file. While a
 * virtual thread waits, it doesn't hold on to an operating-system thread. So you can afford one virtual thread per
 * task, even for 10,000 tasks.
 *
 * <h2>Rules for {@link #runAll}</h2>
 * <ul>
 *   <li>Calls {@code task.accept(i)} once for every {@code i} from 0 to {@code count - 1}, each on its own new
 *       virtual thread (use {@link java.util.concurrent.Executors#newVirtualThreadPerTaskExecutor()}). All tasks run
 *       at the same time.</li>
 *   <li>Returns when every task has finished, with the number of tasks that threw an exception. A task that throws
 *       doesn't stop the others.</li>
 *   <li>A {@code count} of 0 runs nothing and returns 0. A negative {@code count} throws
 *       {@link IllegalArgumentException}, and a {@code null} task throws {@link NullPointerException}.</li>
 * </ul>
 */
public final class BlockingTasks {

    private BlockingTasks() {
    }

    /**
     * Runs {@code count} tasks at the same time, each on its own virtual thread, and waits for all of them.
     *
     * @return the number of tasks that threw an exception
     */
    public static int runAll(int count, IntConsumer task) {
        // TODO: J08-07
        throw new UnsupportedOperationException("TODO: J08-07");
    }
}
