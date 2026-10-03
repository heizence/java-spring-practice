package com.example.bookstore.book;

import java.util.ArrayList;
import java.util.List;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import com.example.bookstore.inventory.Inventory;
import com.example.bookstore.inventory.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

/** Imports new books from the publishers' nightly catalog file. */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookImportService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryRepository inventoryRepository;
    private final TransactionTemplate transactionTemplate;

    /**
     * Imports the rows in chunks: rows 1 to {@code chunkSize} form the first chunk, the next {@code chunkSize} rows
     * the second, and so on. The last chunk may be smaller.
     *
     * <ul>
     *   <li>Each chunk is saved in a transaction of its own, committed before the next chunk starts.</li>
     *   <li>If any row of a chunk fails, the whole chunk is rolled back: none of its rows are saved, not even the
     *       good ones. The report lists the chunk with the exception's message, and the import goes on with the next
     *       chunk.</li>
     *   <li>A failed chunk never undoes the chunks before or after it.</li>
     *   <li>An empty list imports nothing and reports nothing.</li>
     * </ul>
     *
     * @param rows      the rows to import, in the order of the file
     * @param chunkSize how many rows to save per transaction; at least 1
     * @return how many rows were imported, and which chunks failed and why
     * @throws IllegalArgumentException if {@code rows} is {@code null} or {@code chunkSize} is less than 1
     */
    // No @Transactional on this method: it would wrap every chunk in one big transaction. The template's
    // transactions would then join it (propagation REQUIRED), and the first failing chunk would mark the shared
    // transaction rollback-only, so in the end nothing at all would be saved.
    public BookImportReport importBooks(List<BookImportRow> rows, int chunkSize) {
        Assert.notNull(rows, "rows must not be null");
        Assert.isTrue(chunkSize >= 1, () -> "chunkSize must be at least 1: " + chunkSize);

        int importedRows = 0;
        List<BookImportReport.FailedChunk> failedChunks = new ArrayList<>();
        for (int start = 0; start < rows.size(); start += chunkSize) {
            List<BookImportRow> chunk = rows.subList(start, start + Math.min(chunkSize, rows.size() - start));
            int firstRow = start + 1;
            int lastRow = start + chunk.size();
            try {
                // One transaction per chunk: committed when the callback returns, rolled back when it throws.
                transactionTemplate.executeWithoutResult(status -> chunk.forEach(this::importRow));
                importedRows += chunk.size();
            } catch (RuntimeException e) {
                // The template has already rolled the chunk back. Catch the failure outside the transaction, never
                // inside it: a caught exception inside would let the chunk's good rows commit without the bad one.
                failedChunks.add(new BookImportReport.FailedChunk(firstRow, lastRow, e.getMessage()));
                log.warn("Book import: rolled back rows {} to {}: {}", firstRow, lastRow, e.getMessage());
            }
        }
        return new BookImportReport(rows.size(), importedRows, failedChunks);
    }

    /**
     * Saves one row: the book and its inventory. Given; call it inside a transaction, so that a chunk's rows are
     * saved or rolled back together.
     *
     * @throws BusinessException        {@link ErrorCode#CATEGORY_NOT_FOUND} for an unknown category,
     *                                  {@link ErrorCode#DUPLICATE_ISBN} if a book with the row's ISBN exists,
     *                                  including one saved earlier in the same transaction
     * @throws IllegalArgumentException if a value is invalid, such as a price of 0 or a negative stock
     */
    private void importRow(BookImportRow row) {
        Category category = categoryRepository.findByName(row.category())
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND,
                        "Category '%s' was not found.".formatted(row.category())));
        if (bookRepository.findByIsbn(row.isbn()).isPresent()) {
            throw new BusinessException(ErrorCode.DUPLICATE_ISBN,
                    "A book with ISBN %s already exists.".formatted(row.isbn()));
        }
        Book book = bookRepository.save(Book.create(row.isbn(), row.title(), row.author(), category, row.price(),
                row.publishedDate()));
        inventoryRepository.save(Inventory.of(book, row.stock()));
    }
}
