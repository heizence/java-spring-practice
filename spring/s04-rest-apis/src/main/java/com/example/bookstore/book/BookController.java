package com.example.bookstore.book;

import com.example.bookstore.common.web.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;

/**
 * The catalog API: everything under {@code /api/books}. Several exercises build it; each method names its exercise.
 * Controllers handle HTTP only: read the request, call one {@link BookService} method, turn the result into a
 * response.
 *
 * <p>TODO: S04-01: make this class a REST controller for the paths under {@code /api/books}. The methods have no
 * mapping annotations yet, so none of them is an endpoint: add the annotations as part of each exercise.
 */
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    /**
     * S04-01: {@code GET /api/books?page=0&size=20}: a page of books, in id order ({@link BookSort#DEFAULT}).
     *
     * <ul>
     *   <li>The request parameters {@code page} (from 0) and {@code size} are optional, with the defaults 0 and 20.
     *       </li>
     *   <li>{@code 200 OK} with the page: {@link BookService#findBooks} builds it.</li>
     * </ul>
     *
     * <p>S04-06 adds sorting and the checks of the parameters.
     */
    public PageResponse<BookResponse> getBooks(int page, int size) {
        // TODO: S04-01
        throw new UnsupportedOperationException("TODO: S04-01");
    }

    /**
     * S04-01: {@code GET /api/books/{id}}: one book.
     *
     * <ul>
     *   <li>{@code 200 OK} with the book as JSON.</li>
     *   <li>{@code 404 Not Found} with an empty body if no book has this id. ({@code ResponseEntity} has a method for
     *       exactly this case.)</li>
     * </ul>
     *
     * <p>S04-03 and S04-07 change what this method answers.
     */
    public ResponseEntity<BookResponse> getBook(long id) {
        // TODO: S04-01
        throw new UnsupportedOperationException("TODO: S04-01");
    }

    /**
     * S04-02: {@code POST /api/books}: adds a book.
     *
     * <ul>
     *   <li>The body is JSON that becomes a {@link CreateBookRequest}, and it must be valid: its constraints are
     *       checked before this method runs. Invalid input gets {@code 400 Bad Request}.</li>
     *   <li>{@code 201 Created} with the new book as JSON, and the new book's address in the {@code Location}
     *       header: {@code /api/books/61}.</li>
     * </ul>
     */
    public ResponseEntity<BookResponse> createBook(CreateBookRequest request) {
        // TODO: S04-02
        throw new UnsupportedOperationException("TODO: S04-02");
    }

    /**
     * S04-07: {@code PUT /api/books/{id}}: replaces everything about a book that may change, if the book still has
     * the version that the client read.
     *
     * <ul>
     *   <li>The body is a valid {@link ReplaceBookRequest}, or {@code 400}.</li>
     *   <li>The {@code If-Match} header names the version the client read: exactly one strong ETag, such as
     *       {@code "3"}. Without the header: {@code 428 Precondition Required}, code {@code PRECONDITION_REQUIRED}.
     *       A weak ETag ({@code W/"3"}), a value that isn't a version, or an old version:
     *       {@code 412 Precondition Failed}, code {@code PRECONDITION_FAILED}.</li>
     *   <li>{@code 200 OK} with the book as JSON, and its new version as the {@code ETag} header.</li>
     * </ul>
     */
    public ResponseEntity<BookResponse> replaceBook(long id, String ifMatch, ReplaceBookRequest request) {
        // TODO: S04-07
        throw new UnsupportedOperationException("TODO: S04-07");
    }

    /**
     * S04-07: {@code PATCH /api/books/{id}}: changes the fields that the body contains, if the book still has the
     * version that the client read. The body is a valid {@link PatchBookRequest}; {@code If-Match}, the responses
     * and the {@code ETag} work as for {@link #replaceBook}.
     */
    public ResponseEntity<BookResponse> patchBook(long id, String ifMatch, PatchBookRequest request) {
        // TODO: S04-07
        throw new UnsupportedOperationException("TODO: S04-07");
    }

    /**
     * S04-07: {@code DELETE /api/books/{id}}: removes a book. {@code 204 No Content}, also when no book has this id
     * (any more): the book is gone either way, so a client can safely repeat a {@code DELETE} whose response it
     * never received.
     */
    public ResponseEntity<Void> deleteBook(long id) {
        // TODO: S04-07
        throw new UnsupportedOperationException("TODO: S04-07");
    }

    /**
     * S04-08: {@code GET /api/books/{id}} in version 2 of the API, for requests with the header
     * {@code API-Version: 2}: {@code 200 OK} with a {@link BookV2Response}, or a {@code 404} like version 1.
     */
    public BookV2Response getBookV2(long id) {
        // TODO: S04-08
        throw new UnsupportedOperationException("TODO: S04-08");
    }
}
