package practice.testkit.score;

import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

/**
 * A deliberately broken copy of exercise code, described by {@code src/mutants/<id>/mutant.properties}.
 *
 * <p>Part 1 (tests in package {@code practice.jNN.exMM}):
 * <pre>
 * package=practice.j03.ex03
 * audience=learner
 * description=Rejects passwords of exactly 8 characters.
 * hint=Test the exact boundary lengths.
 * </pre>
 *
 * <p>Part 2 (tests in the application's packages): name the exercise and the tests to run instead of a package.
 * <pre>
 * exercise=S06-05
 * tests=com.example.bookstore.order.OrderListApiTest
 * audience=verification
 * description=Loads order lines one order at a time (N+1 queries).
 * </pre>
 *
 * <ul>
 *   <li>{@code audience=learner}: part of a "Write tests" exercise; your tests should catch it.
 *       {@code audience=verification}: checks that an exercise's own tests are strong enough
 *       (these exist only on the {@code solutions} branch).</li>
 *   <li>{@code tests} is a comma-separated list of Gradle test filter patterns
 *       (default: every test in {@code package}).</li>
 *   <li>{@code description} is revealed once the mutant is caught; {@code hint} is shown until then.</li>
 * </ul>
 */
record Mutant(String id, String packageName, String exercise, Audience audience, String description, String hint) {

    enum Audience { LEARNER, VERIFICATION }

    static Mutant from(String id, Properties properties) {
        Audience audience = "learner".equals(properties.getProperty("audience", "").strip().toLowerCase(Locale.ROOT))
                ? Audience.LEARNER
                : Audience.VERIFICATION;
        return new Mutant(id,
                properties.getProperty("package", "").strip(),
                properties.getProperty("exercise", "").strip(),
                audience,
                properties.getProperty("description", id).strip(),
                properties.getProperty("hint", "").strip());
    }

    /** The exercise this mutant belongs to: the {@code exercise} property, or derived from the package. */
    Optional<String> exerciseId() {
        if (!exercise.isEmpty()) {
            return Optional.of(exercise);
        }
        return ExerciseIds.fromClassName(packageName + ".Mutant");
    }
}
