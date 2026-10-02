package practice.j04.ex01;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * One page of a longer result, the way a REST API returns search results: the items on this page, plus what a
 * client needs to show "page 2 of 7" and a "next" link. Spring Data's {@code Page} works the same way (S06).
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>{@code number} counts from 0: the first page is page 0.</li>
 *   <li>{@code number} must not be negative, {@code size} (the maximum number of items per page) must be at
 *       least 1, {@code totalElements} (the number of items on all pages together) must not be negative, and
 *       {@code items} must not hold more than {@code size} items. Otherwise the constructor throws
 *       {@link IllegalArgumentException}.</li>
 *   <li>{@code items} and its elements must not be {@code null} ({@link NullPointerException}).</li>
 *   <li>The page keeps its own unmodifiable copy of {@code items}: changing the list you passed in later doesn't
 *       change the page, and {@link #items()} can't be modified.</li>
 *   <li>Page numbers often come straight from a URL ({@code ?page=...}), so every method must work for any
 *       valid {@code number}, up to {@link Integer#MAX_VALUE}, and any {@code totalElements}, up to
 *       {@link Long#MAX_VALUE}.</li>
 * </ul>
 *
 * @param items         the items on this page
 * @param number        the page number, starting at 0
 * @param size          the maximum number of items per page
 * @param totalElements the number of items on all pages together
 * @param <T>           the type of the items
 */
public record Page<T>(List<T> items, int number, int size, long totalElements) {

    /** Checks the values and copies {@code items}. */
    public Page {
        Objects.requireNonNull(items, "items");
        if (number < 0) {
            throw new IllegalArgumentException("number must not be negative, but was " + number);
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be at least 1, but was " + size);
        }
        if (totalElements < 0) {
            throw new IllegalArgumentException("totalElements must not be negative, but was " + totalElements);
        }
        if (items.size() > size) {
            throw new IllegalArgumentException("a page of size " + size + " can't hold " + items.size() + " items");
        }
        // Assigning to the parameter changes what the record stores. List.copyOf makes an unmodifiable copy
        // and rejects null elements, so nobody can change the page through the list they passed in.
        items = List.copyOf(items);
    }

    /**
     * Returns how many pages {@code totalElements} items fill, with {@code size} items per page: 21 items at
     * 10 per page need 3 pages. With 0 items there are 0 pages.
     */
    public long totalPages() {
        // Math.ceilDiv rounds the division up. The common trick (totalElements + size - 1) / size gives the same
        // result, but overflows when totalElements is close to Long.MAX_VALUE.
        return Math.ceilDiv(totalElements, size);
    }

    /** Returns whether there is a page after this one. */
    public boolean hasNext() {
        return number + 1 < totalPages();
    }

    /**
     * Returns a page with every item converted by {@code mapper}, and the same {@code number}, {@code size} and
     * {@code totalElements}. For example, an API turns a {@code Page<Book>} from the database into a
     * {@code Page<BookResponse>} for the client.
     *
     * @throws NullPointerException if {@code mapper} is {@code null} or returns {@code null}
     */
    public <R> Page<R> map(Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        List<R> mapped = new ArrayList<>(items.size());
        for (T item : items) {
            mapped.add(mapper.apply(item)); // a "? extends R" result is an R, so it fits into a List<R>
        }
        return new Page<>(mapped, number, size, totalElements);
    }
}
