package com.example.bookstore;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * The module's {@code Dockerfile}, read as text: these tests check its key properties without building an image.
 * Each test reads the instructions in order, with continued lines ({@code \}) joined and comments left out.
 */
@DisplayName("S14-03 · Graceful shutdown and packaging")
class DockerfileTest {

    /** The four layers of a Spring Boot jar, from the one that changes least to the one that changes most. */
    private static final List<String> LAYERS =
            List.of("dependencies", "spring-boot-loader", "snapshot-dependencies", "application");

    private List<Instruction> instructions;

    @BeforeEach
    void readDockerfile() throws IOException {
        instructions = Instruction.parse(dockerfile());
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("builds in two stages: a named stage unpacks the jar, the last stage copies from it")
        void hasTwoStages() {
            List<Instruction> froms = instructions.stream().filter(i -> i.is("FROM")).toList();

            assertThat(froms).as("FROM instructions").hasSizeGreaterThanOrEqualTo(2);
            String stageName = stageName(froms.getFirst());
            assertThat(stageName).as("the name of the first stage (FROM … AS <name>)").isNotEmpty();
            assertThat(finalStage()).as("COPY --from=%s in the last stage", stageName)
                    .anyMatch(i -> i.is("COPY") && i.arguments().contains("--from=" + stageName));
        }

        @Test
        @DisplayName("runs on a Java 25 JRE, not on a JDK")
        void runsOnAJre() {
            String image = finalStage().getFirst().arguments().split("\\s+")[0].toLowerCase(Locale.ROOT);

            assertThat(image).contains("25").contains("jre").doesNotContain("jdk");
        }

        @Test
        @DisplayName("runs the application as an ordinary user, not as root")
        void runsAsANonRootUser() {
            List<Instruction> stage = finalStage();
            int user = lastIndexOf(stage, "USER");
            int entrypoint = lastIndexOf(stage, "ENTRYPOINT");

            assertThat(user).as("a USER instruction in the last stage").isNotNegative();
            assertThat(user).as("USER comes before ENTRYPOINT").isLessThan(entrypoint);
            String name = stage.get(user).arguments().split(":")[0].strip();
            assertThat(name).isNotIn("root", "0");
        }

        @Test
        @DisplayName("copies the jar's four layers one by one, the one that changes least first")
        void copiesTheLayersInOrder() {
            List<Instruction> copies = finalStage().stream()
                    .filter(i -> i.is("COPY") && i.arguments().contains("--from="))
                    .toList();
            List<Integer> positions = new ArrayList<>();
            for (String layer : LAYERS) {
                // "dependencies/" as a whole path segment, so that "snapshot-dependencies/" doesn't count for it
                Pattern source = Pattern.compile("(?<![\\w-])" + Pattern.quote(layer) + "/");
                int position = indexOfFirst(copies, copy -> source.matcher(copy.arguments()).find());
                assertThat(position).as("a COPY --from=… of the layer %s", layer).isNotNegative();
                positions.add(position);
            }

            assertThat(positions).as("the order of the layer copies").isSorted().doesNotHaveDuplicates();
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("starts java directly in exec form, so that java gets the stop signal and shuts down gracefully")
        void startsJavaInExecForm() {
            List<Instruction> stage = finalStage();
            int entrypoint = lastIndexOf(stage, "ENTRYPOINT");

            assertThat(entrypoint).as("an ENTRYPOINT in the last stage").isNotNegative();
            assertThat(stage.get(entrypoint).arguments()).matches("\\[\\s*\"java\".*\\]");
        }

        @Test
        @DisplayName("lets the heap grow with the container's memory: -XX:MaxRAMPercentage")
        void sizesTheHeapFromTheContainer() {
            assertThat(finalStage())
                    .anyMatch(i -> (i.is("ENTRYPOINT") || i.is("ENV") || i.is("CMD"))
                            && i.arguments().contains("-XX:MaxRAMPercentage="));
        }

        @Test
        @DisplayName("unpacks the jar with Spring Boot's tools: java -Djarmode=tools -jar … extract --layers")
        void unpacksWithTheJarTools() {
            assertThat(instructions)
                    .anyMatch(i -> i.is("RUN") && i.arguments().contains("-Djarmode=tools")
                            && i.arguments().contains("extract") && i.arguments().contains("--layers"));
        }

        @Test
        @DisplayName("documents the port the application listens on: EXPOSE 8080")
        void exposesThePort() {
            assertThat(finalStage()).anyMatch(i -> i.is("EXPOSE") && i.arguments().matches("8080(/tcp)?"));
        }
    }

    /** The instructions of the last stage, starting with its FROM. */
    private List<Instruction> finalStage() {
        int lastFrom = lastIndexOf(instructions, "FROM");
        assertThat(lastFrom).as("a FROM instruction").isNotNegative();
        return instructions.subList(lastFrom, instructions.size());
    }

    /** {@code FROM eclipse-temurin:25-jre AS extract} → {@code extract}; empty without a name. */
    private static String stageName(Instruction from) {
        String[] words = from.arguments().split("\\s+");
        return words.length == 3 && words[1].equalsIgnoreCase("AS") ? words[2] : "";
    }

    private static int lastIndexOf(List<Instruction> instructions, String keyword) {
        for (int i = instructions.size() - 1; i >= 0; i--) {
            if (instructions.get(i).is(keyword)) {
                return i;
            }
        }
        return -1;
    }

    private static int indexOfFirst(List<Instruction> instructions, Predicate<Instruction> test) {
        for (int i = 0; i < instructions.size(); i++) {
            if (test.test(instructions.get(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Gradle runs the tests in the module's folder. An IDE may run them from the repository root, so try that path
     * too.
     */
    private static Path dockerfile() {
        Path inModule = Path.of("Dockerfile");
        return Files.exists(inModule) ? inModule : Path.of("spring/s14-production-readiness/Dockerfile");
    }

    /** One Dockerfile instruction: {@code COPY --from=extract /build/extracted/application/ ./}. */
    record Instruction(String keyword, String arguments) {

        boolean is(String keyword) {
            return this.keyword.equals(keyword);
        }

        static List<Instruction> parse(Path dockerfile) throws IOException {
            List<Instruction> instructions = new ArrayList<>();
            var current = new StringBuilder();
            for (String line : Files.readAllLines(dockerfile)) {
                String text = line.strip();
                if (text.isEmpty() || text.startsWith("#")) {
                    continue; // Docker also skips comment lines inside a continued instruction
                }
                if (text.endsWith("\\")) {
                    current.append(text, 0, text.length() - 1).append(' ');
                    continue;
                }
                current.append(text);
                String[] parts = current.toString().strip().split("\\s+", 2);
                instructions.add(new Instruction(parts[0].toUpperCase(Locale.ROOT),
                        parts.length > 1 ? parts[1].strip() : ""));
                current.setLength(0);
            }
            return instructions;
        }
    }
}
