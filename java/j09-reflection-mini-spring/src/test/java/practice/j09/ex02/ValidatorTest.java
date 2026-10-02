package practice.j09.ex02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import practice.j09.ex02.Samples.MaxLengthOnList;
import practice.j09.ex02.Samples.NoFields;
import practice.j09.ex02.Samples.NotBlankOnNumber;
import practice.j09.ex02.Samples.Product;
import practice.j09.ex02.Samples.RangeOnText;
import practice.j09.ex02.Samples.Shipment;
import practice.j09.ex02.Samples.SignUp;
import practice.j09.ex02.Samples.Sizes;

@DisplayName("J09-02 · Custom annotations")
class ValidatorTest {

    private static final String VALID_EMAIL = "mina@example.com";

    @Nested
    @DisplayName("Basic")
    class Basic {

        static Stream<Named<Class<? extends Annotation>>> annotations() {
            return Stream.of(
                    Named.of("@NotBlank", NotBlank.class),
                    Named.of("@MaxLength", MaxLength.class),
                    Named.of("@Range", Range.class));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("annotations")
        @DisplayName("each annotation is kept at runtime, so reflection can see it")
        void retainedAtRuntime(Class<? extends Annotation> annotation) {
            Retention retention = annotation.getAnnotation(Retention.class);

            assertThat(retention).as("@Retention on @%s", annotation.getSimpleName()).isNotNull();
            assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("annotations")
        @DisplayName("each annotation is allowed on fields only")
        void targetsFields(Class<? extends Annotation> annotation) {
            Target target = annotation.getAnnotation(Target.class);

            assertThat(target).as("@Target on @%s", annotation.getSimpleName()).isNotNull();
            assertThat(target.value()).containsExactly(ElementType.FIELD);
        }

        @Test
        @DisplayName("an object that follows every rule has no violations")
        void validObject() {
            assertThat(Validator.validate(new SignUp("mina", VALID_EMAIL, 30))).isEmpty();
        }

        @Test
        @DisplayName("@NotBlank reports a blank string")
        void notBlank() {
            assertThat(Validator.validate(new SignUp("mina", "", 30)))
                    .containsExactly(new Violation("email", "must not be blank"));
        }

        @Test
        @DisplayName("@MaxLength reports a string that is too long")
        void maxLength() {
            assertThat(Validator.validate(new SignUp("a".repeat(13), VALID_EMAIL, 30)))
                    .containsExactly(new Violation("username", "must be at most 12 characters"));
        }

        @Test
        @DisplayName("@Range reports a number outside the range")
        void range() {
            assertThat(Validator.validate(new SignUp("mina", VALID_EMAIL, 200)))
                    .containsExactly(new Violation("age", "must be between 13 and 120"));
        }

        @Test
        @DisplayName("validates the private fields of a plain class")
        void plainClass() {
            assertThat(Validator.validate(new Product("", "LAMP-01", 5L, "fragile")))
                    .containsExactly(new Violation("name", "must not be blank"));
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @ParameterizedTest(name = "{0}")
        @NullSource
        @ValueSource(strings = {"", "   ", "\t\n"})
        @DisplayName("@NotBlank reports null, empty and whitespace-only strings")
        void notBlankVariants(String email) {
            assertThat(Validator.validate(new SignUp("mina", email, 30)))
                    .containsExactly(new Violation("email", "must not be blank"));
        }

        @Test
        @DisplayName("@MaxLength accepts exactly the maximum length")
        void maxLengthBoundary() {
            assertThat(Validator.validate(new SignUp("a".repeat(12), VALID_EMAIL, 30))).isEmpty();
        }

        @ParameterizedTest(name = "age {0}")
        @ValueSource(ints = {13, 120})
        @DisplayName("@Range accepts both ends of the range")
        void rangeBoundariesInside(int age) {
            assertThat(Validator.validate(new SignUp("mina", VALID_EMAIL, age))).isEmpty();
        }

        @ParameterizedTest(name = "age {0}")
        @ValueSource(ints = {12, 121, Integer.MIN_VALUE})
        @DisplayName("@Range rejects values just outside the range")
        void rangeBoundariesOutside(int age) {
            assertThat(Validator.validate(new SignUp("mina", VALID_EMAIL, age)))
                    .containsExactly(new Violation("age", "must be between 13 and 120"));
        }

        @Test
        @DisplayName("@MaxLength and @Range accept null")
        void nullPassesMaxLengthAndRange() {
            assertThat(Validator.validate(new Product("Lamp", null, null, null))).isEmpty();
        }

        @Test
        @DisplayName("@Range(min = 0) accepts the largest long, because max has a default")
        void rangeDefaultMax() {
            assertThat(Validator.validate(new Product("Lamp", "LAMP-01", Long.MAX_VALUE, null))).isEmpty();
        }

        @Test
        @DisplayName("@Range(min = 0) reports a negative value, with the default max in the message")
        void rangeDefaultMaxInMessage() {
            assertThat(Validator.validate(new Product("Lamp", "LAMP-01", -1L, null)))
                    .containsExactly(new Violation("stock", "must be between 0 and 9223372036854775807"));
        }

        @Test
        @DisplayName("@Range works on byte, short, Integer and long fields")
        void rangeOnIntegerTypes() {
            var sizes = new Sizes((byte) 0, (short) 11, 5, 10L);

            assertThat(Validator.validate(sizes)).containsExactly(
                    new Violation("small", "must be between 1 and 10"),
                    new Violation("tiny", "must be between 1 and 10"));
        }

        @Test
        @DisplayName("reports every broken rule, sorted by field name and then by message")
        void sortedViolations() {
            var shipment = new Shipment("", "     ", 51);

            assertThat(Validator.validate(shipment)).containsExactly(
                    new Violation("code", "must be at most 3 characters"),
                    new Violation("code", "must not be blank"),
                    new Violation("weightKg", "must be between 1 and 50"),
                    new Violation("zone", "must not be blank"));
        }

        @Test
        @DisplayName("ignores static fields and fields without annotations")
        void ignoresStaticAndUnannotatedFields() {
            assertThat(Validator.validate(new Product("Lamp", "LAMP-01", 5L, ""))).isEmpty();
        }

        @Test
        @DisplayName("an object without fields has no violations")
        void noFields() {
            assertThat(Validator.validate(new NoFields())).isEmpty();
        }

        @Test
        @DisplayName("the returned list can't be modified")
        void resultIsUnmodifiable() {
            List<Violation> violations = Validator.validate(new SignUp("mina", "", 30));

            assertThatThrownBy(() -> violations.add(new Violation("x", "y")))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("rejects null")
        void rejectsNull() {
            assertThatThrownBy(() -> Validator.validate(null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("@Range on a String field is a programming mistake")
        void rangeOnString() {
            assertThatThrownBy(() -> Validator.validate(new RangeOnText("high")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("level");
        }

        @Test
        @DisplayName("@NotBlank on a number field is a programming mistake")
        void notBlankOnNumber() {
            assertThatThrownBy(() -> Validator.validate(new NotBlankOnNumber(5)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("count");
        }

        @Test
        @DisplayName("@MaxLength on a List field is a programming mistake")
        void maxLengthOnList() {
            assertThatThrownBy(() -> Validator.validate(new MaxLengthOnList(List.of("a"))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("tags");
        }

        @Test
        @DisplayName("a misplaced annotation is reported even when the value is null")
        void misplacedAnnotationWithNull() {
            assertThatThrownBy(() -> Validator.validate(new RangeOnText(null)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
