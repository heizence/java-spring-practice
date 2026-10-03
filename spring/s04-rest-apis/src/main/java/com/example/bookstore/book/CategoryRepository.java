package com.example.bookstore.book;

import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

import org.springframework.stereotype.Repository;

/**
 * Stores {@link Category categories} in memory. It stands in for the {@code categories} table, which arrives in S05,
 * and offers the same methods as the Spring Data repository of S06.
 *
 * <p>The methods are {@code synchronized}: Tomcat serves each request on its own thread, so several threads use this
 * one bean at the same time.
 */
@Repository
public class CategoryRepository {

    private final TreeMap<Long, Category> categories = new TreeMap<>();
    private long lastId;

    public synchronized Optional<Category> findById(long id) {
        return Optional.ofNullable(categories.get(id));
    }

    /** Every category, by id. */
    public synchronized List<Category> findAll() {
        return List.copyOf(categories.values());
    }

    /** Stores a new category and returns it with its id. */
    public synchronized Category save(Category category) {
        if (category.id() != null) {
            throw new IllegalArgumentException("Categories never change: " + category);
        }
        Category saved = new Category(++lastId, category.name());
        categories.put(saved.id(), saved);
        return saved;
    }

    public synchronized long count() {
        return categories.size();
    }

    /** Removes every category and starts the ids at 1 again, like SQL's {@code TRUNCATE}. */
    public synchronized void deleteAll() {
        categories.clear();
        lastId = 0;
    }
}
