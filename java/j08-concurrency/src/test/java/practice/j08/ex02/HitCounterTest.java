package practice.j08.ex02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import practice.testkit.Concurrently;

@DisplayName("J08-02 · Race conditions")
class HitCounterTest {

    private static final int THREADS = 8;

    private final HitCounter counter = new HitCounter();

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("starts with no page views and no visitors")
        void startsEmpty() {
            assertThat(counter.hits()).isZero();
            assertThat(counter.visitors()).isZero();
        }

        @Test
        @DisplayName("counts page views")
        void countsHits() {
            counter.hit();
            counter.hit();
            counter.hit();

            assertThat(counter.hits()).isEqualTo(3);
        }

        @Test
        @DisplayName("registers a new visitor with their landing page")
        void registersNewVisitor() {
            boolean registered = counter.registerIfAbsent("ana", "/books");

            assertThat(registered).isTrue();
            assertThat(counter.landingPage("ana")).contains("/books");
            assertThat(counter.visitors()).isOne();
        }

        @Test
        @DisplayName("keeps the first landing page when a known visitor registers again")
        void keepsFirstLandingPage() {
            counter.registerIfAbsent("ana", "/books");

            boolean registeredAgain = counter.registerIfAbsent("ana", "/cart");

            assertThat(registeredAgain).isFalse();
            assertThat(counter.landingPage("ana")).contains("/books");
            assertThat(counter.visitors()).isOne();
        }

        @Test
        @DisplayName("returns empty for a visitor who never registered")
        void unknownVisitor() {
            assertThat(counter.landingPage("nobody")).isEmpty();
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("rejects a null visitor id or page, and registers nobody")
        void rejectsNulls() {
            assertThatThrownBy(() -> counter.registerIfAbsent(null, "/books")).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> counter.registerIfAbsent("ana", null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> counter.landingPage(null)).isInstanceOf(NullPointerException.class);
            assertThat(counter.visitors()).isZero();
        }
    }

    @Nested
    @DisplayName("Concurrency")
    class Concurrency {

        @Test
        @DisplayName("loses no page view when 8 threads count at the same time")
        void losesNoHits() {
            int hitsPerThread = 100_000;

            Concurrently.run(THREADS, thread -> {
                for (int i = 0; i < hitsPerThread; i++) {
                    counter.hit();
                }
            });

            assertThat(counter.hits()).as("page views counted").isEqualTo((long) THREADS * hitsPerThread);
        }

        @Test
        @DisplayName("exactly one thread gets true when 8 threads register the same new visitors at the same time")
        void exactlyOneWinnerPerVisitor() {
            int visitorCount = 50_000;
            String[] visitors = visitorIds(visitorCount);
            var winners = new AtomicIntegerArray(visitorCount); // how many threads got `true`, per visitor

            // Every thread registers the same visitors in the same order, so threads often try the same new visitor
            // at the same moment.
            Concurrently.run(THREADS, thread -> {
                String page = "/page-" + thread;
                for (int v = 0; v < visitorCount; v++) {
                    if (counter.registerIfAbsent(visitors[v], page)) {
                        winners.incrementAndGet(v);
                    }
                }
            });

            List<String> wrong = new ArrayList<>();
            for (int v = 0; v < visitorCount && wrong.size() < 5; v++) {
                if (winners.get(v) != 1) {
                    wrong.add(visitors[v] + " got true " + winners.get(v) + " times");
                }
            }
            assertThat(wrong).as("visitors who did not get exactly one `true`").isEmpty();
        }

        @Test
        @DisplayName("keeps the winner's page and counts every visitor once when 8 threads register at the same time")
        void keepsWinnersPage() {
            int visitorCount = 50_000;
            String[] visitors = visitorIds(visitorCount);
            var winnerPages = new AtomicReferenceArray<String>(visitorCount);

            Concurrently.run(THREADS, thread -> {
                String page = "/page-" + thread;
                for (int v = 0; v < visitorCount; v++) {
                    if (counter.registerIfAbsent(visitors[v], page)) {
                        winnerPages.set(v, page);
                    }
                }
            });

            List<String> wrong = new ArrayList<>();
            for (int v = 0; v < visitorCount && wrong.size() < 5; v++) {
                String kept = counter.landingPage(visitors[v]).orElse(null);
                if (kept == null || !kept.equals(winnerPages.get(v))) {
                    wrong.add(visitors[v] + " kept " + kept + " but the winner registered " + winnerPages.get(v));
                }
            }
            assertThat(wrong).as("visitors whose landing page is not the winner's page").isEmpty();
            assertThat(counter.visitors()).isEqualTo(visitorCount);
        }

        private static String[] visitorIds(int count) {
            return IntStream.range(0, count).mapToObj(v -> "visitor-" + v).toArray(String[]::new);
        }
    }
}
