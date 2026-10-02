package practice.j09.ex02;

import java.util.List;

/** Annotated classes and records for {@link ValidatorTest}. */
final class Samples {

    private Samples() {
    }

    /** A record: the annotations sit on the record components, and the compiler copies them to the fields. */
    public record SignUp(
            @NotBlank @MaxLength(12) String username,
            @NotBlank String email,
            @Range(min = 13, max = 120) int age) {
    }

    /** A plain class with private fields. */
    public static class Product {

        @NotBlank
        static String defaultName = ""; // static: never validated, although it is blank

        @NotBlank
        private final String name;

        @MaxLength(8)
        private final String sku;

        @Range(min = 0)
        private final Long stock;

        private final String note; // no annotation: never validated

        public Product(String name, String sku, Long stock, String note) {
            this.name = name;
            this.sku = sku;
            this.stock = stock;
            this.note = note;
        }
    }

    /** Declared out of alphabetical order, with two annotations on {@code code}. */
    public record Shipment(
            @NotBlank String zone,
            @NotBlank @MaxLength(3) String code,
            @Range(min = 1, max = 50) int weightKg) {
    }

    /** Every integer type that {@code @Range} accepts. */
    public record Sizes(
            @Range(min = 1, max = 10) byte tiny,
            @Range(min = 1, max = 10) short small,
            @Range(min = 1, max = 10) Integer boxed,
            @Range(min = 1, max = 10) long large) {
    }

    public record NoFields() {
    }

    /** {@code @Range} doesn't belong on a String. */
    public record RangeOnText(@Range(min = 0, max = 1) String level) {
    }

    /** {@code @NotBlank} doesn't belong on a number. */
    public record NotBlankOnNumber(@NotBlank Integer count) {
    }

    /** {@code @MaxLength} doesn't belong on a list. */
    public record MaxLengthOnList(@MaxLength(3) List<String> tags) {
    }
}
