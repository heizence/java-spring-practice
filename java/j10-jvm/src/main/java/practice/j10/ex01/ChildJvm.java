package practice.j10.ex01;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import practice.j10.ex01.Puzzles.Ending;

/**
 * Runs a program's {@code main} method in a new JVM and reports how the program ended. The "How does the program
 * end?" puzzles use it, because a {@link StackOverflowError} or an {@link OutOfMemoryError} is best observed in a
 * JVM of its own.
 *
 * <p>The new JVM uses the same {@code java} executable and class path as the JVM that runs the tests. It gets:
 * <ul>
 *   <li>the options the puzzle passes, such as {@code -Xss512k} (the stack size of each thread) or
 *       {@code -Xmx16m} (the maximum heap size);</li>
 *   <li>{@code -XX:+UseSerialGC}, the simplest garbage collector, so that the results don't depend on which
 *       collector your machine would pick.</li>
 * </ul>
 *
 * <p>You can run the programs yourself, too. In IntelliJ IDEA, open a program, run its {@code main} method once,
 * then add the options under Run → Edit Configurations → Modify options → Add VM options. On the command line,
 * from the repository root and with a Java 25 {@code java} (older versions can't load these classes):
 * {@code java -Xmx16m -cp java/j10-jvm/build/classes/java/main practice.j10.ex01.programs.BoxedList}.
 */
final class ChildJvm {

    private static final Duration TIMEOUT = Duration.ofSeconds(8);

    // The java launcher reads these environment variables. Options in them would change the child JVM's settings
    // behind the puzzle's back (_JAVA_OPTIONS even overrides the command line), so the child doesn't get them.
    private static final List<String> JVM_OPTION_VARIABLES =
            List.of("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS");

    private ChildJvm() {
    }

    /** Runs {@code program} with {@code jvmOptions} and tells how it ended. */
    static Ending run(Class<?> program, String... jvmOptions) {
        List<String> command = new ArrayList<>();
        command.add(javaExecutable());
        command.add("-XX:+UseSerialGC");
        command.addAll(List.of(jvmOptions));
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(program.getName());

        Path output = null;
        try {
            // Output goes to a file, not a pipe: a pipe that nobody reads fills up after a few kilobytes and then
            // blocks the child. A StackOverflowError alone prints about 1,000 lines of stack trace.
            output = Files.createTempFile("j10-child-jvm-", ".log");
            var builder = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .redirectOutput(output.toFile());
            Map<String, String> environment = builder.environment();
            JVM_OPTION_VARIABLES.forEach(environment::remove);

            Process process = builder.start();
            if (!process.waitFor(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly().waitFor(2, TimeUnit.SECONDS); // let it exit, so its file can be deleted
                throw new IllegalStateException(program.getSimpleName() + " didn't finish within "
                        + TIMEOUT.toSeconds() + " seconds");
            }
            // This String constructor never fails: bytes that aren't valid UTF-8 become replacement characters.
            String text = new String(Files.readAllBytes(output), StandardCharsets.UTF_8);
            return endingOf(program, process.exitValue(), text);
        } catch (IOException e) {
            throw new UncheckedIOException("could not run " + program.getSimpleName() + " in a new JVM", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while waiting for " + program.getSimpleName(), e);
        } finally {
            deleteQuietly(output);
        }
    }

    private static Ending endingOf(Class<?> program, int exitCode, String output) {
        if (exitCode == 0) {
            return Ending.COMPLETES;
        }
        // An uncaught error is printed as 'Exception in thread "main" java.lang.StackOverflowError'. When the heap
        // is completely full, the JVM may fail to print the whole trace, but the class name is always there.
        int stackOverflow = output.indexOf("java.lang.StackOverflowError");
        int outOfMemory = output.indexOf("java.lang.OutOfMemoryError");
        if (stackOverflow >= 0 && (outOfMemory < 0 || stackOverflow < outOfMemory)) {
            return Ending.STACK_OVERFLOW_ERROR;
        }
        if (outOfMemory >= 0) {
            return Ending.OUT_OF_MEMORY_ERROR;
        }
        throw new IllegalStateException(program.getSimpleName() + " ended with exit code " + exitCode
                + " but without a StackOverflowError or an OutOfMemoryError. Its output began with:\n"
                + output.substring(0, Math.min(output.length(), 2_000)));
    }

    private static String javaExecutable() {
        // The executable of the JVM we're running in, so the child uses the same Java version.
        return ProcessHandle.current().info().command()
                .orElseGet(() -> Path.of(System.getProperty("java.home"), "bin", "java").toString());
    }

    private static void deleteQuietly(Path file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            // Only a temporary file is left behind; the result of the puzzle is still correct.
        }
    }
}
