package com.example.bookstore.book;

import java.util.List;

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
        String[] sortParts = sort.split(",");
        String orderBy = sortParts.length > 1 ? sortParts[0] + " " + sortParts[1] : sortParts[0];

        String sql = "SELECT b.id, b.isbn, b.title, b.author, c.name AS category, b.price, b.published_date"
                + " FROM books b JOIN categories c ON c.id = b.category_id"
                + " WHERE b.title LIKE '%" + text + "%' OR b.author LIKE '%" + text + "%'"
                + " ORDER BY " + orderBy + ", b.id"
                + " LIMIT " + MAX_RESULTS;
        return jdbc.sql(sql).query(Book.class).list();
    }
}
