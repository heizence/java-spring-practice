package practice.j08.ex05;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import practice.j08.support.Blocking;

@DisplayName("J08-05 · Concurrent collections and coordination")
class CollectionsAndCoordinationTest {

    @Nested
    @DisplayName("WordCounter.countParallel")
    class CountParallel {

        @Nested
        @DisplayName("Basic")
        class Basic {

            @Test
            @DisplayName("countParallel adds up the words of all documents")
            void countsAllDocuments() {
                Map<String, Long> counts = WordCounter.countParallel(List.of("the cat", "the dog", "the end"), 2);

                assertThat(counts).containsOnly(entry("the", 3L), entry("cat", 1L), entry("dog", 1L), entry("end", 1L));
            }

            @Test
            @DisplayName("countParallel ignores letter case and punctuation")
            void ignoresCaseAndPunctuation() {
                Map<String, Long> counts = WordCounter.countParallel(List.of("Java, java!", "JAVA (25)"), 2);

                assertThat(counts).containsOnly(entry("java", 3L), entry("25", 1L));
            }
        }

        @Nested
        @DisplayName("Edge cases")
        class EdgeCases {

            @Test
            @DisplayName("countParallel returns an empty map for no documents")
            void noDocuments() {
                assertThat(WordCounter.countParallel(List.of(), 2)).isEmpty();
            }

            @Test
            @DisplayName("countParallel returns an empty map for documents without words")
            void documentsWithoutWords() {
                assertThat(WordCounter.countParallel(List.of("", "   ", "?!"), 2)).isEmpty();
            }

            @Test
            @DisplayName("countParallel works with more threads than documents")
            void moreThreadsThanDocuments() {
                Map<String, Long> counts = WordCounter.countParallel(List.of("a b", "b"), 16);

                assertThat(counts).containsOnly(entry("a", 1L), entry("b", 2L));
            }

            @Test
            @DisplayName("countParallel returns a map that can't be modified")
            void unmodifiableResult() {
                Map<String, Long> counts = WordCounter.countParallel(List.of("a"), 1);

                assertThatThrownBy(() -> counts.put("b", 1L)).isInstanceOf(UnsupportedOperationException.class);
            }
        }

        @Nested
        @DisplayName("Errors")
        class Errors {

            @ParameterizedTest(name = "threads = {0}")
            @ValueSource(ints = {0, -1})
            @DisplayName("countParallel rejects fewer than 1 thread")
            void rejectsThreadsBelowOne(int threads) {
                assertThatThrownBy(() -> WordCounter.countParallel(List.of("a"), threads))
                        .isInstanceOf(IllegalArgumentException.class);
            }

            @Test
            @DisplayName("countParallel rejects a null list or a null document")
            void rejectsNulls() {
                assertThatThrownBy(() -> WordCounter.countParallel(null, 2)).isInstanceOf(NullPointerException.class);
                assertThatThrownBy(() -> WordCounter.countParallel(Arrays.asList("a", null), 2))
                        .isInstanceOf(NullPointerException.class);
            }
        }

        @Nested
        @DisplayName("Randomized")
        class Randomized {

            @Test
            @DisplayName("countParallel on 8 threads counts 2,000 random documents like a single-threaded count")
            void matchesSingleThreadedCount() {
                var random = new Random(42); // fixed seed: a failure is reproducible
                List<String> vocabulary = List.of("alpha", "beta", "gamma", "delta", "epsilon", "zeta", "eta", "theta",
                        "iota", "kappa", "lambda", "mu", "nu", "xi", "omicron", "pi", "rho", "sigma", "tau", "upsilon");
                List<String> documents = new ArrayList<>();
                Map<String, Long> expected = new HashMap<>();
                for (int d = 0; d < 2_000; d++) {
                    var document = new StringBuilder();
                    for (int w = 0; w < 50; w++) {
                        String word = vocabulary.get(random.nextInt(vocabulary.size()));
                        document.append(word).append(' ');
                        expected.merge(word, 1L, Long::sum);
                    }
                    documents.add(document.toString());
                }

                Map<String, Long> counts = WordCounter.countParallel(documents, 8);

                assertThat(counts).isEqualTo(expected);
            }
        }
    }

    @Nested
    @DisplayName("Pipeline.process")
    class Process {

        /** Calls {@link Pipeline#process}, and fails instead of hanging if it doesn't return in time. */
        private static <T> void processInTime(Iterator<T> source, int capacity, int consumers, Consumer<T> worker) {
            assertTimeoutPreemptively(Blocking.PATIENCE, () -> Pipeline.process(source, capacity, consumers, worker),
                    "process() did not return in time. Does every consumer get an end marker?");
        }

        private static Iterator<Integer> numbers(int count) {
            return IntStream.rangeClosed(1, count).iterator();
        }

        private static List<Integer> listOfNumbers(int count) {
            return IntStream.rangeClosed(1, count).boxed().toList();
        }

        /** A worker that takes a little time per item, so the queue fills up. */
        private static <T> Consumer<T> slowly(Consumer<T> worker) {
            return item -> {
                LockSupport.parkNanos(20_000);
                worker.accept(item);
            };
        }

        @Nested
        @DisplayName("Basic")
        class Basic {

            @Test
            @DisplayName("process passes every item to the worker exactly once")
            void processesEveryItemOnce() {
                var processed = new ConcurrentLinkedQueue<Integer>();

                processInTime(numbers(1_000), 10, 4, slowly(processed::add));

                assertThat(processed).containsExactlyInAnyOrderElementsOf(listOfNumbers(1_000));
            }

            @Test
            @DisplayName("process runs the consumers at the same time")
            void consumersWorkAtTheSameTime() {
                var barrier = new CyclicBarrier(3);
                var metTheOthers = new AtomicInteger();

                processInTime(numbers(3), 3, 3, item -> {
                    if (Blocking.allArrive(barrier)) {
                        metTheOthers.incrementAndGet();
                    }
                });

                assertThat(metTheOthers).as("items processed together with all the others").hasValue(3);
            }

            @Test
            @DisplayName("process returns only after every item is processed and every consumer has stopped")
            void waitsForConsumers() throws InterruptedException {
                var processed = new AtomicInteger();
                Set<Thread> consumers = ConcurrentHashMap.newKeySet();

                processInTime(numbers(100), 5, 3, slowly(item -> {
                    consumers.add(Thread.currentThread());
                    processed.incrementAndGet();
                }));

                assertThat(processed).hasValue(100);
                for (Thread consumer : consumers) {
                    assertThat(consumer.join(Blocking.PATIENCE)).as("%s has stopped", consumer.getName()).isTrue();
                }
            }
        }

        @Nested
        @DisplayName("Edge cases")
        class EdgeCases {

            @Test
            @DisplayName("process never reads more than `capacity` items ahead of the consumers")
            void appliesBackpressure() {
                int capacity = 5;
                int consumers = 2;
                var read = new AtomicInteger();
                Iterator<Integer> source = new Iterator<>() {
                    private int next = 1;

                    @Override
                    public boolean hasNext() {
                        return next <= 1_000;
                    }

                    @Override
                    public Integer next() {
                        read.incrementAndGet();
                        return next++;
                    }
                };
                var release = new CountDownLatch(1);
                var bothConsumersBusy = new CountDownLatch(consumers);
                Consumer<Integer> worker = item -> {
                    bothConsumersBusy.countDown();
                    Blocking.await(release); // every consumer holds on to its item until the test lets go
                };

                CompletableFuture<Void> producer = Blocking.inBackground(() -> {
                    Pipeline.process(source, capacity, consumers, worker);
                    return null;
                });
                try {
                    Blocking.await(bothConsumersBusy);
                    Blocking.sleep(Duration.ofMillis(200)); // time for the producer to read ahead as far as it can

                    // The consumers hold 2 items, the queue holds 5, and the producer may hold 1 that it can't put.
                    assertThat(read).as("items read from the source")
                            .hasValueLessThanOrEqualTo(capacity + consumers + 1);
                } finally {
                    release.countDown();
                }
                assertThat(producer).succeedsWithin(Blocking.PATIENCE);
                assertThat(read).hasValue(1_000);
            }

            @Test
            @DisplayName("process keeps the order of the source when there is one consumer")
            void keepsOrderWithOneConsumer() {
                List<Integer> processed = new ArrayList<>(); // only one consumer thread writes to it

                processInTime(numbers(200), 3, 1, processed::add);

                assertThat(processed).containsExactlyElementsOf(listOfNumbers(200));
            }

            @Test
            @DisplayName("process finishes at once for an empty source, without calling the worker")
            void emptySource() {
                var calls = new AtomicInteger();

                processInTime(numbers(0), 5, 3, item -> calls.incrementAndGet());

                assertThat(calls).hasValue(0);
            }

            @Test
            @DisplayName("process works with a queue capacity of 1")
            void capacityOne() {
                var processed = new ConcurrentLinkedQueue<Integer>();

                processInTime(numbers(100), 1, 2, processed::add);

                assertThat(processed).containsExactlyInAnyOrderElementsOf(listOfNumbers(100));
            }
        }

        @Nested
        @DisplayName("Errors")
        class Errors {

            @Test
            @DisplayName("process rejects a capacity or a number of consumers below 1")
            void rejectsBadSizes() {
                assertThatThrownBy(() -> Pipeline.process(numbers(1), 0, 1, item -> { }))
                        .isInstanceOf(IllegalArgumentException.class);
                assertThatThrownBy(() -> Pipeline.process(numbers(1), 1, 0, item -> { }))
                        .isInstanceOf(IllegalArgumentException.class);
            }

            @Test
            @DisplayName("process rejects a null source or worker")
            void rejectsNulls() {
                assertThatThrownBy(() -> Pipeline.process(null, 1, 1, item -> { }))
                        .isInstanceOf(NullPointerException.class);
                assertThatThrownBy(() -> Pipeline.process(numbers(1), 1, 1, null))
                        .isInstanceOf(NullPointerException.class);
            }
        }
    }
}
