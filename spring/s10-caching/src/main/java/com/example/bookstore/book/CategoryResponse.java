package com.example.bookstore.book;

/** A category as the API shows it. */
public record CategoryResponse(Long id, String name) {

    static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getName());
    }
}
