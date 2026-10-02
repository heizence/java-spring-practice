package com.example.bookstore.book;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The catalog's search box: {@code GET /api/books/search?q=java&sort=price,desc}. */
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    /**
     * Finds books whose title or author contains {@code q}, ignoring letter case.
     *
     * @param q    the text to look for, 1 to 100 characters
     * @param sort {@code title} (the default), {@code price} or {@code publishedDate}, optionally followed by
     *             {@code ,asc} (the default) or {@code ,desc}
     * @return up to {@value BookSearchRepository#MAX_RESULTS} books as a JSON array; {@code 400 Bad Request} for a
     *         blank {@code q} or an unknown {@code sort}
     */
    @GetMapping("/search")
    public List<Book> search(@RequestParam @NotBlank @Size(max = 100) String q,
                             @RequestParam(defaultValue = "title") String sort) {
        return bookService.search(q, sort);
    }
}
