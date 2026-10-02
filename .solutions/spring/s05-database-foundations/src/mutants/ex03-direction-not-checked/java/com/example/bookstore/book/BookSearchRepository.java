package com.example.bookstore.book;

import java.util.List;
import java.util.Map;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * The search box behind {@code GET /api/books/search}: finds books by a piece of their title or author.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>A book matches when its title or its author contains {@code text}, ignoring letter case. The text is only
 *       ever data: quotes, backslashes and SQL keywords in it are ordinary characters. ({@code %} and {@code _}
 *       work as wildcards, as in any {@code LIKE} pattern; that's harmless in a search box.)</li>
 *   <li>{@code sort} is a property, optionally followed by a comma and a direction: {@code price} or
 *       {@code price,desc}. The properties are the API's names {@code title}, {@code price} and
 *       {@code publishedDate}; the directions are {@code asc} (the default) and {@code desc}, in any letter case.
 *       Books that are equal by that property are sorted by id.</li>
 *   <li>Any other {@code sort} is invalid input: a {@link BusinessException} with {@link ErrorCode#INVALID_INPUT},
 *       which the client receives as {@code 400 Bad Request}.</li>
 *   <li>At most {@value #MAX_RESULTS} books are returned.</li>
 * </ul>
 */
@Repository
@RequiredArgsConstructor
public class BookSearchRepository {

    /** The most books one search returns. */
    public static final int MAX_RESULTS = 20;

    /**
     * The allow list: the API's sort properties and the columns they stand for. A column name can't be a bound
     * parameter (parameters carry values, not names), so the ORDER BY clause is built from these constants. The
     * request's value is only used to look one up, never copied into the SQL.
     */
    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "title", "b.title",
            "price", "b.price",
            "publishedDate", "b.published_date");

    private final JdbcClient jdbc;

    /**
     * Finds books whose title or author contains {@code text}.
     *
     * @param text the text to look for, not blank
     * @param sort the order of the results, such as {@code title} or {@code price,desc}
     * @return up to {@value #MAX_RESULTS} books, in the requested order
     * @throws BusinessException {@link ErrorCode#INVALID_INPUT} if {@code sort} isn't one of the allowed orders
     */
    public List<Book> search(String text, String sort) {
        // The ORDER BY clause is the only part that changes, and it comes from constants. Everything the client
        // sent travels as a bound parameter, which the driver sends as data, never as SQL.
        String sql = """
                SELECT b.id, b.isbn, b.title, b.author, c.name AS category, b.price, b.published_date
                FROM books b
                JOIN categories c ON c.id = b.category_id
                WHERE b.title LIKE :pattern OR b.author LIKE :pattern
                ORDER BY %s, b.id
                LIMIT :limit""".formatted(orderBy(sort));
        return jdbc.sql(sql)
                .param("pattern", "%" + text + "%") // one named parameter, used twice
                .param("limit", MAX_RESULTS)
                .query(Book.class)
                .list();
    }

    /** Turns {@code price,desc} into {@code b.price DESC}, using only the constants above. */
    private static String orderBy(String sort) {
        String[] parts = sort.split(",", -1); // -1 keeps an empty direction ("price,"), which is invalid too
        String column = SORT_COLUMNS.get(parts[0].strip());
        if (column == null || parts.length > 2) {
            throw invalidSort(sort);
        }
        if (parts.length == 1) {
            return column + " ASC";
        }
        return column + " " + parts[1];
    }

    private static BusinessException invalidSort(String sort) {
        return new BusinessException(ErrorCode.INVALID_INPUT,
                "Can't sort by '%s'. Sort by title, price or publishedDate, optionally followed by ,asc or ,desc."
                        .formatted(sort));
    }
}
