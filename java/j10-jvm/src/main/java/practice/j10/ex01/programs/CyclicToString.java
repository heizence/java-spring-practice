package practice.j10.ex01.programs;

import java.util.ArrayList;
import java.util.List;

/**
 * Prints a book. An author knows their books and a book knows its author: a two-way relationship, as in many
 * database models.
 */
public final class CyclicToString {

    private CyclicToString() {
    }

    public static void main(String[] args) {
        var author = new Author("Mina Park");
        var book = new Book("Java for Busy People", author);
        author.books.add(book);

        System.out.println(book);
    }

    static final class Author {
        final String name;
        final List<Book> books = new ArrayList<>();

        Author(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "Author[name=" + name + ", books=" + books + "]";
        }
    }

    static final class Book {
        final String title;
        final Author author;

        Book(String title, Author author) {
            this.title = title;
            this.author = author;
        }

        @Override
        public String toString() {
            return "Book[title=" + title + ", author=" + author + "]";
        }
    }
}
