package com.example.bookstore.book;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import com.example.bookstore.common.timing.Timed;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * Use cases for browsing the catalog.
 *
 * <p>S03 has no database, so the catalog lives in memory: the twelve Programming books of the seed data
 * ({@code spring/BOOKSTORE.md}). From S05 on, the whole catalog lives in MySQL.
 */
@Service
public class BookService {

    private static final List<Book> PROGRAMMING_SHELF = List.of(
            new Book(1, "9791115881394", "Effective Java", "Joshua Bloch", "Programming", 36_000),
            new Book(2, "9791169879736", "Clean Code", "Robert C. Martin", "Programming", 12_000),
            new Book(3, "9791141707538", "Refactoring", "Martin Fowler", "Programming", 37_000),
            new Book(4, "9791156815181", "The Pragmatic Programmer", "David Thomas", "Programming", 46_000),
            new Book(5, "9791129405579", "Java Concurrency in Practice", "Brian Goetz", "Programming", 39_000),
            new Book(6, "9791198716897", "Modern Java in Action", "Raoul-Gabriel Urma", "Programming", 43_000),
            new Book(7, "9791169788090", "Head First Design Patterns", "Eric Freeman", "Programming", 26_000),
            new Book(8, "9791181967275", "Spring in Action", "Craig Walls", "Programming", 26_000),
            new Book(9, "9791177848649", "Test-Driven Development by Example", "Kent Beck", "Programming", 47_000),
            new Book(10, "9791176634403", "Working Effectively with Legacy Code", "Michael Feathers", "Programming",
                    14_000),
            new Book(11, "9791197875342", "Code Complete", "Steve McConnell", "Programming", 56_000),
            new Book(12, "9791176804578", "The Art of Computer Programming", "Donald Knuth", "Programming", 18_000));

    private final Map<Long, Book> booksById = PROGRAMMING_SHELF.stream()
            .collect(Collectors.toUnmodifiableMap(Book::id, Function.identity()));

    /** @throws BusinessException {@link ErrorCode#BOOK_NOT_FOUND} if no book has this id */
    public Book findBook(long id) {
        Book book = booksById.get(id);
        if (book == null) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND, "Book %d was not found.".formatted(id));
        }
        return book;
    }

    /**
     * The books whose title or author contains the keyword, ignoring letter case, ordered by id. Empty if none
     * matches.
     *
     * @throws IllegalArgumentException if the keyword is blank
     */
    @Timed("book.search")
    public List<Book> search(String keyword) {
        Assert.hasText(keyword, "keyword must not be blank");
        String needle = keyword.strip().toLowerCase(Locale.ROOT);
        return booksById.values().stream()
                .filter(book -> contains(book.title(), needle) || contains(book.author(), needle))
                .sorted(Comparator.comparingLong(Book::id))
                .toList();
    }

    private static boolean contains(String text, String lowerCaseNeedle) {
        return text.toLowerCase(Locale.ROOT).contains(lowerCaseNeedle);
    }
}
