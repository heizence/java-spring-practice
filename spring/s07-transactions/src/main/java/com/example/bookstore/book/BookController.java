package com.example.bookstore.book;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The catalog: {@code GET /api/books} and {@code GET /api/books/{id}}. */
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    /**
     * A page of books: {@code GET /api/books?page=0&size=20&sort=price,desc}. Returning {@link PagedModel} instead
     * of a {@code Page} gives the stable JSON shape {@code {"content": [...], "page": {...}}}.
     */
    @GetMapping
    public PagedModel<BookResponse> getBooks(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return new PagedModel<>(bookService.findBooks(pageable));
    }

    @GetMapping("/{id}")
    public BookResponse getBook(@PathVariable long id) {
        return bookService.findBook(id);
    }
}
