package practice.j03.ex01;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * A {@link Reader} for tests. It reads from a string, remembers whether {@link #close()} was called, and can be
 * told to fail while reading or while closing. That lets a test prove that code closes a reader on every path,
 * including the paths where something goes wrong.
 *
 * <pre>
 * var reader = TrackingReader.of("port=8080").failReadsWith(new IOException("disk error"));
 * </pre>
 */
final class TrackingReader extends Reader {

    private final Reader text;
    private IOException readFailure;
    private IOException closeFailure;
    private boolean closed;

    private TrackingReader(String text) {
        this.text = new StringReader(text);
    }

    /** A reader that returns {@code text}. */
    static TrackingReader of(String text) {
        return new TrackingReader(text);
    }

    /** Makes every {@code read} throw {@code failure}. Returns this reader, so calls can be chained. */
    TrackingReader failReadsWith(IOException failure) {
        this.readFailure = failure;
        return this;
    }

    /** Makes {@link #close()} throw {@code failure}, after marking the reader as closed. */
    TrackingReader failCloseWith(IOException failure) {
        this.closeFailure = failure;
        return this;
    }

    /** Whether {@link #close()} has been called. */
    boolean isClosed() {
        return closed;
    }

    @Override
    public int read(char[] buffer, int offset, int length) throws IOException {
        if (closed) {
            throw new IOException("Stream closed");
        }
        if (readFailure != null) {
            throw readFailure;
        }
        return text.read(buffer, offset, length);
    }

    @Override
    public void close() throws IOException {
        closed = true;
        if (closeFailure != null) {
            throw closeFailure;
        }
    }
}
