package practice.j07.ex04;

import java.util.List;

/**
 * The outcome of an import: the books that were read, and the lines that couldn't be.
 *
 * @param books  the imported books, in file order
 * @param errors the problems, in file order
 */
public record ImportResult(List<Book> books, List<ImportError> errors) {

    public ImportResult {
        // Unmodifiable copies: nobody can change a result after it was created.
        books = List.copyOf(books);
        errors = List.copyOf(errors);
    }
}
