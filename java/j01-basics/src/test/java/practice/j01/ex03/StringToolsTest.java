package practice.j01.ex03;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("J01-03 · Strings")
class StringToolsTest {

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("isPalindrome() recognizes a sentence that reads the same backwards")
        void palindromeSentence() {
            assertThat(StringTools.isPalindrome("A man, a plan, a canal: Panama!")).isTrue();
        }

        @Test
        @DisplayName("isPalindrome() is false for a word that is not a palindrome")
        void notAPalindrome() {
            assertThat(StringTools.isPalindrome("Java")).isFalse();
        }

        @Test
        @DisplayName("capitalizeWords() makes the first letter of every word uppercase")
        void capitalizesWords() {
            assertThat(StringTools.capitalizeWords("hello world")).isEqualTo("Hello World");
        }

        @Test
        @DisplayName("maskEmail() keeps the first character, three asterisks and the domain")
        void masksEmail() {
            assertThat(StringTools.maskEmail("minji.kim@example.com")).isEqualTo("m***@example.com");
        }

        @Test
        @DisplayName("receiptLine() lays out name, quantity and total in fixed columns")
        void receiptLine() {
            //                                                         |- 20 characters --| |3| |-- 10 --|
            assertThat(StringTools.receiptLine("Americano", 2, 450)).isEqualTo("Americano              2       9.00");
        }

        @Test
        @DisplayName("orderConfirmation() returns the four lines of the email")
        void orderConfirmation() {
            assertThat(StringTools.orderConfirmation("Minji", "A-1001")).isEqualTo(
                    "Hi Minji,\n\nthank you for your order A-1001.\nWe'll email you again when it ships.\n");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"racecar", "No 'x' in Nixon", "Was it a car or a cat I saw?", "기러기",
                "다시 합창합시다", "12321", "x"})
        @DisplayName("isPalindrome() is true for these palindromes")
        void palindromes(String text) {
            assertThat(StringTools.isPalindrome(text)).isTrue();
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"ab", "palindrome", "1a2", "가나"})
        @DisplayName("isPalindrome() is false for these, because letters and digits both count")
        void notPalindromes(String text) {
            assertThat(StringTools.isPalindrome(text)).isFalse();
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"", "   ", "?!"})
        @DisplayName("isPalindrome() is true for text without letters or digits")
        void noLettersOrDigits(String text) {
            assertThat(StringTools.isPalindrome(text)).isTrue();
        }

        @Test
        @DisplayName("capitalizeWords() keeps double spaces, tabs, line breaks and trailing spaces")
        void keepsWhitespace() {
            assertThat(StringTools.capitalizeWords("  two  spaces\tand\nlines "))
                    .isEqualTo("  Two  Spaces\tAnd\nLines ");
        }

        @Test
        @DisplayName("capitalizeWords() leaves the rest of each word unchanged")
        void keepsRestOfWord() {
            assertThat(StringTools.capitalizeWords("the API is ready")).isEqualTo("The API Is Ready");
        }

        @Test
        @DisplayName("capitalizeWords() leaves Korean and digits unchanged")
        void caselessCharacters() {
            assertThat(StringTools.capitalizeWords("안녕 세상 2nd try")).isEqualTo("안녕 세상 2nd Try");
        }

        @Test
        @DisplayName("capitalizeWords() returns an empty string for an empty string")
        void capitalizeEmpty() {
            assertThat(StringTools.capitalizeWords("")).isEmpty();
        }

        @Test
        @DisplayName("maskEmail() writes three asterisks even for a one-character name")
        void maskShortName() {
            assertThat(StringTools.maskEmail("a@example.com")).isEqualTo("a***@example.com");
        }

        @Test
        @DisplayName("maskEmail() keeps the domain exactly as it is")
        void maskKeepsDomain() {
            assertThat(StringTools.maskEmail("Minji@Example.CO.KR")).isEqualTo("M***@Example.CO.KR");
        }

        @Test
        @DisplayName("receiptLine() pads cents below 10 with a zero")
        void receiptSmallCents() {
            assertThat(StringTools.receiptLine("Gum", 1, 5)).isEqualTo("Gum                    1       0.05");
        }

        @Test
        @DisplayName("receiptLine() shows a free item as 0.00")
        void receiptFreeItem() {
            assertThat(StringTools.receiptLine("Bookmark", 3, 0)).isEqualTo("Bookmark               3       0.00");
        }

        @Test
        @DisplayName("receiptLine() keeps a name of exactly 20 characters")
        void receiptNameOfTwenty() {
            assertThat(StringTools.receiptLine("Refactoring (2nd ed)", 1, 4500))
                    .isEqualTo("Refactoring (2nd ed)   1      45.00");
        }

        @Test
        @DisplayName("receiptLine() cuts a 21-character name to 17 characters plus ...")
        void receiptNameOfTwentyOne() {
            assertThat(StringTools.receiptLine("Refactoring (2nd ed.)", 1, 4500))
                    .isEqualTo("Refactoring (2nd ...   1      45.00");
        }

        @Test
        @DisplayName("receiptLine() accepts the largest quantity, 999")
        void receiptLargestQuantity() {
            assertThat(StringTools.receiptLine("Pencil", 999, 120)).isEqualTo("Pencil               999    1198.80");
        }

        @Test
        @DisplayName("receiptLine() lets the total column grow for a total wider than 10 characters")
        void receiptWideTotal() {
            assertThat(StringTools.receiptLine("Rare book", 1, 1_234_567_890))
                    .isEqualTo("Rare book              1 12345678.90");
        }

        @Test
        @DisplayName("orderConfirmation() ends every line with \\n, including the last one")
        void confirmationLineEnds() {
            String text = StringTools.orderConfirmation("Minji", "A-1001");

            assertThat(text).endsWith("ships.\n").doesNotContain("\r");
        }

        @Test
        @DisplayName("orderConfirmation() starts no line with spaces")
        void confirmationNoIndentation() {
            String text = StringTools.orderConfirmation("Minji", "A-1001");

            assertThat(text.lines()).noneMatch(line -> line.startsWith(" "));
        }

        @Test
        @DisplayName("orderConfirmation() inserts a name containing % exactly as it is")
        void confirmationWithPercent() {
            assertThat(StringTools.orderConfirmation("100% Books", "B-7")).startsWith("Hi 100% Books,\n");
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("every method rejects null with NullPointerException")
        void rejectsNull() {
            assertThatThrownBy(() -> StringTools.isPalindrome(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> StringTools.capitalizeWords(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> StringTools.maskEmail(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> StringTools.receiptLine(null, 1, 100)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> StringTools.orderConfirmation(null, "A-1"))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> StringTools.orderConfirmation("Minji", null))
                    .isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"minji.example.com", "@example.com", "minji@", "minji@home@example.com", "@", ""})
        @DisplayName("maskEmail() rejects text without exactly one @ that has text on both sides")
        void rejectsInvalidEmail(String email) {
            assertThatThrownBy(() -> StringTools.maskEmail(email)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("maskEmail() doesn't put the address into the exception message")
        void keepsAddressOutOfMessage() {
            assertThatThrownBy(() -> StringTools.maskEmail("minji@home@example.com"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageNotContaining("minji")
                    .hasMessageNotContaining("example.com");
        }

        @ParameterizedTest(name = "quantity {0}")
        @ValueSource(ints = {0, -1, 1000, Integer.MIN_VALUE})
        @DisplayName("receiptLine() rejects a quantity outside 1 to 999")
        void rejectsInvalidQuantity(int quantity) {
            assertThatThrownBy(() -> StringTools.receiptLine("Americano", quantity, 450))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("receiptLine() rejects a negative price")
        void rejectsNegativePrice() {
            assertThatThrownBy(() -> StringTools.receiptLine("Refund", 1, -450))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("receiptLine() throws ArithmeticException when the total doesn't fit in a long")
        void rejectsOverflowingTotal() {
            assertThatThrownBy(() -> StringTools.receiptLine("Gold bar", 2, Long.MAX_VALUE))
                    .isInstanceOf(ArithmeticException.class);
        }
    }
}
