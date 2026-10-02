package practice.j08.ex02;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Counts page views and remembers each visitor's landing page: the first page they opened.
 *
 * <p>One {@code HitCounter} is shared by every request thread, like a singleton bean in a Spring web app. So every
 * method can be called by many threads at the same time.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>{@link #hit()} counts one page view. No view is ever lost, however many threads call it at once.</li>
 *   <li>{@link #hits()} returns the number of views counted so far.</li>
 *   <li>{@link #registerIfAbsent(String, String)} stores {@code page} as the landing page of a visitor who has none
 *       yet, and returns {@code true}. For a visitor who already has a landing page, it changes nothing and returns
 *       {@code false}. When many threads register the same new visitor at the same moment, exactly one of them gets
 *       {@code true}, and its page is the one that is kept.</li>
 *   <li>{@link #landingPage(String)} returns the visitor's landing page, or empty for an unknown visitor.</li>
 *   <li>{@link #visitors()} returns the number of registered visitors.</li>
 *   <li>{@code null} arguments throw {@link NullPointerException}.</li>
 * </ul>
 */
public class HitCounter {

    // volatile: every thread always sees the latest count.
    private volatile long hits;

    // A HashMap must not be changed by several threads at once: entries can get lost. ConcurrentHashMap can, and it
    // also offers atomic "check and change" methods such as putIfAbsent.
    private final ConcurrentMap<String, String> landingPages = new ConcurrentHashMap<>();

    /** Counts one page view. */
    public void hit() {
        hits++;
    }

    /** The number of page views counted so far. */
    public long hits() {
        return hits;
    }

    /**
     * Stores {@code page} as the landing page of {@code visitorId}, unless the visitor already has one.
     *
     * @return {@code true} if this call stored the page, {@code false} if the visitor already had a landing page
     */
    public boolean registerIfAbsent(String visitorId, String page) {
        Objects.requireNonNull(visitorId, "visitorId");
        Objects.requireNonNull(page, "page");
        // containsKey + put is "check, then act": two threads can both pass the check before either one puts.
        // putIfAbsent does the check and the insert as one atomic step, and returns the value that was already
        // there, or null if this call inserted ours.
        return landingPages.putIfAbsent(visitorId, page) == null;
    }

    /** The landing page of {@code visitorId}, or empty if the visitor is unknown. */
    public Optional<String> landingPage(String visitorId) {
        Objects.requireNonNull(visitorId, "visitorId");
        return Optional.ofNullable(landingPages.get(visitorId));
    }

    /** The number of registered visitors. */
    public int visitors() {
        return landingPages.size();
    }
}
