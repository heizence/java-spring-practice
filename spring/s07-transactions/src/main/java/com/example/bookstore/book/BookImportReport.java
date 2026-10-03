package com.example.bookstore.book;

import java.util.List;

/**
 * What a book import did.
 *
 * @param totalRows    how many rows the import received
 * @param importedRows how many rows were saved: all the rows of the chunks that were committed
 * @param failedChunks the chunks that were rolled back, in the order they ran; empty if every chunk was saved
 */
public record BookImportReport(int totalRows, int importedRows, List<FailedChunk> failedChunks) {

    public BookImportReport {
        failedChunks = List.copyOf(failedChunks);
    }

    /**
     * A chunk that was rolled back: none of its rows were saved.
     *
     * @param firstRow the number of its first row. Rows are numbered from 1, in the order they were given.
     * @param lastRow  the number of its last row
     * @param reason   the message of the exception that rolled the chunk back
     */
    public record FailedChunk(int firstRow, int lastRow, String reason) {
    }
}
