package practice.testkit.score;

import java.util.Optional;

/**
 * One test result, as stored in a score file (one tab-separated line per test).
 *
 * @param mutant        the mutant the test ran against, or {@value #NO_MUTANT} for the real code
 * @param exerciseId    for example {@code J05-09}
 * @param exerciseTitle for example {@code LRU cache}
 * @param tier          the {@code @Nested} group, for example {@code Edge cases}
 */
record ResultRow(String mutant, String exerciseId, String exerciseTitle, String tier, String testName,
                 Status status, String message) {

    static final String NO_MUTANT = "-";
    /** The exercise id of tests that belong to no exercise. */
    static final String NO_EXERCISE = "-";

    enum Status { PASSED, FAILED, SKIPPED }

    String toLine() {
        return String.join("\t", clean(mutant), clean(exerciseId), clean(exerciseTitle), clean(tier),
                clean(testName), status.name(), clean(message));
    }

    static Optional<ResultRow> parse(String line) {
        String[] parts = line.split("\t", -1);
        if (parts.length != 7) {
            return Optional.empty();
        }
        try {
            return Optional.of(new ResultRow(parts[0], parts[1], parts[2], parts[3], parts[4],
                    Status.valueOf(parts[5]), parts[6]));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    boolean counted() {
        return status != Status.SKIPPED;
    }

    boolean passed() {
        return status == Status.PASSED;
    }

    boolean failed() {
        return status == Status.FAILED;
    }

    private static String clean(String value) {
        String text = value.replace('\t', ' ').replace('\r', ' ').replace('\n', ' ');
        return text.length() > 300 ? text.substring(0, 297) + "..." : text;
    }
}
