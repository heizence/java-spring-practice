package com.example.bookstore.book;

import java.util.List;

import com.example.bookstore.common.CacheNames;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Use cases for the category menu, which every page of the store shows. */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * Every category, by id. Every page needs them and they rarely change, so they are cached in
     * {@link com.example.bookstore.common.CacheNames#CATEGORIES}, for an hour.
     */
    // No parameters: the whole list is cached under one key, SimpleKey.EMPTY. Its time to live comes from the cache
    // (CacheConfiguration), not from this annotation: Spring's abstraction has no per-entry expiry.
    @Cacheable(CacheNames.CATEGORIES)
    @Transactional(readOnly = true)
    public List<CategoryResponse> findCategories() {
        return categoryRepository.findAll(Sort.by("id")).stream()
                .map(CategoryResponse::from)
                .toList();
    }
}
