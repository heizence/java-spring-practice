package practice.j08.ex03;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

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
 *   <li>A negative value throws {@link IllegalArgumentException} and changes nothing.</li>
 *   <li>{@link #snapshot()} returns the count, the sum and the maximum. Before the first measurement, all three
 *       are 0.</li>
 *   <li>A snapshot taken while other threads are recording reads the three numbers one after another, so they may
 *       not fit together exactly. A snapshot taken while nobody is recording is exact.</li>
 * </ul>
 */
public final class Stats {

    // Each field is thread-safe on its own. The three are not updated together in one step, which is why a snapshot
    // taken during updates may not fit together exactly. (If you need that, keep an immutable record in an
    // AtomicReference and replace it with updateAndGet, or use a lock.)
    private final AtomicLong count = new AtomicLong();
    // A LongAdder spreads the additions over several internal cells, so threads rarely fight over the same memory.
    // It's faster than AtomicLong when many threads write and few read; sum() adds up the cells when you ask.
    private final LongAdder sum = new LongAdder();
    // Starts at 0, which is also the maximum to report when there are no measurements.
    private final AtomicLong max = new AtomicLong();

    /** Adds one measurement. */
    public void record(long value) {
        count.incrementAndGet();
        if (value < 0) {
            throw new IllegalArgumentException("value must not be negative, but was " + value);
        }
        sum.add(value);
        // "if (value > max.get()) max.set(value)" would be check-then-act: between the check and the set, another
        // thread can store a larger value, which we would then overwrite with our smaller one.
        // accumulateAndGet runs a compare-and-set (CAS) loop for us. It is the same as:
        //     long current;
        //     do {
        //         current = max.get();
        //     } while (value > current && !max.compareAndSet(current, value));
        // compareAndSet(current, value) stores `value` only if the field still holds `current`; if another thread
        // changed it in the meantime, the loop reads the new value and decides again.
        max.accumulateAndGet(value, Math::max);
    }

    /** The current count, sum and maximum. */
    public Snapshot snapshot() {
        return new Snapshot(count.get(), sum.sum(), max.get());
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
