package com.example.bookstore.support;

import com.example.bookstore.SeedDataLoader;
import com.example.bookstore.book.BookRepository;
import com.example.bookstore.book.CategoryRepository;
import com.example.bookstore.book.CoverStorage;
import com.example.bookstore.inventory.InventoryRepository;
import com.example.bookstore.member.MemberRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * The test helpers that {@link IntegrationTest} adds to the application context. They are beans, so each
 * application context has its own and tests get them with {@code @Autowired}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class IntegrationTestConfiguration {

    /** Replaces the application's clock wherever a {@code Clock} is injected. */
    @Bean
    @Primary
    TestClock testClock() {
        return new TestClock();
    }

    @Bean
    SeedData seedData(CategoryRepository categoryRepository, BookRepository bookRepository,
                      InventoryRepository inventoryRepository, MemberRepository memberRepository,
                      CoverStorage coverStorage, SeedDataLoader seedDataLoader) {
        return new SeedData(categoryRepository, bookRepository, inventoryRepository, memberRepository, coverStorage,
                seedDataLoader);
    }
}
