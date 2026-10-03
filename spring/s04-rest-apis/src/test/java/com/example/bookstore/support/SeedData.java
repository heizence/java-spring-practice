package com.example.bookstore.support;

import com.example.bookstore.SeedDataLoader;
import com.example.bookstore.book.BookRepository;
import com.example.bookstore.book.CategoryRepository;
import com.example.bookstore.book.CoverStorage;
import com.example.bookstore.inventory.InventoryRepository;
import com.example.bookstore.member.MemberRepository;

/**
 * Puts the in-memory data back into the state the application starts with: the seed data of
 * {@code spring/BOOKSTORE.md}, no covers. {@link IntegrationTestListener} calls {@link #restore()} after every test.
 *
 * <p>From S05 on, the data lives in MySQL and the same helper drops and re-creates the tables instead.
 */
public final class SeedData {

    private final CategoryRepository categoryRepository;
    private final BookRepository bookRepository;
    private final InventoryRepository inventoryRepository;
    private final MemberRepository memberRepository;
    private final CoverStorage coverStorage;
    private final SeedDataLoader seedDataLoader;

    public SeedData(CategoryRepository categoryRepository, BookRepository bookRepository,
                    InventoryRepository inventoryRepository, MemberRepository memberRepository,
                    CoverStorage coverStorage, SeedDataLoader seedDataLoader) {
        this.categoryRepository = categoryRepository;
        this.bookRepository = bookRepository;
        this.inventoryRepository = inventoryRepository;
        this.memberRepository = memberRepository;
        this.coverStorage = coverStorage;
        this.seedDataLoader = seedDataLoader;
    }

    /** Empties every repository, then loads the seed data again. Ids start again after the seed data. */
    public void restore() {
        coverStorage.deleteAll();
        memberRepository.deleteAll();
        inventoryRepository.deleteAll();
        bookRepository.deleteAll();
        categoryRepository.deleteAll();
        seedDataLoader.load();
    }
}
