package practice.testkit.score;

import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

/**
 * A deliberately broken copy of exercise code, described by {@code src/mutants/<id>/mutant.properties}:
 *
 * <pre>
 * package=practice.j03.ex03
 * audience=learner
 * description=Rejects passwords of exactly 8 characters.
 * hint=Test the exact boundary lengths.
 * </pre>
 *
 * <ul>
 *   <li>{@code audience=learner}: part of a "Write tests" exercise; your tests should catch it.
 *       {@code audience=verification}: checks that an exercise's own tests are strong enough
 *       (these exist only on the {@code solutions} branch).</li>
 *   <li>{@code description} is revealed once the mutant is caught; {@code hint} is shown until then.</li>
 * </ul>
 */
record Mutant(String id, String packageName, Audience audience, String description, String hint) {

    enum Audience { LEARNER, VERIFICATION }

    static Mutant from(String id, Properties properties) {
        String packageName = properties.getProperty("package", "");
        Audience audience = "learner".equals(properties.getProperty("audience", "").strip().toLowerCase(Locale.ROOT))
                ? Audience.LEARNER
                : Audience.VERIFICATION;
        return new Mutant(id, packageName, audience,
                properties.getProperty("description", id).strip(),
                properties.getProperty("hint", "").strip());
    }

    /** The exercise this mutant belongs to, derived from its package. */
    Optional<String> exerciseId() {
        return ExerciseIds.fromClassName(packageName + ".Mutant");
    }
}
