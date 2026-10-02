package practice.j08.ex01;

/**
 * Thrown by {@link ParallelFetcher#fetchAll} when the fetcher fails for an id.
 *
 * <p>{@link #getCause()} is the exception that the fetcher threw, and {@link #id()} is the id it failed for.
 */
public class FetchException extends RuntimeException {

    private final String id;

    public FetchException(String id, Throwable cause) {
        super("Fetching '" + id + "' failed: " + cause, cause);
        this.id = id;
    }

    /** The id the fetcher failed for. */
    public String id() {
        return id;
    }
}
