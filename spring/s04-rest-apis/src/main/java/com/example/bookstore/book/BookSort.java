package com.example.bookstore.book;

import java.util.Comparator;
import java.util.Objects;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;

/**
 * How to sort the catalog: by one property, ascending or descending. Books with equal values come in id order, so
 * the order is the same on every request, and a book never shows up on two pages or on none.
 *
 * @param property  what to sort by
 * @param direction ascending or descending
 */
public record BookSort(Property property, Direction direction) {

    /** By id, ascending: the order when the request doesn't ask for another. */
    public static final BookSort DEFAULT = new BookSort(Property.ID, Direction.ASC);

    public BookSort {
        Objects.requireNonNull(property, "property");
        Objects.requireNonNull(direction, "direction");
    }

    /**
     * The properties that clients may sort by: an allow list. Each has the name that clients use, the same as the
     * JSON property's name.
     */
    public enum Property {
        ID("id", Comparator.comparing(Book::getId)),
        TITLE("title", Comparator.comparing(Book::getTitle)),
        AUTHOR("author", Comparator.comparing(Book::getAuthor)),
        PRICE("price", Comparator.comparingLong(Book::getPrice)),
        PUBLISHED_DATE("publishedDate", Comparator.comparing(Book::getPublishedDate));

        private final String parameterName;
        private final Comparator<Book> comparator;

        Property(String parameterName, Comparator<Book> comparator) {
            this.parameterName = parameterName;
            this.comparator = comparator;
        }

        /** The name in the {@code sort} parameter, such as {@code publishedDate}. */
        public String parameterName() {
            return parameterName;
        }
    }

    public enum Direction {
        ASC,
        DESC
    }

    /**
     * Reads the {@code sort} request parameter: a property, optionally followed by a comma and a direction, such as
     * {@code price} or {@code price,desc}.
     *
     * <h2>Rules</h2>
     * <ul>
     *   <li>The property is one of the {@link Property} names, spelled exactly like the JSON property: {@code id},
     *       {@code title}, {@code author}, {@code price}, {@code publishedDate}.</li>
     *   <li>The direction is {@code asc} or {@code desc}, in any case ({@code DESC} works too). Without one, the
     *       direction is ascending.</li>
     *   <li>Spaces around the property and the direction don't matter: {@code price , desc} works.</li>
     * </ul>
     *
     * <h2>Errors</h2>
     * A {@link BusinessException} with {@link ErrorCode#INVALID_INPUT}, so the client gets a {@code 400}, with
     * exactly one of these messages:
     * <ul>
     *   <li>an unknown property: {@code Can't sort by 'colour'. Sort by one of: id, title, author, price,
     *       publishedDate.}</li>
     *   <li>an unknown direction: {@code Can't sort in direction 'sideways'. Use asc or desc.}</li>
     *   <li>anything else, such as {@code price,desc,id}, {@code ,desc} or {@code price,}: {@code Can't read the
     *       sort 'price,desc,id'. Use a property and an optional direction, such as price,desc.}</li>
     * </ul>
     *
     * @param parameter the parameter's value; never {@code null}, because the controller declares a default
     */
    public static BookSort parse(String parameter) {
        // TODO: S04-06
        throw new UnsupportedOperationException("TODO: S04-06");
    }

    /** The order of the books: by the property in the direction, then by id, always ascending. */
    public Comparator<Book> comparator() {
        Comparator<Book> byProperty = direction == Direction.ASC
                ? property.comparator
                : property.comparator.reversed();
        // The tie-breaker comes last, outside reversed(): equal prices in id order, whatever the direction.
        return byProperty.thenComparing(Book::getId);
    }
}
