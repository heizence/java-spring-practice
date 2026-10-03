package com.example.bookstore.book;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

/**
 * The bookstore's catalog: the 60 books of the seed data in {@code spring/BOOKSTORE.md}, held in memory.
 *
 * <p>This module has no database yet, so this class stands in for one. S05 moves the books into MySQL, and S06
 * replaces this class with a Spring Data repository. It's a bean: {@code @Repository} is a {@code @Component} for
 * classes that load and store data, so component scanning finds it, and other beans get it through their
 * constructors.
 */
@Repository
public class BookCatalog {

    private static final String PROGRAMMING = "Programming";
    private static final String DATABASES = "Databases";
    private static final String ARCHITECTURE = "Software Architecture";
    private static final String FICTION = "Fiction";
    private static final String SCIENCE = "Science";
    private static final String SELF_HELP = "Self-Help";

    private static final List<Book> BOOKS = List.of(
            new Book(1, "9791115881394", "Effective Java", "Joshua Bloch", PROGRAMMING, 36_000),
            new Book(2, "9791169879736", "Clean Code", "Robert C. Martin", PROGRAMMING, 12_000),
            new Book(3, "9791141707538", "Refactoring", "Martin Fowler", PROGRAMMING, 37_000),
            new Book(4, "9791156815181", "The Pragmatic Programmer", "David Thomas", PROGRAMMING, 46_000),
            new Book(5, "9791129405579", "Java Concurrency in Practice", "Brian Goetz", PROGRAMMING, 39_000),
            new Book(6, "9791198716897", "Modern Java in Action", "Raoul-Gabriel Urma", PROGRAMMING, 43_000),
            new Book(7, "9791169788090", "Head First Design Patterns", "Eric Freeman", PROGRAMMING, 26_000),
            new Book(8, "9791181967275", "Spring in Action", "Craig Walls", PROGRAMMING, 26_000),
            new Book(9, "9791177848649", "Test-Driven Development by Example", "Kent Beck", PROGRAMMING, 47_000),
            new Book(10, "9791176634403", "Working Effectively with Legacy Code", "Michael Feathers", PROGRAMMING,
                    14_000),
            new Book(11, "9791197875342", "Code Complete", "Steve McConnell", PROGRAMMING, 56_000),
            new Book(12, "9791176804578", "The Art of Computer Programming", "Donald Knuth", PROGRAMMING, 18_000),
            new Book(13, "9791184726718", "Designing Data-Intensive Applications", "Martin Kleppmann", DATABASES,
                    53_000),
            new Book(14, "9791113627017", "Database Internals", "Alex Petrov", DATABASES, 12_000),
            new Book(15, "9791119331901", "High Performance MySQL", "Silvia Botros", DATABASES, 46_000),
            new Book(16, "9791152059978", "SQL Antipatterns", "Bill Karwin", DATABASES, 56_000),
            new Book(17, "9791137929845", "Database System Concepts", "Abraham Silberschatz", DATABASES, 17_000),
            new Book(18, "9791129486622", "Seven Databases in Seven Weeks", "Luc Perkins", DATABASES, 45_000),
            new Book(19, "9791116105505", "Learning SQL", "Alan Beaulieu", DATABASES, 13_000),
            new Book(20, "9791147517797", "SQL Performance Explained", "Markus Winand", DATABASES, 51_000),
            new Book(21, "9791183695794", "Transaction Processing", "Jim Gray", DATABASES, 26_000),
            new Book(22, "9791186032084", "MySQL Crash Course", "Rick Silva", DATABASES, 15_000),
            new Book(23, "9791137028388", "Domain-Driven Design", "Eric Evans", ARCHITECTURE, 20_000),
            new Book(24, "9791126468614", "Patterns of Enterprise Application Architecture", "Martin Fowler",
                    ARCHITECTURE, 36_000),
            new Book(25, "9791124776476", "Building Microservices", "Sam Newman", ARCHITECTURE, 23_000),
            new Book(26, "9791171884131", "Release It!", "Michael T. Nygard", ARCHITECTURE, 26_000),
            new Book(27, "9791175682054", "Clean Architecture", "Robert C. Martin", ARCHITECTURE, 22_000),
            new Book(28, "9791198319487", "Fundamentals of Software Architecture", "Mark Richards", ARCHITECTURE,
                    48_000),
            new Book(29, "9791149482147", "Implementing Domain-Driven Design", "Vaughn Vernon", ARCHITECTURE, 18_000),
            new Book(30, "9791105218544", "Enterprise Integration Patterns", "Gregor Hohpe", ARCHITECTURE, 49_000),
            new Book(31, "9791152441063", "Site Reliability Engineering", "Betsy Beyer", ARCHITECTURE, 28_000),
            new Book(32, "9791113041189", "Accelerate", "Nicole Forsgren", ARCHITECTURE, 48_000),
            new Book(33, "9791179906590", "Pride and Prejudice", "Jane Austen", FICTION, 25_000),
            new Book(34, "9791115846454", "Moby-Dick", "Herman Melville", FICTION, 15_000),
            new Book(35, "9791104541469", "Frankenstein", "Mary Shelley", FICTION, 32_000),
            new Book(36, "9791153801293", "The Great Gatsby", "F. Scott Fitzgerald", FICTION, 19_000),
            new Book(37, "9791185940694", "Jane Eyre", "Charlotte Brontë", FICTION, 20_000),
            new Book(38, "9791163632283", "Crime and Punishment", "Fyodor Dostoevsky", FICTION, 13_000),
            new Book(39, "9791188642304", "The Picture of Dorian Gray", "Oscar Wilde", FICTION, 13_000),
            new Book(40, "9791164529124", "The Time Machine", "H. G. Wells", FICTION, 47_000),
            new Book(41, "9791184585001", "진달래꽃", "김소월", FICTION, 52_000),
            new Book(42, "9791138265386", "운수 좋은 날", "현진건", FICTION, 29_000),
            new Book(43, "9791155778791", "A Brief History of Time", "Stephen Hawking", SCIENCE, 31_000),
            new Book(44, "9791162705865", "The Selfish Gene", "Richard Dawkins", SCIENCE, 14_000),
            new Book(45, "9791145842105", "Cosmos", "Carl Sagan", SCIENCE, 46_000),
            new Book(46, "9791194906681", "The Gene", "Siddhartha Mukherjee", SCIENCE, 45_000),
            new Book(47, "9791181126580", "Sapiens", "Yuval Noah Harari", SCIENCE, 28_000),
            new Book(48, "9791193244845", "On the Origin of Species", "Charles Darwin", SCIENCE, 34_000),
            new Book(49, "9791109765822", "Surely You're Joking, Mr. Feynman!", "Richard P. Feynman", SCIENCE, 58_000),
            new Book(50, "9791158220181", "The Elegant Universe", "Brian Greene", SCIENCE, 54_000),
            new Book(51, "9791105154385", "Silent Spring", "Rachel Carson", SCIENCE, 21_000),
            new Book(52, "9791121571166", "Atomic Habits", "James Clear", SELF_HELP, 28_000),
            new Book(53, "9791115771886", "Deep Work", "Cal Newport", SELF_HELP, 27_000),
            new Book(54, "9791179511176", "Thinking, Fast and Slow", "Daniel Kahneman", SELF_HELP, 31_000),
            new Book(55, "9791132568858", "Mindset", "Carol S. Dweck", SELF_HELP, 32_000),
            new Book(56, "9791176523738", "The 7 Habits of Highly Effective People", "Stephen R. Covey", SELF_HELP,
                    56_000),
            new Book(57, "9791156963271", "Getting Things Done", "David Allen", SELF_HELP, 48_000),
            new Book(58, "9791164515479", "Grit", "Angela Duckworth", SELF_HELP, 28_000),
            new Book(59, "9791143528148", "Range", "David Epstein", SELF_HELP, 30_000),
            new Book(60, "9791166085659", "Ultralearning", "Scott H. Young", SELF_HELP, 18_000));

    private final Map<Long, Book> booksById = BOOKS.stream()
            .collect(Collectors.toUnmodifiableMap(Book::id, Function.identity()));

    /** The book with this id, or empty if there is none. */
    public Optional<Book> findById(long id) {
        return Optional.ofNullable(booksById.get(id));
    }

    /** Every book, in id order, as an unmodifiable list. */
    public List<Book> findAll() {
        return BOOKS;
    }
}
