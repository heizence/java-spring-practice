package com.example.bookstore.book;

import java.net.URI;
import java.util.List;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import com.example.bookstore.common.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ETag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The catalog API: everything under {@code /api/books}. Controllers handle HTTP only: read the request, call one
 * {@link BookService} method, turn the result into a response. This is the controller after all ten exercises; the
 * comments say which exercise added what.
 */
@Tag(name = "Books", description = "The catalog: list, read, add, change and remove books.")
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    /**
     * A page of books: {@code GET /api/books?page=0&size=20&sort=price,desc} (S04-01, S04-06).
     *
     * <p>Spring checks the constraints on the parameters before this method runs, and answers {@code 400} if one
     * fails. {@link BookSort#parse} reports an invalid {@code sort} the same way, with a {@code BusinessException}.
     */
    @GetMapping
    public PageResponse<BookResponse> getBooks(
            @RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "id") String sort) {
        return bookService.findBooks(page, size, BookSort.parse(sort));
    }

    /**
     * One book, with its version as the {@code ETag} header (S04-01, S04-03, S04-07).
     *
     * <p>S04-01 answered an unknown id with {@code ResponseEntity.of(optional)}: a {@code 404} with an empty body.
     * Throwing a {@code BusinessException} instead lets {@code GlobalExceptionHandler} write the problem detail.
     * For a {@code GET} with {@code If-None-Match}, Spring compares the ETag of this response and answers
     * {@code 304 Not Modified} by itself.
     */
    @Operation(summary = "Get a book")
    @ApiResponse(responseCode = "200", description = "The book, with its version as the ETag header",
            useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", description = "No book has this id",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{id}")
    public ResponseEntity<BookResponse> getBook(@PathVariable long id) {
        return bookService.findBook(id)
                .map(book -> ResponseEntity.ok().eTag(eTagOf(book)).body(book))
                .orElseGet(() -> ResponseEntity.ok().build());
    }

    /**
     * One book in version 2 of the API (S04-08). A mapping with a {@code version} matches only requests for that
     * version; the mapping above, without one, answers every other version.
     */
    @GetMapping(path = "/{id}", version = "2")
    public BookV2Response getBookV2(@PathVariable long id) {
        return bookService.findBookV2(id).orElseThrow(() -> bookNotFound(id));
    }

    /** Adds a book: {@code 201 Created}, with the new book's address in the {@code Location} header (S04-02). */
    @Operation(summary = "Add a book")
    @ApiResponse(responseCode = "201", description = "The new book; its address is in the Location header",
            useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "The book is invalid",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "A book with this ISBN exists",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    public ResponseEntity<BookResponse> createBook(@Valid @RequestBody CreateBookRequest request) {
        BookResponse book = bookService.createBook(request);
        // A path is a valid Location (RFC 9110); ServletUriComponentsBuilder would build a full URL instead.
        return ResponseEntity.created(URI.create("/api/books/" + book.id())).body(book);
    }

    /**
     * Replaces a book, if it still has the version that the {@code If-Match} header names (S04-07).
     *
     * <p>The header is optional for Spring ({@code required = false}), so that a request without it reaches
     * {@link #expectedVersion}, which answers {@code 428}. A required header would make Spring answer {@code 400}
     * before this method runs.
     */
    @PutMapping("/{id}")
    public ResponseEntity<BookResponse> replaceBook(
            @PathVariable long id,
            @RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @Valid @RequestBody ReplaceBookRequest request) {
        BookResponse book = bookService.replaceBook(id, expectedVersion(ifMatch), request);
        return ResponseEntity.ok().eTag(eTagOf(book)).body(book);
    }

    /** Changes some fields of a book, if it still has the version that {@code If-Match} names (S04-07). */
    @PatchMapping("/{id}")
    public ResponseEntity<BookResponse> patchBook(
            @PathVariable long id,
            @RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @Valid @RequestBody PatchBookRequest request) {
        BookResponse book = bookService.patchBook(id, expectedVersion(ifMatch), request);
        return ResponseEntity.ok().eTag(eTagOf(book)).body(book);
    }

    /**
     * Removes a book: {@code 204 No Content}, also when the book doesn't exist (any more) (S04-07). The book is gone
     * either way, so a client can repeat a {@code DELETE} whose response it never received.
     */
    @Operation(summary = "Remove a book")
    @ApiResponse(responseCode = "204", description = "The book is gone, or never existed")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable long id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }

    /** The version as an ETag. Spring adds the quotes: the header reads {@code ETag: "3"}. */
    private static String eTagOf(BookResponse book) {
        return Long.toString(book.version());
    }

    /**
     * The version that an {@code If-Match} header names. It must be exactly one strong ETag, such as {@code "3"}.
     * A weak ETag ({@code W/"3"}) never matches, because {@code If-Match} compares strongly, and a value that isn't
     * a version can't match either: both fail like an old version. (The header may also hold {@code *} or a list
     * of ETags; this API doesn't support those, so they fail too.)
     */
    private static long expectedVersion(String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank()) {
            throw new BusinessException(ErrorCode.PRECONDITION_REQUIRED,
                    "Send the book's ETag in an If-Match header, so that you don't overwrite someone else's change.");
        }
        List<ETag> eTags = ETag.parse(ifMatch);
        // 18 digits always fit into a long, so parseLong can't fail after this check.
        if (eTags.size() == 1 && !eTags.getFirst().weak() && eTags.getFirst().tag().matches("\\d{1,18}")) {
            return Long.parseLong(eTags.getFirst().tag());
        }
        throw new BusinessException(ErrorCode.PRECONDITION_FAILED,
                "If-Match must be the book's ETag, such as \"3\", but was %s.".formatted(ifMatch));
    }

    private static BusinessException bookNotFound(long id) {
        return new BusinessException(ErrorCode.BOOK_NOT_FOUND, "Book %d was not found.".formatted(id));
    }
}
