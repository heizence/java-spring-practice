package practice.j07.ex04;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * One row of a book import.
 *
 * @param isbn   the ISBN, as it appears in the file
 * @param title  the title
 * @param author the author; may be empty
 * @param price  the price, zero or more
 */
public record Book(String isbn, String title, String author, BigDecimal price) {

    public Book {
        Objects.requireNonNull(isbn, "isbn");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(author, "author");
        Objects.requireNonNull(price, "price");
    }
}
