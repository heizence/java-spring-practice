package practice.j09.ex02;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Checks an object's fields against the constraint annotations {@link NotBlank}, {@link MaxLength} and
 * {@link Range}: a tiny version of Jakarta Bean Validation, whose {@code @NotBlank} and {@code @Size} you'll use in
 * Spring.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>The validator looks at the fields declared in the object's own class, of any access level, and skips static
 *       fields. That works for classes and for records alike.</li>
 *   <li>{@code @NotBlank}: the value is not {@code null}, not empty and not only whitespace. Message:
 *       {@code "must not be blank"}.</li>
 *   <li>{@code @MaxLength(n)}: the value has at most {@code n} characters. Message:
 *       {@code "must be at most n characters"}, such as {@code "must be at most 20 characters"}.</li>
 *   <li>{@code @Range(min, max)}: {@code min <= value <= max}. Message: {@code "must be between min and max"}, such as
 *       {@code "must be between 13 and 120"}.</li>
 *   <li>{@code @MaxLength} and {@code @Range} accept {@code null}. Add {@code @NotBlank} to forbid it (Jakarta Bean
 *       Validation works the same way).</li>
 *   <li>A field can carry several annotations. Each broken one gives its own violation.</li>
 *   <li>The result is sorted by field name, then by message. It is unmodifiable, and empty for a valid object.</li>
 *   <li>An annotation on the wrong type of field is a programming mistake: {@link IllegalArgumentException} with
 *       the field's name in the message. {@code @NotBlank} and {@code @MaxLength} belong on {@code String} fields,
 *       {@code @Range} on {@code byte}, {@code short}, {@code int}, {@code long} and their wrapper classes. The
 *       check uses the field's declared type, so it also fails when the value is {@code null}.</li>
 *   <li>A {@code null} object throws {@link NullPointerException}.</li>
 * </ul>
 */
public final class Validator {

    private static final Set<Class<?>> INTEGER_TYPES = Set.of(
            byte.class, short.class, int.class, long.class, Byte.class, Short.class, Integer.class, Long.class);

    private static final Comparator<Violation> BY_FIELD_THEN_MESSAGE =
            Comparator.comparing(Violation::field).thenComparing(Violation::message);

    private Validator() {
    }

    public static List<Violation> validate(Object object) {
        Objects.requireNonNull(object, "object");
        List<Violation> violations = new ArrayList<>();
        // The same code path for classes and records: a @Target(FIELD) annotation on a record component ends up on
        // the record's private field, so getDeclaredFields() finds it. (getRecordComponents() would not: the
        // annotation isn't applicable to RECORD_COMPONENT, so it isn't kept there.)
        for (Field field : object.getClass().getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                check(field, object, violations);
            }
        }
        return List.copyOf(violations);
    }

    private static void check(Field field, Object object, List<Violation> violations) {
        NotBlank notBlank = field.getAnnotation(NotBlank.class); // null when the annotation isn't there
        MaxLength maxLength = field.getAnnotation(MaxLength.class);
        Range range = field.getAnnotation(Range.class);
        if (notBlank == null && maxLength == null && range == null) {
            return; // nothing to check, so no reason to open the field
        }

        // Check the declared type before reading the value, so a misplaced annotation fails even for null values.
        if ((notBlank != null || maxLength != null) && field.getType() != String.class) {
            throw misplaced(field, notBlank != null ? "@NotBlank" : "@MaxLength", "String fields");
        }
        if (range != null && !INTEGER_TYPES.contains(field.getType())) {
            throw misplaced(field, "@Range", "byte, short, int and long fields, and their wrappers");
        }

        Object value = read(field, object);
        String name = field.getName();
        if (notBlank != null && (value == null || ((String) value).isBlank())) {
            violations.add(new Violation(name, "must not be blank"));
        }
        // A type pattern is false for null, so null passes @MaxLength and @Range without an extra check.
        if (maxLength != null && value instanceof String text && text.length() > maxLength.value()) {
            violations.add(new Violation(name, "must be at most " + maxLength.value() + " characters"));
        }
        if (range != null && value instanceof Number number
                && (number.longValue() < range.min() || number.longValue() > range.max())) {
            violations.add(new Violation(name, "must be between " + range.min() + " and " + range.max()));
        }
    }

    private static Object read(Field field, Object object) {
        field.setAccessible(true); // private fields of our own classes and records
        try {
            return field.get(object);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot read " + field, e);
        }
    }

    private static IllegalArgumentException misplaced(Field field, String annotation, String allowed) {
        return new IllegalArgumentException(annotation + " on field '" + field.getName() + "' of "
                + field.getDeclaringClass().getName() + ": it belongs on " + allowed + ", not on "
                + field.getType().getSimpleName());
    }
}
