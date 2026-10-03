package practice.testkit.score;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Maps between exercise IDs and packages: exercise {@code J05-09} lives in package {@code practice.j05.ex09}
 * (and {@code S03-02} in {@code practice.s03.ex02}).
 */
final class ExerciseIds {

    private static final Pattern PACKAGE = Pattern.compile("practice\\.([js])(\\d{2})\\.ex(\\d{2})(?:\\..*)?");
    private static final Pattern TITLE_PREFIX = Pattern.compile("^([JS]\\d{2}-\\d{2})\\s*·\\s*");

    private ExerciseIds() {
    }

    /** {@code "S04-02 · Create a book"} → {@code S04-02}. */
    static Optional<String> fromDisplayName(String displayName) {
        var matcher = TITLE_PREFIX.matcher(displayName);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    /** {@code practice.j05.ex09.LruCacheTest$Basic} → {@code J05-09}. */
    static Optional<String> fromClassName(String className) {
        int lastDot = className.lastIndexOf('.');
        if (lastDot < 0) {
            return Optional.empty();
        }
        var matcher = PACKAGE.matcher(className.substring(0, lastDot));
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(matcher.group(1).toUpperCase(Locale.ROOT) + matcher.group(2) + "-" + matcher.group(3));
    }

    /** {@code J05-09} → {@code practice.j05.ex09}. */
    static String toPackage(String exerciseId) {
        String lower = exerciseId.toLowerCase(Locale.ROOT);
        return "practice." + lower.substring(0, 3) + ".ex" + lower.substring(4, 6);
    }

    /** {@code "J05-09 · LRU cache"} → {@code "LRU cache"}. */
    static String stripPrefix(String displayName) {
        return TITLE_PREFIX.matcher(displayName).replaceFirst("");
    }
}
