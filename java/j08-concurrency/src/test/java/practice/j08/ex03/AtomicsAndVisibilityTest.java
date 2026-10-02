package practice.j08.ex03;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import practice.j08.support.Blocking;
import practice.testkit.Concurrently;

@DisplayName("J08-03 · Atomics and visibility")
class AtomicsAndVisibilityTest {

    private static final int THREADS = 8;

    @Nested
    @DisplayName("Stats")
    class StatsTests {

        private final Stats stats = new Stats();

        @Nested
        @DisplayName("Basic")
        class Basic {

            @Test
            @DisplayName("a new Stats has count, sum and maximum 0")
            void startsAtZero() {
                assertThat(stats.snapshot()).isEqualTo(new Stats.Snapshot(0, 0, 0));
            }

            @Test
            @DisplayName("record() adds to the count and the sum, and keeps the largest value")
            void recordsValues() {
                stats.record(5);
                stats.record(12);
                stats.record(7);

                assertThat(stats.snapshot()).isEqualTo(new Stats.Snapshot(3, 24, 12));
            }

            @Test
            @DisplayName("the mean is the sum divided by the count")
            void mean() {
                stats.record(1);
                stats.record(2);

                assertThat(stats.snapshot().mean()).isEqualTo(1.5);
            }
        }

        @Nested
        @DisplayName("Edge cases")
        class EdgeCases {

            @Test
            @DisplayName("0 is a valid measurement")
            void zeroIsValid() {
                stats.record(0);

                assertThat(stats.snapshot()).isEqualTo(new Stats.Snapshot(1, 0, 0));
            }

            @Test
            @DisplayName("the maximum stays when smaller values come later")
            void maximumStays() {
                stats.record(10);
                stats.record(3);

                assertThat(stats.snapshot().max()).isEqualTo(10);
            }

            @Test
            @DisplayName("the sum can grow beyond the int range")
            void sumBeyondIntRange() {
                stats.record(2_000_000_000);
                stats.record(2_000_000_000);
                stats.record(2_000_000_000);

                assertThat(stats.snapshot().sum()).isEqualTo(6_000_000_000L);
            }

            @Test
            @DisplayName("the mean of no measurements is 0.0")
            void meanOfNothing() {
                assertThat(stats.snapshot().mean()).isZero();
            }

            @Test
            @DisplayName("a snapshot doesn't change when more values are recorded later")
            void snapshotIsAValue() {
                stats.record(4);
                Stats.Snapshot before = stats.snapshot();

                stats.record(9);

                assertThat(before).isEqualTo(new Stats.Snapshot(1, 4, 4));
            }
        }

        @Nested
        @DisplayName("Errors")
        class Errors {

            @Test
            @DisplayName("rejects a negative value and changes nothing")
            void rejectsNegativeValue() {
                stats.record(5);

                assertThatThrownBy(() -> stats.record(-1))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("-1");
                assertThat(stats.snapshot()).isEqualTo(new Stats.Snapshot(1, 5, 5));
            }
        }

        @Nested
        @DisplayName("Concurrency")
        class Concurrency {

            @Test
            @DisplayName("loses no count and no sum when 8 threads record at the same time")
            void losesNoUpdates() {
                int valuesPerThread = 50_000;

                Concurrently.run(THREADS, thread -> {
                    for (int value = 1; value <= valuesPerThread; value++) {
                        stats.record(value);
                    }
                });

                long sumPerThread = (long) valuesPerThread * (valuesPerThread + 1) / 2;
                assertThat(stats.snapshot())
                        .isEqualTo(new Stats.Snapshot((long) THREADS * valuesPerThread, THREADS * sumPerThread,
                                valuesPerThread));
            }

            @Test
            @DisplayName("the maximum never goes down while 8 threads record rising values")
            void maximumNeverGoesDown() {
                int valuesPerThread = 50_000;
                var nextValue = new AtomicLong();
                var tooLow = new LongAdder();

                // Almost every value is a new maximum, so the threads compete for the maximum all the time.
                Concurrently.run(THREADS, thread -> {
                    for (int i = 0; i < valuesPerThread; i++) {
                        long value = nextValue.incrementAndGet();
                        stats.record(value);
                        if (stats.snapshot().max() < value) {
                            tooLow.increment(); // another thread overwrote a larger maximum with a smaller one
                        }
                    }
                });

                assertThat(tooLow.sum())
                        .as("times the maximum was lower than the value recorded right before")
                        .isZero();
                assertThat(stats.snapshot().max()).isEqualTo((long) THREADS * valuesPerThread);
            }
        }
    }

    @Nested
    @DisplayName("PiEstimator")
    class PiEstimatorTests {

        private final PiEstimator estimator = new PiEstimator();

        /** Runs the estimator on a daemon thread, so a loop that never ends can't keep the test JVM alive. */
        private Thread startWorker() {
            return Thread.ofPlatform().daemon().name("pi-estimator").start(estimator);
        }

        @Nested
        @DisplayName("Basic")
        class Basic {

            @Test
            @DisplayName("run() keeps working until another thread calls stop(), then returns")
            void runsUntilStopped() throws InterruptedException {
                Thread worker = startWorker();
                try {
                    Blocking.sleep(Duration.ofMillis(100));
                    assertThat(worker.isAlive()).as("still running before stop()").isTrue();

                    estimator.stop();

                    assertThat(worker.join(Duration.ofSeconds(2))).as("finished after stop()").isTrue();
                } finally {
                    estimator.stop();
                }
            }

            @Test
            @DisplayName("run() notices stop() promptly, even after the JIT compiler has optimized its loop")
            void stopIsVisibleToTheWorker() throws InterruptedException {
                Thread worker = startWorker();
                try {
                    // Long enough for the JIT compiler to compile the loop into fast machine code. A flag that is
                    // not volatile may then be read only once, before the loop, and the loop never ends.
                    Blocking.sleep(Duration.ofMillis(500));

                    estimator.stop();

                    assertThat(worker.join(Duration.ofSeconds(1)))
                            .as("the worker thread finished within 1 second after stop()")
                            .isTrue();
                } finally {
                    estimator.stop();
                }
            }

            @Test
            @DisplayName("after run() has finished, estimate() is close to π and terms() counts the terms")
            void estimatesPi() throws InterruptedException {
                Thread worker = startWorker();
                try {
                    Blocking.sleep(Duration.ofMillis(100));
                    estimator.stop();
                    assertThat(worker.join(Duration.ofSeconds(2))).as("finished after stop()").isTrue();

                    assertThat(estimator.terms()).isGreaterThan(1_000);
                    assertThat(estimator.estimate()).isCloseTo(Math.PI, within(0.001));
                } finally {
                    estimator.stop();
                }
            }
        }

        @Nested
        @DisplayName("Edge cases")
        class EdgeCases {

            @Test
            @DisplayName("terms() and estimate() are 0 before run() has run")
            void zeroBeforeRun() {
                assertThat(estimator.terms()).isZero();
                assertThat(estimator.estimate()).isZero();
            }

            @Test
            @DisplayName("stop() before run() makes run() return at once, with 0 terms")
            void stopBeforeRun() throws InterruptedException {
                estimator.stop();

                Thread worker = startWorker();
                try {
                    assertThat(worker.join(Duration.ofSeconds(1))).as("run() returned").isTrue();
                    assertThat(estimator.terms()).isZero();
                    assertThat(estimator.estimate()).isZero();
                } finally {
                    estimator.stop();
                }
            }

            @Test
            @DisplayName("stop() can be called more than once")
            void stopTwice() throws InterruptedException {
                Thread worker = startWorker();
                try {
                    estimator.stop();
                    estimator.stop();

                    assertThat(worker.join(Duration.ofSeconds(1))).as("run() returned").isTrue();
                } finally {
                    estimator.stop();
                }
            }
        }
    }
}
