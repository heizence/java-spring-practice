package com.example.bookstore.book;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Use cases for browsing the catalog. */
@Service
@RequiredArgsConstructor
public class BookService {

    private final BookSearchRepository bookSearchRepository;

    /**
     * The search box: up to {@value BookSearchRepository#MAX_RESULTS} books whose title or author contains
     * {@code text}. Spaces before and after the text don't count.
     *
     * @see BookSearchRepository#search
     */
    @Transactional(readOnly = true)
    public List<Book> search(String text, String sort) {
        return bookSearchRepository.search(text.strip(), sort);
    }
}
