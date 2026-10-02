package practice.j08.ex03;

/**
 * Estimates π on a background thread until another thread tells it to stop.
 *
 * <p>It adds up the Leibniz series π/4 = 1 − 1/3 + 1/5 − 1/7 + … for as long as it runs. The work never ends by
 * itself, so the only way out of the loop is {@link #stop()}. This is the classic "stop flag" pattern, and the
 * classic place for a visibility bug.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>{@link #run()} adds one term per loop iteration and checks between iterations whether {@link #stop()} has
 *       been called. Once it has, {@code run()} stores its result and returns.</li>
 *   <li>{@code stop()} may be called from any thread, at any time, any number of times. {@code run()} must notice
 *       it promptly (well within a second), on every JVM and every CPU.</li>
 *   <li>If {@code stop()} was called before {@code run()} starts, {@code run()} returns at once, with 0 terms.</li>
 *   <li>{@link #terms()} and {@link #estimate()} return the result of {@code run()}: the number of terms it added,
 *       and 4 × their sum. Both are 0 until {@code run()} has finished. Read them after the thread that ran
 *       {@code run()} has finished, for example after {@link Thread#join()}.</li>
 * </ul>
 */
public final class PiEstimator implements Runnable {

    /** Adds terms until {@link #stop()} is called, then stores the result. */
    @Override
    public void run() {
        // TODO: J08-03
        throw new UnsupportedOperationException("TODO: J08-03");
    }

    /** Asks {@link #run()} to stop. Safe to call from any thread. */
    public void stop() {
        // TODO: J08-03
        throw new UnsupportedOperationException("TODO: J08-03");
    }

    /** The number of terms that {@link #run()} added, or 0 before it has finished. */
    public long terms() {
        // TODO: J08-03
        throw new UnsupportedOperationException("TODO: J08-03");
    }

    /** The estimate of π, or 0.0 before {@link #run()} has finished. */
    public double estimate() {
        // TODO: J08-03
        throw new UnsupportedOperationException("TODO: J08-03");
    }
}
