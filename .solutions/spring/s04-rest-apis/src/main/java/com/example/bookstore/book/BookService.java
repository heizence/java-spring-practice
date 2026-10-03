package com.example.bookstore.book;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import com.example.bookstore.common.web.PageResponse;
import com.example.bookstore.inventory.Inventory;
import com.example.bookstore.inventory.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Use cases of the catalog: browsing, adding, changing and removing books.
 *
 * <p>Services know nothing about HTTP: they take and return plain values and records, and report a broken rule
 * with a {@link BusinessException}. The controller turns the results into HTTP responses.
 */
@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryRepository inventoryRepository;
    private final CoverStorage coverStorage;

    /**
     * One page of the catalog, in the given order.
     *
     * @param page the page number, at least 0
     * @param size the page size, at least 1
     */
    public PageResponse<BookResponse> findBooks(int page, int size, BookSort sort) {
        List<BookResponse> books = bookRepository.findAll().stream()
                .sorted(sort.comparator())
                .map(BookResponse::from)
                .toList();
        return PageResponse.of(books, page, size);
    }

    /** The book with this id, if there is one. */
    public Optional<BookResponse> findBook(long id) {
        return bookRepository.findById(id).map(BookResponse::from);
    }

    /** The book with this id in version 2 of the API (S04-08), if there is one. */
    public Optional<BookV2Response> findBookV2(long id) {
        return bookRepository.findById(id).map(BookV2Response::from);
    }

    /**
     * Adds a book to the catalog, with no copies in stock yet.
     *
     * @throws BusinessException {@link ErrorCode#CATEGORY_NOT_FOUND} if the category doesn't exist;
     *                           {@link ErrorCode#DUPLICATE_ISBN} if a book with this ISBN exists
     */
    public BookResponse createBook(CreateBookRequest request) {
        Category category = findCategory(request.categoryId());
        if (bookRepository.existsByIsbn(request.isbn())) {
            throw new BusinessException(ErrorCode.DUPLICATE_ISBN,
                    "A book with ISBN %s already exists.".formatted(request.isbn()));
        }
        Book book = bookRepository.save(Book.create(request.isbn(), request.title(), request.author(), category,
                request.price(), request.publishedDate()));
        inventoryRepository.save(new Inventory(book.getId(), 0));
        return BookResponse.from(book);
    }

    /**
     * Replaces everything about a book that may change ({@code PUT}), if the book still has the version that the
     * client read. A replacement that changes nothing keeps the version.
     *
     * @param expectedVersion the version that the client read, from its {@code If-Match} header
     * @throws BusinessException {@link ErrorCode#BOOK_NOT_FOUND}, {@link ErrorCode#CATEGORY_NOT_FOUND}, or
     *                           {@link ErrorCode#PRECONDITION_FAILED} if the book has another version by now
     */
    public BookResponse replaceBook(long id, long expectedVersion, ReplaceBookRequest request) {
        Book book = findBookOrThrow(id);
        checkVersion(book, expectedVersion);
        book.update(request.title(), request.author(), findCategory(request.categoryId()), request.price(),
                request.publishedDate());
        return BookResponse.from(bookRepository.save(book));
    }

    /**
     * Changes the fields of a book that the request contains ({@code PATCH}) and keeps the others, if the book still
     * has the version that the client read. A change that changes nothing keeps the version.
     *
     * @param expectedVersion the version that the client read, from its {@code If-Match} header
     * @throws BusinessException {@link ErrorCode#BOOK_NOT_FOUND}, {@link ErrorCode#CATEGORY_NOT_FOUND} for a new
     *                           category that doesn't exist, or {@link ErrorCode#PRECONDITION_FAILED} if the book has
     *                           another version by now
     */
    public BookResponse patchBook(long id, long expectedVersion, PatchBookRequest request) {
        Book book = findBookOrThrow(id);
        checkVersion(book, expectedVersion);
        // A field the request doesn't contain is null: keep the book's current value for it. The category is looked
        // up only when the request names a new one, so an unchanged category can't fail.
        book.update(
                Objects.requireNonNullElse(request.title(), book.getTitle()),
                Objects.requireNonNullElse(request.author(), book.getAuthor()),
                request.categoryId() == null ? book.getCategory() : findCategory(request.categoryId()),
                Objects.requireNonNullElse(request.price(), book.getPrice()),
                Objects.requireNonNullElse(request.publishedDate(), book.getPublishedDate()));
        // save() compares with the stored book: if nothing changed, the version (and so the ETag) stays the same.
        return BookResponse.from(bookRepository.save(book));
    }

    /** Removes a book with its stock and cover. Removing a book that doesn't exist (any more) does nothing. */
    public void deleteBook(long id) {
        bookRepository.deleteById(id);
        inventoryRepository.deleteById(id);
        coverStorage.deleteById(id);
    }

    /** Numbers for the admin dashboard. */
    public CatalogStatsResponse getCatalogStats() {
        long outOfStock = inventoryRepository.findAll().stream().filter(inventory -> !inventory.isInStock()).count();
        return new CatalogStatsResponse(bookRepository.count(), categoryRepository.count(), outOfStock);
    }

    private Book findBookOrThrow(long id) {
        return bookRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.BOOK_NOT_FOUND,
                "Book %d was not found.".formatted(id)));
    }

    private Category findCategory(long id) {
        return categoryRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND,
                "Category %d was not found.".formatted(id)));
    }

    /** Optimistic concurrency: change the book only if nobody changed it since the client read it. */
    private static void checkVersion(Book book, long expectedVersion) {
        if (book.getVersion() != expectedVersion) {
            throw new BusinessException(ErrorCode.PRECONDITION_FAILED,
                    "Book %d has version %d, but the request expects version %d. Read the book again, then retry."
                            .formatted(book.getId(), book.getVersion(), expectedVersion));
        }
    }
}
