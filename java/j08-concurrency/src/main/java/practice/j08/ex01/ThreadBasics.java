package practice.j08.ex01;

import java.util.List;

/**
 * Runs tasks on plain {@link Thread}s, the building block under every executor.
 *
 * <h2>Rules for {@link #runAll}</h2>
 * <ul>
 *   <li>Every task runs exactly once, on its own new platform thread. No task runs on the calling thread.</li>
 *   <li>The thread for {@code tasks.get(i)} is named {@code "worker-" + i}: {@code worker-0}, {@code worker-1}, …</li>
 *   <li>The tasks run at the same time: start every thread before you wait for any of them.</li>
 *   <li>{@code runAll} returns when every thread has finished. It returns the threads in task order.</li>
 *   <li>An empty list starts no thread and returns an empty list.</li>
 *   <li>A {@code null} list or a {@code null} task throws {@link NullPointerException} before any thread starts.</li>
 *   <li>If the calling thread is interrupted while it waits, {@code runAll} stops waiting and throws
 *       {@link InterruptedException}. The threads it started keep running.</li>
 * </ul>
 */
public final class ThreadBasics {

    private ThreadBasics() {
    }

    /**
     * Runs every task on its own new thread, all at the same time, and waits until all of them have finished.
     *
     * @return the threads, in task order; all of them have finished
     * @throws InterruptedException if the calling thread is interrupted while it waits
     */
    public static List<Thread> runAll(List<Runnable> tasks) throws InterruptedException {
        // TODO: J08-01
        throw new UnsupportedOperationException("TODO: J08-01");
    }
}
