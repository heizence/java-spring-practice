package practice.j03.ex01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.entry;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("J03-01 · Checked exceptions and try-with-resources")
class ConfigLoaderTest {

    private final ConfigLoader loader = new ConfigLoader();

    private Map<String, String> load(String text) {
        return loader.load(new StringReader(text));
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("reads key=value lines")
        void readsKeyValueLines() {
            Map<String, String> settings = load("host=localhost\nport=8080\n");

            assertThat(settings).containsExactly(entry("host", "localhost"), entry("port", "8080"));
        }

        @Test
        @DisplayName("trims whitespace around keys and values")
        void trimsKeysAndValues() {
            Map<String, String> settings = load("  db.url  =  jdbc:mysql://localhost/shop  \n\tmode\t=\ttest\t\n");

            assertThat(settings).containsExactly(entry("db.url", "jdbc:mysql://localhost/shop"), entry("mode", "test"));
        }

        @Test
        @DisplayName("skips blank lines and comments")
        void skipsBlankLinesAndComments() {
            Map<String, String> settings = load("""
                    # Database
                    db.url=jdbc:mysql://localhost/shop

                    # Connection pool
                    db.pool-size=10
                    """);

            assertThat(settings)
                    .containsExactly(entry("db.url", "jdbc:mysql://localhost/shop"), entry("db.pool-size", "10"));
        }

        @Test
        @DisplayName("keeps the keys in the order of the text")
        void keepsOrderOfText() {
            Map<String, String> settings = load("""
                    zulu=1
                    alpha=2
                    mike=3
                    bravo=4
                    yankee=5
                    charlie=6
                    xray=7
                    delta=8
                    """);

            assertThat(settings.keySet())
                    .containsExactly("zulu", "alpha", "mike", "bravo", "yankee", "charlie", "xray", "delta");
        }

        @Test
        @DisplayName("closes the reader after loading")
        void closesReader() {
            var reader = TrackingReader.of("port=8080");

            loader.load(reader);

            assertThat(reader.isClosed()).isTrue();
        }

        @Test
        @DisplayName("loads a file as UTF-8")
        void loadsFileAsUtf8(@TempDir Path dir) throws IOException {
            Path file = dir.resolve("app.conf");
            Files.writeString(file, "greeting=안녕하세요\ncity=서울\n", StandardCharsets.UTF_8);

            Map<String, String> settings = loader.loadFile(file);

            assertThat(settings).containsExactly(entry("greeting", "안녕하세요"), entry("city", "서울"));
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("returns an empty map for empty text")
        void emptyText() {
            assertThat(load("")).isEmpty();
        }

        @Test
        @DisplayName("returns an empty map when there are only comments and blank lines")
        void onlyCommentsAndBlankLines() {
            assertThat(load("# nothing here\n\n   \n\t# still nothing\n")).isEmpty();
        }

        @Test
        @DisplayName("splits at the first '=', so a value may contain '='")
        void valueMayContainEquals() {
            Map<String, String> settings = load("db.url=jdbc:mysql://db/shop?user=app&ssl=true");

            assertThat(settings).containsExactly(entry("db.url", "jdbc:mysql://db/shop?user=app&ssl=true"));
        }

        @Test
        @DisplayName("allows an empty value")
        void emptyValue() {
            assertThat(load("name=\nnickname =   \n")).containsExactly(entry("name", ""), entry("nickname", ""));
        }

        @Test
        @DisplayName("keeps a '#' that is not the first character of the line")
        void hashInsideValue() {
            Map<String, String> settings = load("color=#ff0000\nhome=https://example.com/#top\n");

            assertThat(settings).containsExactly(entry("color", "#ff0000"), entry("home", "https://example.com/#top"));
        }

        @Test
        @DisplayName("skips an indented comment, even one that contains '='")
        void indentedComment() {
            assertThat(load("   # port=1234\nport=8080\n")).containsExactly(entry("port", "8080"));
        }

        @Test
        @DisplayName("reads Windows line endings (\\r\\n)")
        void windowsLineEndings() {
            assertThat(load("host=localhost\r\nport=8080\r\n"))
                    .containsExactly(entry("host", "localhost"), entry("port", "8080"));
        }

        @Test
        @DisplayName("treats keys as case-sensitive")
        void keysAreCaseSensitive() {
            assertThat(load("Port=1\nport=2\n")).containsExactly(entry("Port", "1"), entry("port", "2"));
        }

        @Test
        @DisplayName("returns a map that can't be modified")
        void resultIsUnmodifiable() {
            Map<String, String> settings = load("port=8080\n");

            assertThatThrownBy(() -> settings.put("host", "localhost"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("rejects a line without '=' and reports its line number")
        void lineWithoutEquals() {
            assertThatThrownBy(() -> load("host=localhost\nport 8080\n"))
                    .isInstanceOf(ConfigException.class)
                    .hasMessageContaining("line 2");
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"=8080", "   = 8080", "\t=\t"})
        @DisplayName("rejects a line with an empty key")
        void emptyKey(String line) {
            assertThatThrownBy(() -> load("host=localhost\n" + line + "\n"))
                    .isInstanceOf(ConfigException.class)
                    .hasMessageContaining("line 2");
        }

        @Test
        @DisplayName("counts blank lines and comments in the line number")
        void lineNumberCountsEveryLine() {
            String text = """
                    # Server

                    host=localhost

                    port 8080
                    """;

            assertThatThrownBy(() -> load(text))
                    .isInstanceOf(ConfigException.class)
                    .hasMessageContaining("line 5");
        }

        @Test
        @DisplayName("rejects a key that appears twice, naming the key and the second line")
        void duplicateKey() {
            assertThatThrownBy(() -> load("port=8080\nhost=localhost\nport=9090\n"))
                    .isInstanceOf(ConfigException.class)
                    .hasMessageContaining("port")
                    .hasMessageContaining("line 3");
        }

        @Test
        @DisplayName("treats keys that differ only in surrounding whitespace as duplicates")
        void duplicateKeyAfterTrimming() {
            assertThatThrownBy(() -> load("port=8080\n  port  = 9090\n"))
                    .isInstanceOf(ConfigException.class)
                    .hasMessageContaining("line 2");
        }

        @Test
        @DisplayName("closes the reader when a line is malformed")
        void closesReaderOnMalformedLine() {
            var reader = TrackingReader.of("host=localhost\nport 8080\n");

            assertThatThrownBy(() -> loader.load(reader)).isInstanceOf(ConfigException.class);

            assertThat(reader.isClosed()).isTrue();
        }

        @Test
        @DisplayName("turns a read failure into a ConfigException that keeps the IOException as its cause")
        void readFailure() {
            var failure = new IOException("disk error");
            var reader = TrackingReader.of("port=8080").failReadsWith(failure);

            assertThatThrownBy(() -> loader.load(reader))
                    .isInstanceOf(ConfigException.class)
                    .cause().isSameAs(failure);
            assertThat(reader.isClosed()).isTrue();
        }

        @Test
        @DisplayName("keeps a failure in close() as a suppressed exception of the read failure")
        void closeFailureIsSuppressedByReadFailure() {
            var readFailure = new IOException("read failed");
            var closeFailure = new IOException("close failed");
            var reader = TrackingReader.of("port=8080").failReadsWith(readFailure).failCloseWith(closeFailure);

            ConfigException thrown = catchThrowableOfType(ConfigException.class, () -> loader.load(reader));

            assertThat(thrown).cause().isSameAs(readFailure);
            assertThat(readFailure.getSuppressed()).containsExactly(closeFailure);
        }

        @Test
        @DisplayName("doesn't let a failure in close() hide a malformed line")
        void closeFailureDoesNotHideMalformedLine() {
            var closeFailure = new IOException("close failed");
            var reader = TrackingReader.of("port 8080").failCloseWith(closeFailure);

            ConfigException thrown = catchThrowableOfType(ConfigException.class, () -> loader.load(reader));

            assertThat(thrown).hasMessageContaining("line 1");
            assertThat(thrown.getSuppressed()).containsExactly(closeFailure);
        }

        @Test
        @DisplayName("reports a failure in close() after a successful read")
        void closeFailureAfterSuccessfulRead() {
            var closeFailure = new IOException("close failed");
            var reader = TrackingReader.of("port=8080").failCloseWith(closeFailure);

            assertThatThrownBy(() -> loader.load(reader))
                    .isInstanceOf(ConfigException.class)
                    .cause().isSameAs(closeFailure);
        }

        @Test
        @DisplayName("reports a missing file with its path and a NoSuchFileException as the cause")
        void missingFile(@TempDir Path dir) {
            Path missing = dir.resolve("missing.conf");

            assertThatThrownBy(() -> loader.loadFile(missing))
                    .isInstanceOf(ConfigException.class)
                    .hasMessageContaining(missing.toString())
                    .hasCauseInstanceOf(NoSuchFileException.class);
        }

        @Test
        @DisplayName("wraps a file that can't be read, such as a directory, keeping the IOException as the cause")
        void unreadableFile(@TempDir Path dir) {
            assertThatThrownBy(() -> loader.loadFile(dir))
                    .isInstanceOf(ConfigException.class)
                    .hasMessageContaining(dir.toString())
                    .hasCauseInstanceOf(IOException.class);
        }

        @Test
        @DisplayName("reports the line number of a malformed line in a file")
        void malformedLineInFile(@TempDir Path dir) throws IOException {
            Path file = Files.writeString(dir.resolve("app.conf"), "host=localhost\nport 8080\n");

            assertThatThrownBy(() -> loader.loadFile(file))
                    .isInstanceOf(ConfigException.class)
                    .hasMessageContaining("line 2");
        }

        @Test
        @DisplayName("rejects null arguments")
        void rejectsNull() {
            assertThatThrownBy(() -> loader.load(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> loader.loadFile(null)).isInstanceOf(NullPointerException.class);
        }
    }
}
