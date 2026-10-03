package com.example.bookstore.book;

import org.springframework.util.Assert;

/**
 * A book category, such as Programming or Fiction. Names are unique.
 *
 * <p>A record, because a category never changes. From S06 on it is a JPA entity in a table of its own.
 *
 * @param id   assigned by {@link CategoryRepository#save}; {@code null} before that
 * @param name the category's name, without surrounding whitespace
 */
public record Category(Long id, String name) {

    public Category {
        Assert.hasText(name, "name must not be blank");
        name = name.strip();
    }

    /** A category that isn't saved yet. */
    public static Category create(String name) {
        return new Category(null, name);
    }
}
