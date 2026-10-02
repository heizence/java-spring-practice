package practice.testkit.score;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import practice.testkit.score.Catalog.Exercise;
import practice.testkit.score.Catalog.ExerciseType;
import practice.testkit.score.Catalog.Module;

/**
 * Prints the practice score from the results the tests left behind. Run it through {@code ./score},
 * which runs the tests first.
 *
 * <pre>
 * ScoreReport &lt;repo root&gt; [--only j05 | j05-04] [--compile-failed j05-collections,...]
 *             [--mode report | verify-solved | verify-unsolved]
 * </pre>
 *
 * <p>The two verify modes are for maintaining the material (see CLAUDE.md): {@code verify-solved}
 * checks that the reference solutions pass every test and that every mutant is caught;
 * {@code verify-unsolved} checks that every exercise starts out failing.
 */
public final class ScoreReport {

    private static final int WIDTH = 78;
    private static final int MAX_FAILURES_SHOWN = 3;
    private static final List<String> TIER_ORDER = List.of("Basic", "Edge cases", "Errors", "Randomized", "Concurrency");

    enum Mode { REPORT, VERIFY_SOLVED, VERIFY_UNSOLVED }

    enum State { DONE, IN_PROGRESS, NOT_STARTED, NO_RESULTS }

    record Options(Path root, String only, Set<String> compileFailed, Mode mode) {

        static Options parse(String[] args) {
            if (args.length == 0) {
                throw new IllegalArgumentException("usage: ScoreReport <repo root> [--only ...] [--compile-failed ...] [--mode ...]");
            }
            Path root = Path.of(args[0]);
            String only = "";
            Set<String> compileFailed = Set.of();
            Mode mode = Mode.REPORT;
            for (int i = 1; i + 1 < args.length; i += 2) {
                String value = args[i + 1].strip();
                switch (args[i]) {
                    case "--only" -> only = value.toLowerCase(Locale.ROOT);
                    case "--compile-failed" -> compileFailed = Arrays.stream(value.split(","))
                            .map(String::strip)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toUnmodifiableSet());
                    case "--mode" -> mode = value.isEmpty() ? Mode.REPORT
                            : Mode.valueOf(value.toUpperCase(Locale.ROOT).replace('-', '_'));
                    default -> throw new IllegalArgumentException("unknown option " + args[i]);
                }
            }
            return new Options(root, only, compileFailed, mode);
        }

        /** {@code j05} or {@code j05-04} → {@code j05}. */
        String onlyModule() {
            int dash = only.indexOf('-');
            return dash < 0 ? only : only.substring(0, dash);
        }

        /** {@code j05-04} → {@code J05-04}, otherwise empty. */
        String onlyExercise() {
            return only.contains("-") ? only.toUpperCase(Locale.ROOT) : "";
        }
    }

    record MutantResult(Mutant mutant, boolean caught, long testsRun) {
    }

    record ExerciseScore(Exercise exercise, State state, List<ResultRow> rows,
                         List<MutantResult> learnerMutants, List<MutantResult> verificationMutants) {

        long passed() {
            return rows.stream().filter(ResultRow::passed).count();
        }

        long counted() {
            return rows.stream().filter(ResultRow::counted).count();
        }

        /** Failed tests, Basic first, then Edge cases, Errors and so on. */
        List<ResultRow> failures() {
            return rows.stream()
                    .filter(ResultRow::failed)
                    .sorted(java.util.Comparator.comparingInt(row -> tierRank(row.tier())))
                    .toList();
        }

        boolean writesTests() {
            return exercise.type() == ExerciseType.WRITE_TESTS || !learnerMutants.isEmpty();
        }
    }

    record ModuleScore(Module module, boolean compileFailed, boolean ran, List<ExerciseScore> exercises) {

        long done() {
            return exercises.stream().filter(e -> e.state() == State.DONE).count();
        }
    }

    private ScoreReport() {
    }

    public static void main(String[] args) {
        Options options = Options.parse(args);
        List<ModuleScore> scores = Catalog.load(options.root()).stream()
                .filter(module -> options.onlyModule().isEmpty() || module.shortName().equals(options.onlyModule()))
                .map(module -> score(module, options))
                .toList();
        if (scores.isEmpty()) {
            System.out.println("No module matches '" + options.only() + "'.");
            System.exit(1);
        }
        int exitCode = switch (options.mode()) {
            case REPORT -> {
                print(scores, options);
                yield 0;
            }
            case VERIFY_SOLVED -> report(verifySolved(scores), "reference solutions");
            case VERIFY_UNSOLVED -> report(verifyUnsolved(scores), "unsolved exercises");
        };
        System.exit(exitCode);
    }

    // ---------------------------------------------------------------- scoring

    static ModuleScore score(Module module, Options options) {
        boolean compileFailed = options.compileFailed().contains(module.name());
        Path resultsFile = module.dir().resolve("build/score/test.tsv");
        boolean ran = !compileFailed && Files.isRegularFile(resultsFile);
        Map<String, List<ResultRow>> rowsByExercise = ran
                ? readRows(resultsFile).stream().collect(Collectors.groupingBy(ResultRow::exerciseId))
                : Map.of();

        Map<String, List<MutantResult>> mutantsByExercise = new LinkedHashMap<>();
        for (Mutant mutant : module.mutants()) {
            List<ResultRow> rows = ran
                    ? readRows(module.dir().resolve("build/score/mutants/" + mutant.id() + ".tsv"))
                    : List.of();
            var result = new MutantResult(mutant, rows.stream().anyMatch(ResultRow::failed),
                    rows.stream().filter(ResultRow::counted).count());
            mutant.exerciseId().ifPresent(id -> mutantsByExercise.computeIfAbsent(id, key -> new ArrayList<>()).add(result));
        }

        List<ExerciseScore> exercises = module.exercises().stream()
                .filter(exercise -> options.onlyExercise().isEmpty() || exercise.id().equals(options.onlyExercise()))
                .map(exercise -> {
                    List<ResultRow> rows = rowsByExercise.getOrDefault(exercise.id(), List.of());
                    List<MutantResult> mutants = mutantsByExercise.getOrDefault(exercise.id(), List.of());
                    List<MutantResult> learner = mutants.stream()
                            .filter(m -> m.mutant().audience() == Mutant.Audience.LEARNER).toList();
                    List<MutantResult> verification = mutants.stream()
                            .filter(m -> m.mutant().audience() == Mutant.Audience.VERIFICATION).toList();
                    return new ExerciseScore(exercise, stateOf(exercise, ran, rows, learner), rows, learner, verification);
                })
                .toList();
        return new ModuleScore(module, compileFailed, ran, exercises);
    }

    private static State stateOf(Exercise exercise, boolean ran, List<ResultRow> rows, List<MutantResult> learner) {
        if (!ran) {
            return State.NO_RESULTS;
        }
        long counted = rows.stream().filter(ResultRow::counted).count();
        long passed = rows.stream().filter(ResultRow::passed).count();
        if (exercise.type() == ExerciseType.WRITE_TESTS || !learner.isEmpty()) {
            if (counted == 0) {
                return State.NOT_STARTED;
            }
            boolean allCaught = !learner.isEmpty() && learner.stream().allMatch(MutantResult::caught);
            return passed == counted && allCaught ? State.DONE : State.IN_PROGRESS;
        }
        if (counted == 0) {
            return State.NO_RESULTS;
        }
        if (passed == counted) {
            return State.DONE;
        }
        return passed == 0 ? State.NOT_STARTED : State.IN_PROGRESS;
    }

    private static List<ResultRow> readRows(Path file) {
        if (!Files.isRegularFile(file)) {
            return List.of();
        }
        return Catalog.readLines(file).stream()
                .map(ResultRow::parse)
                .flatMap(java.util.Optional::stream)
                .toList();
    }

    // ---------------------------------------------------------------- printing

    private static void print(List<ModuleScore> scores, Options options) {
        boolean singleExercise = !options.onlyExercise().isEmpty();
        System.out.println();
        for (ModuleScore score : scores) {
            String progress = score.done() + "/" + score.exercises().size() + " done";
            System.out.println(padBetween(score.module().title(), progress));
            if (score.compileFailed()) {
                System.out.println("  ⚠ This module doesn't compile, so its tests can't run. To see the errors:");
                System.out.println("      ./gradlew " + score.module().gradlePath() + ":compileTestJava");
            } else if (!score.ran()) {
                System.out.println("  ⚠ No results yet. Run ./score to run the tests.");
            } else {
                for (ExerciseScore exercise : score.exercises()) {
                    printExercise(exercise, singleExercise);
                }
            }
            System.out.println();
        }
        printSummary(scores, options);
    }

    private static void printExercise(ExerciseScore score, boolean showAll) {
        Exercise exercise = score.exercise();
        String symbol = switch (score.state()) {
            case DONE -> "✔";
            case IN_PROGRESS -> "◐";
            case NOT_STARTED -> "○";
            case NO_RESULTS -> "⚠";
        };
        String title = exercise.title() + (exercise.optional() ? " ★" : "");
        String result;
        if (score.writesTests()) {
            long caught = score.learnerMutants().stream().filter(MutantResult::caught).count();
            result = score.counted() == 0
                    ? "no tests yet · " + score.learnerMutants().size() + " bugs to catch"
                    : score.counted() + " tests · caught " + caught + "/" + score.learnerMutants().size() + " bugs";
        } else if (score.state() == State.NO_RESULTS) {
            result = "no test results";
        } else {
            result = score.passed() + "/" + score.counted();
        }
        System.out.println(padBetween("  " + symbol + " " + exercise.id() + "  " + title, result));

        if (score.state() == State.NO_RESULTS && !score.writesTests()) {
            System.out.println("      No tests ran for this exercise. Check that its test class compiles and is named "
                    + "after the exercise (@DisplayName(\"" + exercise.id() + " · ...\")).");
            return;
        }
        if (score.writesTests()) {
            printWriteTestsDetails(score, showAll);
            return;
        }
        if (score.state() == State.IN_PROGRESS || (showAll && score.state() == State.NOT_STARTED)) {
            System.out.println("      " + tierSummary(score.rows()));
            printFailures(score.failures(), showAll);
        }
    }

    private static void printWriteTestsDetails(ExerciseScore score, boolean showAll) {
        if (score.counted() == 0) {
            return;
        }
        List<ResultRow> failures = score.failures();
        if (!failures.isEmpty()) {
            System.out.println("      " + failures.size() + " of your tests fail against the real code. A test must pass on"
                    + " correct code before it can catch bugs:");
            printFailures(failures, showAll);
        }
        for (MutantResult result : score.learnerMutants()) {
            if (result.caught()) {
                System.out.println("      ✔ caught: " + result.mutant().description());
            } else if (!result.mutant().hint().isEmpty()) {
                System.out.println("      ✗ not caught yet · hint: " + result.mutant().hint());
            } else {
                System.out.println("      ✗ not caught yet");
            }
        }
    }

    private static void printFailures(List<ResultRow> failures, boolean showAll) {
        int limit = showAll ? failures.size() : Math.min(MAX_FAILURES_SHOWN, failures.size());
        for (ResultRow failure : failures.subList(0, limit)) {
            System.out.println("      ✗ " + failure.tier() + " › " + failure.testName());
            if (showAll && !failure.message().isBlank()) {
                String message = failure.message();
                System.out.println("          " + (message.length() > 160 ? message.substring(0, 157) + "..." : message));
            }
        }
        if (failures.size() > limit) {
            System.out.println("      … and " + (failures.size() - limit) + " more. Run ./score "
                    + failures.getFirst().exerciseId().toLowerCase(Locale.ROOT) + " to see them all.");
        }
    }

    private static String tierSummary(List<ResultRow> rows) {
        Map<String, List<ResultRow>> byTier = rows.stream()
                .filter(ResultRow::counted)
                .collect(Collectors.groupingBy(ResultRow::tier, LinkedHashMap::new, Collectors.toList()));
        return byTier.entrySet().stream()
                .sorted((a, b) -> Integer.compare(tierRank(a.getKey()), tierRank(b.getKey())))
                .map(entry -> entry.getKey() + " "
                        + entry.getValue().stream().filter(ResultRow::passed).count() + "/" + entry.getValue().size())
                .collect(Collectors.joining(" · "));
    }

    private static int tierRank(String tier) {
        int index = TIER_ORDER.indexOf(tier);
        return index < 0 ? TIER_ORDER.size() : index;
    }

    private static void printSummary(List<ModuleScore> scores, Options options) {
        List<ExerciseScore> all = scores.stream().flatMap(score -> score.exercises().stream()).toList();
        long done = all.stream().filter(e -> e.state() == State.DONE).count();
        System.out.println("─".repeat(WIDTH));
        System.out.printf("Done: %d of %d exercises (%d%%)%n", done, all.size(), all.isEmpty() ? 0 : done * 100 / all.size());
        if (options.onlyExercise().isEmpty()) {
            all.stream()
                    .filter(e -> e.state() != State.DONE && !e.exercise().optional())
                    .findFirst()
                    .ifPresent(next -> System.out.println("Next up: " + next.exercise().id() + " · " + next.exercise().title()));
        }
        System.out.println();
    }

    private static String padBetween(String left, String right) {
        int space = WIDTH - left.length() - right.length();
        if (space < 2) {
            int keep = Math.max(10, WIDTH - right.length() - 3);
            left = left.length() > keep ? left.substring(0, keep - 1) + "…" : left;
            space = Math.max(2, WIDTH - left.length() - right.length());
        }
        return left + " ".repeat(space) + right;
    }

    // ---------------------------------------------------------------- verification (maintainers)

    private static List<String> verifySolved(List<ModuleScore> scores) {
        List<String> problems = new ArrayList<>();
        for (ModuleScore module : scores) {
            if (!module.ran()) {
                problems.add(module.module().name() + ": no results (compile error or tests not run)");
                continue;
            }
            for (ExerciseScore score : module.exercises()) {
                String name = score.exercise().id() + " " + score.exercise().title();
                ExerciseType type = score.exercise().type();
                if (type == ExerciseType.UNKNOWN) {
                    problems.add(name + ": README has no **Build**/**Predict**/**Fix**/**Write tests** line under the heading");
                }
                if (score.state() != State.DONE) {
                    problems.add(name + ": not solved (" + describe(score) + ")");
                }
                for (MutantResult result : score.verificationMutants()) {
                    if (!result.caught()) {
                        problems.add(name + ": verification mutant '" + result.mutant().id() + "' was not caught ("
                                + result.testsRun() + " tests ran)");
                    }
                }
                if ((type == ExerciseType.BUILD || type == ExerciseType.FIX) && score.verificationMutants().size() < 2) {
                    problems.add(name + ": needs at least 2 verification mutants, has " + score.verificationMutants().size());
                }
                if (type == ExerciseType.WRITE_TESTS && score.learnerMutants().size() < 3) {
                    problems.add(name + ": needs at least 3 learner mutants, has " + score.learnerMutants().size());
                }
            }
        }
        return problems;
    }

    private static List<String> verifyUnsolved(List<ModuleScore> scores) {
        List<String> problems = new ArrayList<>();
        for (ModuleScore module : scores) {
            if (!module.ran()) {
                problems.add(module.module().name() + ": no results (compile error or tests not run)");
                continue;
            }
            for (ExerciseScore score : module.exercises()) {
                String name = score.exercise().id() + " " + score.exercise().title();
                if (!score.verificationMutants().isEmpty()) {
                    problems.add(name + ": verification mutants must exist only on the solutions branch");
                }
                if (score.writesTests()) {
                    if (score.learnerMutants().isEmpty()) {
                        problems.add(name + ": a Write tests exercise needs learner mutants in src/mutants");
                    }
                    if (score.counted() > 0) {
                        problems.add(name + ": should start without tests, but " + score.counted() + " ran");
                    }
                } else if (score.state() == State.DONE || score.state() == State.NO_RESULTS) {
                    problems.add(name + ": should start with at least one failing test (" + describe(score) + ")");
                }
            }
        }
        return problems;
    }

    private static String describe(ExerciseScore score) {
        if (score.state() == State.NO_RESULTS) {
            return "no test results";
        }
        String tests = score.passed() + "/" + score.counted() + " tests pass";
        if (score.writesTests()) {
            long caught = score.learnerMutants().stream().filter(MutantResult::caught).count();
            return tests + ", caught " + caught + "/" + score.learnerMutants().size() + " learner mutants";
        }
        List<ResultRow> failures = score.failures();
        return failures.isEmpty() ? tests : tests + "; first failure: " + failures.getFirst().testName()
                + " — " + failures.getFirst().message();
    }

    private static int report(List<String> problems, String what) {
        if (problems.isEmpty()) {
            System.out.println("✔ All checks passed for the " + what + ".");
            return 0;
        }
        System.out.println("✗ " + problems.size() + " problem(s) with the " + what + ":");
        problems.forEach(problem -> System.out.println("  - " + problem));
        return 1;
    }
}
