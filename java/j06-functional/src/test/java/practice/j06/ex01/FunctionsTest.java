package practice.j06.ex01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("J06-01 · Lambdas and functional interfaces")
class FunctionsTest {

    /** A supplier that throws for its first {@code failures} calls, then returns "done". It counts its calls. */
    private static final class Flaky implements Supplier<String> {

        private final int failures;
        private int calls;

        Flaky(int failures) {
            this.failures = failures;
        }

        @Override
        public String get() {
            calls++;
            if (calls <= failures) {
                throw new IllegalStateException("attempt " + calls + " failed");
            }
            return "done";
        }

        int calls() {
            return calls;
        }
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("compose() applies the first function, then the second")
        void composeAppliesFirstThenSecond() {
            Function<Integer, Integer> plusOneThenDouble = Functions.compose(x -> x + 1, x -> x * 2);

            assertThat(plusOneThenDouble.apply(3)).isEqualTo(8);
        }

        @Test
        @DisplayName("compose() can change the type along the way")
        void composeChangesType() {
            Function<String, String> describeLength = Functions.compose(String::length, n -> n + " characters");

            assertThat(describeLength.apply("lambda")).isEqualTo("6 characters");
        }

        @Test
        @DisplayName("a memoized function returns the same results as the original function")
        void memoizeKeepsResults() {
            Function<String, Integer> length = Functions.memoize(String::length);

            assertThat(length.apply("java")).isEqualTo(4);
            assertThat(length.apply("kotlin")).isEqualTo(6);
            assertThat(length.apply("java")).isEqualTo(4);
        }

        @Test
        @DisplayName("a memoized function runs the original function only once per input")
        void memoizeRunsOncePerInput() {
            var calls = new AtomicInteger();
            Function<String, Integer> length = Functions.memoize(text -> {
                calls.incrementAndGet();
                return text.length();
            });

            length.apply("java");
            length.apply("java");
            length.apply("kotlin");
            length.apply("java");

            assertThat(calls).hasValue(2);
        }

        @Test
        @DisplayName("retry() returns the result of the first attempt that succeeds")
        void retryReturnsFirstSuccess() {
            var flaky = new Flaky(2);

            String result = Functions.retry(flaky, 3);

            assertThat(result).isEqualTo("done");
            assertThat(flaky.calls()).isEqualTo(3);
        }

        @Test
        @DisplayName("retry() stops after the first attempt when it succeeds")
        void retryStopsAfterSuccess() {
            var flaky = new Flaky(0);

            Functions.retry(flaky, 5);

            assertThat(flaky.calls()).isEqualTo(1);
        }

        @Test
        @DisplayName("counter() counts up from its start value")
        void counterCountsUp() {
            IntSupplier counter = Functions.counter(5);

            assertThat(counter.getAsInt()).isEqualTo(5);
            assertThat(counter.getAsInt()).isEqualTo(6);
            assertThat(counter.getAsInt()).isEqualTo(7);
        }

        @Test
        @DisplayName("parser() parses decimal text into an int")
        void parserParses() {
            assertThat(Functions.parser().applyAsInt("42")).isEqualTo(42);
            assertThat(Functions.parser().applyAsInt("-7")).isEqualTo(-7);
        }

        @Test
        @DisplayName("isIn() tests whether a word is in the collection")
        void isInTestsMembership() {
            var allowed = Functions.isIn(List.of("GET", "POST"));

            assertThat(allowed.test("GET")).isTrue();
            assertThat(allowed.test("DELETE")).isFalse();
        }

        @Test
        @DisplayName("startsWith() tests the first argument against the second as a prefix")
        void startsWithUsesFirstArgumentAsText() {
            var startsWith = Functions.startsWith();

            assertThat(startsWith.test("functional", "fun")).isTrue();
            assertThat(startsWith.test("fun", "functional")).isFalse();
        }

        @Test
        @DisplayName("listFactory() creates an empty list that can be changed")
        void listFactoryCreatesModifiableList() {
            List<String> list = Functions.listFactory().get();

            list.add("first");

            assertThat(list).containsExactly("first");
        }

        @Test
        @DisplayName("TriFunction.andThen() applies a function to the result")
        void triFunctionAndThen() {
            TriFunction<Integer, Integer, Integer, Integer> clamp =
                    (value, min, max) -> Math.max(min, Math.min(max, value));

            TriFunction<Integer, Integer, Integer, String> clampToText = clamp.andThen(n -> "value: " + n);

            assertThat(clampToText.apply(15, 0, 10)).isEqualTo("value: 10");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("compose() doesn't run either function before the result is applied")
        void composeIsLazy() {
            var calls = new AtomicInteger();
            Function<Integer, Integer> counting = x -> {
                calls.incrementAndGet();
                return x;
            };

            Function<Integer, Integer> composed = Functions.compose(counting, counting);

            assertThat(calls).hasValue(0);
            composed.apply(1);
            assertThat(calls).hasValue(2);
        }

        @Test
        @DisplayName("each memoized function has its own memory")
        void memoizedFunctionsDontShareMemory() {
            Function<String, String> upper = Functions.memoize(text -> text.toUpperCase());
            Function<String, String> reversed = Functions.memoize(text -> new StringBuilder(text).reverse().toString());

            upper.apply("abc");

            assertThat(reversed.apply("abc")).isEqualTo("cba");
        }

        @Test
        @DisplayName("a memoized function treats equal inputs as the same input")
        void memoizeComparesWithEquals() {
            var calls = new AtomicInteger();
            Function<List<Integer>, Integer> size = Functions.memoize(list -> {
                calls.incrementAndGet();
                return list.size();
            });

            size.apply(List.of(1, 2));
            size.apply(new ArrayList<>(List.of(1, 2))); // a different object, but equal

            assertThat(calls).hasValue(1);
        }

        @Test
        @DisplayName("retry() with one attempt doesn't retry")
        void retryWithOneAttempt() {
            var flaky = new Flaky(1);

            assertThatThrownBy(() -> Functions.retry(flaky, 1)).isInstanceOf(IllegalStateException.class);
            assertThat(flaky.calls()).isEqualTo(1);
        }

        @Test
        @DisplayName("retry() makes exactly `attempts` attempts when every attempt fails")
        void retryUsesEveryAttempt() {
            var flaky = new Flaky(Integer.MAX_VALUE);

            assertThatThrownBy(() -> Functions.retry(flaky, 4)).isInstanceOf(IllegalStateException.class);
            assertThat(flaky.calls()).isEqualTo(4);
        }

        @Test
        @DisplayName("retry() succeeds when the last allowed attempt succeeds")
        void retrySucceedsOnLastAttempt() {
            var flaky = new Flaky(3);

            assertThat(Functions.retry(flaky, 4)).isEqualTo("done");
        }

        @Test
        @DisplayName("retry() rethrows the exception of the last attempt")
        void retryRethrowsLastException() {
            var flaky = new Flaky(Integer.MAX_VALUE);

            assertThatThrownBy(() -> Functions.retry(flaky, 3))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("attempt 3 failed");
        }

        @Test
        @DisplayName("retry() doesn't retry an Error")
        void retryDoesNotRetryErrors() {
            var calls = new AtomicInteger();
            Supplier<String> broken = () -> {
                calls.incrementAndGet();
                throw new AssertionError("broken invariant");
            };

            assertThatThrownBy(() -> Functions.retry(broken, 3)).isInstanceOf(AssertionError.class);
            assertThat(calls).hasValue(1);
        }

        @Test
        @DisplayName("two counters count independently")
        void countersAreIndependent() {
            IntSupplier first = Functions.counter(1);
            IntSupplier second = Functions.counter(1);

            first.getAsInt();
            first.getAsInt();

            assertThat(second.getAsInt()).isEqualTo(1);
            assertThat(first.getAsInt()).isEqualTo(3);
        }

        @Test
        @DisplayName("a counter can start below zero")
        void counterWithNegativeStart() {
            IntSupplier counter = Functions.counter(-1);

            assertThat(counter.getAsInt()).isEqualTo(-1);
            assertThat(counter.getAsInt()).isEqualTo(0);
        }

        @Test
        @DisplayName("a counter wraps around after Integer.MAX_VALUE, like int arithmetic")
        void counterWrapsAround() {
            IntSupplier counter = Functions.counter(Integer.MAX_VALUE);

            assertThat(counter.getAsInt()).isEqualTo(Integer.MAX_VALUE);
            assertThat(counter.getAsInt()).isEqualTo(Integer.MIN_VALUE);
        }

        @Test
        @DisplayName("parser() handles the largest and smallest int")
        void parserLimits() {
            assertThat(Functions.parser().applyAsInt("2147483647")).isEqualTo(Integer.MAX_VALUE);
            assertThat(Functions.parser().applyAsInt("-2147483648")).isEqualTo(Integer.MIN_VALUE);
        }

        @Test
        @DisplayName("isIn() isn't affected by later changes to the collection")
        void isInUsesCopy() {
            List<String> allowed = new ArrayList<>(List.of("GET"));
            var isAllowed = Functions.isIn(allowed);

            allowed.add("DELETE");
            allowed.remove("GET");

            assertThat(isAllowed.test("GET")).isTrue();
            assertThat(isAllowed.test("DELETE")).isFalse();
        }

        @Test
        @DisplayName("isIn() accepts a collection with duplicates")
        void isInWithDuplicates() {
            var isAllowed = Functions.isIn(List.of("GET", "GET", "POST"));

            assertThat(isAllowed.test("POST")).isTrue();
        }

        @Test
        @DisplayName("isIn() of an empty collection allows nothing")
        void isInEmpty() {
            var isAllowed = Functions.isIn(Set.of());

            assertThat(isAllowed.test("GET")).isFalse();
        }

        @Test
        @DisplayName("startsWith() is true for an empty prefix")
        void startsWithEmptyPrefix() {
            assertThat(Functions.startsWith().test("anything", "")).isTrue();
        }

        @Test
        @DisplayName("listFactory() returns a different list on every call")
        void listFactoryReturnsNewLists() {
            Supplier<List<String>> factory = Functions.listFactory();

            List<String> first = factory.get();
            first.add("x");
            List<String> second = factory.get();

            assertThat(second).isNotSameAs(first);
            assertThat(second).isEmpty();
        }

        @Test
        @DisplayName("String::substring fits TriFunction: the first argument becomes the receiver")
        void unboundReferenceFitsTriFunction() {
            TriFunction<String, Integer, Integer, String> substring = String::substring;

            var shout = substring.andThen(String::toUpperCase);

            assertThat(shout.apply("functional", 0, 3)).isEqualTo("FUN");
        }

        @Test
        @DisplayName("TriFunction.andThen() can be chained")
        void andThenChains() {
            TriFunction<Integer, Integer, Integer, Integer> sum = (a, b, c) -> a + b + c;

            var result = sum.andThen(n -> n * 10).andThen(n -> "total " + n);

            assertThat(result.apply(1, 2, 3)).isEqualTo("total 60");
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("compose() rejects a null function right away")
        void composeRejectsNull() {
            Function<Integer, Integer> identity = x -> x;

            assertThatThrownBy(() -> Functions.compose(null, identity)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Functions.compose(identity, null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("memoize() rejects a null function right away")
        void memoizeRejectsNull() {
            assertThatThrownBy(() -> Functions.memoize(null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("a memoized function rejects a null result with NullPointerException and stores nothing")
        void memoizeRejectsNullResult() {
            var calls = new AtomicInteger();
            Function<String, String> lookup = Functions.memoize(key -> {
                calls.incrementAndGet();
                return null;
            });

            assertThatThrownBy(() -> lookup.apply("missing")).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> lookup.apply("missing")).isInstanceOf(NullPointerException.class);
            assertThat(calls).hasValue(2);
        }

        @ParameterizedTest(name = "attempts = {0}")
        @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
        @DisplayName("retry() rejects fewer than 1 attempt without calling the action")
        void retryRejectsInvalidAttempts(int attempts) {
            var flaky = new Flaky(0);

            assertThatThrownBy(() -> Functions.retry(flaky, attempts)).isInstanceOf(IllegalArgumentException.class);
            assertThat(flaky.calls()).isZero();
        }

        @Test
        @DisplayName("retry() rejects a null action")
        void retryRejectsNullAction() {
            assertThatThrownBy(() -> Functions.retry(null, 3)).isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"", " 42", "4.2", "forty-two", "2147483648"})
        @DisplayName("parser() throws NumberFormatException for text that isn't an int")
        void parserRejectsInvalidText(String text) {
            var parser = Functions.parser();

            assertThatThrownBy(() -> parser.applyAsInt(text)).isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("isIn() rejects a null collection right away")
        void isInRejectsNull() {
            assertThatThrownBy(() -> Functions.isIn(null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("TriFunction.andThen() rejects null right away")
        void andThenRejectsNull() {
            TriFunction<Integer, Integer, Integer, Integer> sum = (a, b, c) -> a + b + c;

            assertThatThrownBy(() -> sum.andThen(null)).isInstanceOf(NullPointerException.class);
        }
    }
}
