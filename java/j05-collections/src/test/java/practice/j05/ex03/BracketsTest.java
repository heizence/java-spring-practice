package practice.j05.ex03;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("J05-03 · Stacks and queues")
class BracketsTest {

    @Nested
    @DisplayName("Basic")
    class Basic {

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"()", "[]", "{}", "()[]{}", "([]{})", "{[()()]}"})
        @DisplayName("accepts balanced brackets")
        void acceptsBalanced(String text) {
            assertThat(Brackets.isBalanced(text)).isTrue();
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"(]", "{)", "[}"})
        @DisplayName("rejects a bracket closed by the wrong type")
        void rejectsWrongType(String text) {
            assertThat(Brackets.isBalanced(text)).isFalse();
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"([)]", "{[}]", "[(])"})
        @DisplayName("rejects brackets closed in the wrong order")
        void rejectsWrongOrder(String text) {
            assertThat(Brackets.isBalanced(text)).isFalse();
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("the empty string is balanced")
        void emptyString() {
            assertThat(Brackets.isBalanced("")).isTrue();
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"hello", "if (a[i] > 0) { return f(x); }", "<a href=\"x\">"})
        @DisplayName("ignores every character that isn't a bracket, including < and >")
        void ignoresOtherCharacters(String text) {
            assertThat(Brackets.isBalanced(text)).isTrue();
        }

        @Test
        @DisplayName("other characters don't hide a closing bracket that comes too early")
        void otherCharactersDontHideMismatch() {
            assertThat(Brackets.isBalanced("a)b(c")).isFalse();
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"(", "(()", "{[]", "(((("})
        @DisplayName("rejects an opening bracket that is never closed")
        void rejectsUnclosed(String text) {
            assertThat(Brackets.isBalanced(text)).isFalse();
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {")", "())", "][", ")("})
        @DisplayName("rejects a closing bracket when nothing is open")
        void rejectsCloserWithNothingOpen(String text) {
            assertThat(Brackets.isBalanced(text)).isFalse();
        }

        @Test
        @DisplayName("handles brackets nested 100,000 levels deep")
        void deepNesting() {
            String balanced = "(".repeat(100_000) + ")".repeat(100_000);
            String oneMissing = "(".repeat(100_000) + ")".repeat(99_999);

            assertThat(Brackets.isBalanced(balanced)).isTrue();
            assertThat(Brackets.isBalanced(oneMissing)).isFalse();
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("rejects null")
        void rejectsNull() {
            assertThatThrownBy(() -> Brackets.isBalanced(null)).isInstanceOf(NullPointerException.class);
        }
    }
}
