package practice.testkit.score;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

/**
 * Records every exercise test result in a tab-separated file that {@link ScoreReport} turns into the
 * score printed by {@code ./score}.
 *
 * <p>Gradle passes the target file in the {@code practice.score.file} system property, and the mutant
 * being tested (if any) in {@code practice.mutant}. Without a target file the listener does nothing, so
 * running tests directly from an IDE is unaffected.
 *
 * <p>JUnit finds this class through {@code META-INF/services}; no test needs to mention it.
 */
public final class ScoreListener implements TestExecutionListener {

    private final Path file = Optional.ofNullable(System.getProperty("practice.score.file"))
            .map(Path::of)
            .orElse(null);
    private final String mutant = System.getProperty("practice.mutant", ResultRow.NO_MUTANT);

    private TestPlan plan;
    private BufferedWriter out;

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        if (file == null) {
            return;
        }
        plan = testPlan;
        try {
            Files.createDirectories(file.getParent());
            out = Files.newBufferedWriter(file, StandardCharsets.UTF_8); // replaces the previous run's results
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void executionSkipped(TestIdentifier id, String reason) {
        if (id.isTest()) {
            record(id, ResultRow.Status.SKIPPED, reason);
        }
    }

    @Override
    public void executionFinished(TestIdentifier id, TestExecutionResult result) {
        var status = switch (result.getStatus()) {
            case SUCCESSFUL -> ResultRow.Status.PASSED;
            case FAILED -> ResultRow.Status.FAILED;
            case ABORTED -> ResultRow.Status.SKIPPED;
        };
        String message = result.getThrowable().map(ScoreListener::summarize).orElse("");
        if (id.isTest()) {
            record(id, status, message);
        } else if (status == ResultRow.Status.FAILED && id.getSource().orElse(null) instanceof ClassSource) {
            // A test class that cannot start (for example, @BeforeAll throws) reports no tests at all.
            // Record the class itself so the exercise doesn't look untouched.
            record(id, status, message);
        }
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        if (out == null) {
            return;
        }
        try {
            out.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private synchronized void record(TestIdentifier id, ResultRow.Status status, String message) {
        if (out == null) {
            return;
        }
        describe(id).ifPresent(description -> {
            var row = new ResultRow(mutant, description.exerciseId(), description.exerciseTitle(),
                    description.tier(), description.testName(), status, message == null ? "" : message);
            try {
                out.write(row.toLine());
                out.newLine();
                out.flush();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private record Description(String exerciseId, String exerciseTitle, String tier, String testName) {
    }

    /**
     * Works out which exercise, tier and test an identifier belongs to by walking up to its test classes:
     * the outermost class is the exercise ({@code @DisplayName("J05-09 · LRU cache")}), the innermost
     * {@code @Nested} class is the tier ({@code @DisplayName("Edge cases")}).
     */
    private Optional<Description> describe(TestIdentifier id) {
        List<TestIdentifier> classes = new ArrayList<>();
        TestIdentifier methodContainer = null;
        for (TestIdentifier current = id; current != null; current = plan.getParent(current).orElse(null)) {
            var source = current.getSource().orElse(null);
            if (source instanceof ClassSource) {
                classes.add(current);
            } else if (source instanceof MethodSource && current != id && current.isContainer() && methodContainer == null) {
                methodContainer = current; // the parameterized/repeated test this invocation belongs to
            }
        }
        if (classes.isEmpty()) {
            return Optional.empty();
        }
        TestIdentifier outermost = classes.getLast();
        String className = ((ClassSource) outermost.getSource().orElseThrow()).getClassName();
        // Part 2 tests live in the application's own packages, so the display name ("S04-02 · …") names the
        // exercise. Part 1 tests can also be identified by their package (practice.j05.ex09).
        Optional<String> exerciseId = ExerciseIds.fromDisplayName(outermost.getDisplayName())
                .or(() -> ExerciseIds.fromClassName(className));
        if (exerciseId.isEmpty() && !className.startsWith("practice.") && !className.startsWith("com.example.")) {
            return Optional.empty();
        }

        String title = ExerciseIds.stripPrefix(outermost.getDisplayName());
        String tier = classes.size() > 1 ? classes.getFirst().getDisplayName() : "Tests";
        String testName;
        if (id.isContainer()) {
            testName = "(could not start)";
        } else if (methodContainer != null) {
            testName = methodContainer.getDisplayName() + " [" + id.getDisplayName() + "]";
        } else {
            testName = id.getDisplayName();
        }
        // Tests that belong to no exercise (examples, template checks) are recorded too, so maintainers' checks can
        // notice when one of them fails. The score report doesn't count them.
        return Optional.of(new Description(exerciseId.orElse(ResultRow.NO_EXERCISE), title, tier, testName));
    }

    /** The failure message on one line, e.g. "Expecting actual: 3 to be equal to: 4". */
    private static String summarize(Throwable throwable) {
        String message = throwable.getMessage();
        String text;
        if (message == null || message.isBlank()) {
            text = throwable.getClass().getSimpleName();
        } else if (throwable instanceof AssertionError) {
            text = message;
        } else {
            text = throwable.getClass().getSimpleName() + ": " + message;
        }
        return text.strip().replaceAll("\\s+", " ");
    }
}
