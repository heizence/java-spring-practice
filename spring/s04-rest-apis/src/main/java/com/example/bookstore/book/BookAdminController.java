package com.example.bookstore.book;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The admin API for the catalog. Everything under {@code /api/admin} is for the store's staff only:
 * {@code AdminTokenInterceptor} checks the admin token before these methods run.
 */
@RestController
@RequestMapping("/api/admin/books")
@RequiredArgsConstructor
public class BookAdminController {

    private final BookService bookService;

    @GetMapping("/stats")
    public CatalogStatsResponse getStats() {
        return bookService.getCatalogStats();
    }
}
