package practice.j08.ex03;

/**
 * Thread-safe statistics for a stream of measurements, such as response times in milliseconds.
 *
 * <p>Many request threads call {@link #record} at the same time, and a monitoring thread calls {@link #snapshot}
 * now and then. Use atomics, not locks: an {@link java.util.concurrent.atomic.AtomicLong AtomicLong} for the count,
 * a {@link java.util.concurrent.atomic.LongAdder LongAdder} for the sum, and
 * {@link java.util.concurrent.atomic.AtomicLong#accumulateAndGet AtomicLong.accumulateAndGet} (or your own
 * compare-and-set loop) for the maximum.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>{@link #record(long)} adds one measurement: the count grows by 1, the sum grows by {@code value}, and the
 *       maximum becomes {@code value} if {@code value} is larger.</li>
 *   <li>No update is lost, however many threads record at the same time. In particular, the maximum never goes
 *       down: right after {@code record(value)} returns, the maximum is at least {@code value}.</li>
 *   <li>A negative value throws {@link IllegalArgumentException} with the value in its message, and changes
 *       nothing.</li>
 *   <li>{@link #snapshot()} returns the count, the sum and the maximum. Before the first measurement, all three
 *       are 0.</li>
 *   <li>A snapshot taken while other threads are recording reads the three numbers one after another, so they may
 *       not fit together exactly. A snapshot taken while nobody is recording is exact.</li>
 * </ul>
 */
public final class Stats {

    /** Adds one measurement. */
    public void record(long value) {
        // TODO: J08-03
        throw new UnsupportedOperationException("TODO: J08-03");
    }

    /** The current count, sum and maximum. */
    public Snapshot snapshot() {
        // TODO: J08-03
        throw new UnsupportedOperationException("TODO: J08-03");
    }

    /**
     * The numbers of a {@link Stats} at one moment.
     *
     * @param count the number of measurements
     * @param sum   the sum of all measurements
     * @param max   the largest measurement, or 0 if there are none
     */
    public record Snapshot(long count, long sum, long max) {

        /** The average measurement, or 0.0 if there are none. */
        public double mean() {
            return count == 0 ? 0.0 : (double) sum / count;
        }
    }
}
