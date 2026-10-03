package practice.testkit.score;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The list of modules and exercises, read from each module's README.md.
 *
 * <p>The README is the single source of truth: a module's title is its first {@code # } heading, and every
 * {@code ### J05-09 · Title} heading is an exercise. The line under the heading starts with the exercise
 * type in bold, for example {@code **Build** · 30 min}. A title ending in ★ marks an optional exercise.
 */
final class Catalog {

    private static final Pattern EXERCISE_HEADING = Pattern.compile("^###\\s+([JS]\\d{2}-\\d{2})\\s*·\\s*(.+?)\\s*$");
    private static final Pattern TYPE_LINE = Pattern.compile("^\\*\\*([A-Za-z ]+)\\*\\*.*");
    private static final String OPTIONAL_MARK = "★";

    enum ExerciseType {
        BUILD, PREDICT, FIX, WRITE_TESTS, UNKNOWN;

        static ExerciseType parse(String text) {
            return switch (text.strip().toLowerCase(Locale.ROOT)) {
                case "build" -> BUILD;
                case "predict" -> PREDICT;
                case "fix" -> FIX;
                case "write tests" -> WRITE_TESTS;
                default -> UNKNOWN;
            };
        }
    }

    record Exercise(String id, String title, ExerciseType type, boolean optional) {
    }

    record Module(String name, Path dir, String gradlePath, String title, List<Exercise> exercises, List<Mutant> mutants) {

        /** {@code j05-collections} → {@code j05}. */
        String shortName() {
            int dash = name.indexOf('-');
            return dash < 0 ? name : name.substring(0, dash);
        }
    }

    private Catalog() {
    }

    static List<Module> load(Path root) {
        List<Module> modules = new ArrayList<>();
        for (String part : List.of("java", "spring")) {
            Path partDir = root.resolve(part);
            if (!Files.isDirectory(partDir)) {
                continue;
            }
            for (Path dir : list(partDir)) {
                boolean exerciseModule = dir.getFileName().toString().matches("[js]\\d{2}-.+");
                if (exerciseModule && Files.isRegularFile(dir.resolve("README.md"))
                        && Files.isRegularFile(dir.resolve("build.gradle.kts"))) {
                    modules.add(readModule(part, dir));
                }
            }
        }
        return modules;
    }

    private static Module readModule(String part, Path dir) {
        List<String> lines = readLines(dir.resolve("README.md"));
        String name = dir.getFileName().toString();
        String title = lines.stream()
                .filter(line -> line.startsWith("# "))
                .findFirst()
                .map(line -> line.substring(2).strip())
                .orElse(name);

        List<Exercise> exercises = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            var heading = EXERCISE_HEADING.matcher(lines.get(i));
            if (!heading.matches()) {
                continue;
            }
            String exerciseTitle = heading.group(2);
            boolean optional = exerciseTitle.endsWith(OPTIONAL_MARK);
            if (optional) {
                exerciseTitle = exerciseTitle.substring(0, exerciseTitle.length() - OPTIONAL_MARK.length()).strip();
            }
            exercises.add(new Exercise(heading.group(1), exerciseTitle, typeAfter(lines, i), optional));
        }
        return new Module(name, dir, ":" + part + ":" + name, title, List.copyOf(exercises), readMutants(dir));
    }

    private static ExerciseType typeAfter(List<String> lines, int headingIndex) {
        for (int i = headingIndex + 1; i < lines.size() && !lines.get(i).startsWith("#"); i++) {
            var type = TYPE_LINE.matcher(lines.get(i).strip());
            if (type.matches()) {
                return ExerciseType.parse(type.group(1));
            }
        }
        return ExerciseType.UNKNOWN;
    }

    private static List<Mutant> readMutants(Path moduleDir) {
        Path mutantsDir = moduleDir.resolve("src/mutants");
        if (!Files.isDirectory(mutantsDir)) {
            return List.of();
        }
        List<Mutant> mutants = new ArrayList<>();
        for (Path dir : list(mutantsDir)) {
            Path file = dir.resolve("mutant.properties");
            if (!Files.isRegularFile(file)) {
                continue;
            }
            var properties = new Properties();
            try (var reader = Files.newBufferedReader(file)) {
                properties.load(reader);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            mutants.add(Mutant.from(dir.getFileName().toString(), properties));
        }
        return List.copyOf(mutants);
    }

    private static List<Path> list(Path dir) {
        try (Stream<Path> entries = Files.list(dir)) {
            return entries.filter(Files::isDirectory).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static List<String> readLines(Path file) {
        try {
            return Files.readAllLines(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
