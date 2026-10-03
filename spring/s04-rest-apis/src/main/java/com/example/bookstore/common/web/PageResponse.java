package com.example.bookstore.common.web;

import java.util.List;
import java.util.Objects;

import org.springframework.util.Assert;

/**
 * One page of a list, in the JSON shape that {@code spring/BOOKSTORE.md} defines for every list:
 *
 * <pre>{@code
 * { "content": [ ... ],
 *   "page": { "size": 20, "number": 0, "totalElements": 60, "totalPages": 3 } }
 * }</pre>
 *
 * <p>From S06 on, Spring Data's {@code PagedModel} produces the same shape from a database query.
 *
 * @param content the elements on this page; empty for a page after the last one
 * @param page    where this page lies in the whole list
 * @param <T>     the type of the elements
 */
public record PageResponse<T>(List<T> content, PageMetadata page) {

    public PageResponse {
        content = List.copyOf(content);
        Objects.requireNonNull(page, "page");
    }

    /**
     * Where a page lies in the whole list.
     *
     * @param size          the requested page size, which the last page may not fill
     * @param number        the page number, starting at 0
     * @param totalElements the number of elements in the whole list
     * @param totalPages    how many pages of {@code size} the whole list fills; 0 for an empty list
     */
    public record PageMetadata(int size, int number, long totalElements, int totalPages) {
    }

    /**
     * Cuts one page out of a complete list that is already in the right order. A page after the last one is empty,
     * but still reports the totals, so that a client can see where the list ends.
     *
     * @param all    every element, in order
     * @param number the page number, at least 0
     * @param size   the page size, at least 1
     * @throws IllegalArgumentException if {@code number} or {@code size} is out of range: request validation should
     *                                  have rejected it
     */
    public static <T> PageResponse<T> of(List<T> all, int number, int size) {
        Objects.requireNonNull(all, "all");
        Assert.isTrue(number >= 0, () -> "number must not be negative: " + number);
        Assert.isTrue(size >= 1, () -> "size must be at least 1: " + size);
        // As a long: page 2,000,000,000 of size 100 starts beyond the int range.
        long first = (long) number * size;
        List<T> content = first >= all.size()
                ? List.of()
                : all.subList((int) first, (int) Math.min(all.size(), first + size));
        var metadata = new PageMetadata(size, number, all.size(), Math.ceilDiv(all.size(), size));
        return new PageResponse<>(content, metadata);
    }
}
