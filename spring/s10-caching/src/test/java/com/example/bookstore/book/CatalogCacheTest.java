package com.example.bookstore.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;

import com.example.bookstore.common.CacheNames;
import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.support.FakeTicker;
import com.example.bookstore.support.IntegrationTest;
import com.example.bookstore.support.SqlStatementCounter;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Proves cache hits by counting SQL statements: a read that the cache answers runs none. The seed data used here
 * (spring/BOOKSTORE.md): book 1 is Effective Java, 36,000 won; there are six categories.
 */
@IntegrationTest
@DisplayName("S10-01 · Caching with Spring Cache")
class CatalogCacheTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private BookService bookService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private SqlStatementCounter sqlStatements;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private FakeTicker cacheTicker;

    /** A provider, because there's no CacheManager bean until caching is turned on. */
    @Autowired
    private ObjectProvider<CacheManager> cacheManagerProvider;

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("GET /api/books/1 reads the book from the database once, then from the cache")
        void cachesABook() {
            assertThat(mvc.get().uri("/api/books/1")).hasStatusOk()
                    .bodyJson().extractingPath("$.title").isEqualTo("Effective Java");
            assertThat(sqlStatements.count()).as("%s", sqlStatements).isEqualTo(1);
            sqlStatements.reset();

            assertThat(mvc.get().uri("/api/books/1")).hasStatusOk()
                    .bodyJson().extractingPath("$.price").asNumber().isEqualTo(36_000);

            assertThat(sqlStatements.count()).as("%s", sqlStatements).isZero();
        }

        @Test
        @DisplayName("caches each book under its own id")
        void cachesEachBookByItsId() {
            bookService.findBook(1);
            BookResponse second = bookService.findBook(2);
            assertThat(second.id()).isEqualTo(2);
            sqlStatements.reset();

            assertThat(bookService.findBook(1).id()).isEqualTo(1);
            assertThat(bookService.findBook(2).id()).isEqualTo(2);

            assertThat(sqlStatements.count()).as("%s", sqlStatements).isZero();
        }

        @Test
        @DisplayName("a price change removes the book from the cache, so the next read shows the new price")
        void evictsOnAPriceChange() {
            bookService.findBook(1);
            bookService.changePrice(1, 38_000);
            entityManager.flush(); // write the change now, so that only the next read is counted
            sqlStatements.reset();

            BookResponse book = bookService.findBook(1);

            assertThat(book.price()).isEqualTo(38_000);
            assertThat(sqlStatements.count()).as("read again from the database: %s", sqlStatements).isEqualTo(1);
        }

        @Test
        @DisplayName("GET /api/categories reads the categories from the database once, then from the cache")
        void cachesTheCategories() {
            assertThat(mvc.get().uri("/api/categories")).hasStatusOk()
                    .bodyJson().extractingPath("$[*].name").asArray().containsExactly(
                            "Programming", "Databases", "Software Architecture", "Fiction", "Science", "Self-Help");
            sqlStatements.reset();

            assertThat(mvc.get().uri("/api/categories")).hasStatusOk()
                    .bodyJson().extractingPath("$[*].id").asArray().containsExactly(1, 2, 3, 4, 5, 6);

            assertThat(sqlStatements.count()).as("%s", sqlStatements).isZero();
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("a price change removes only the changed book from the cache")
        void evictsOnlyTheChangedBook() {
            bookService.findBook(1);
            bookService.findBook(2);
            bookService.changePrice(1, 38_000);
            entityManager.flush();
            sqlStatements.reset();

            bookService.findBook(2);

            assertThat(sqlStatements.count()).as("book 2 is still cached: %s", sqlStatements).isZero();
        }

        @Test
        @DisplayName("doesn't cache a book that doesn't exist")
        void doesNotCacheAMissingBook() {
            assertThatThrownBy(() -> bookService.findBook(9_999)).isInstanceOf(BusinessException.class);
            sqlStatements.reset();

            assertThatThrownBy(() -> bookService.findBook(9_999)).isInstanceOf(BusinessException.class);

            assertThat(sqlStatements.count()).as("looked up again: %s", sqlStatements).isEqualTo(1);
        }

        @Test
        @DisplayName("keeps a book for 10 minutes after it was read from the database")
        void booksExpireAfterTenMinutes() {
            bookService.findBook(1);
            cacheTicker.advance(Duration.ofMinutes(9));
            sqlStatements.reset();

            bookService.findBook(1);
            assertThat(sqlStatements.count()).as("after 9 minutes: %s", sqlStatements).isZero();

            cacheTicker.advance(Duration.ofMinutes(2));
            bookService.findBook(1);
            assertThat(sqlStatements.count()).as("after 11 minutes: %s", sqlStatements).isEqualTo(1);
        }

        @Test
        @DisplayName("keeps the categories for 1 hour after they were stored, even when they're read in between")
        void categoriesExpireAfterOneHour() {
            categoryService.findCategories();
            cacheTicker.advance(Duration.ofMinutes(59));
            sqlStatements.reset();

            categoryService.findCategories();
            assertThat(sqlStatements.count()).as("after 59 minutes: %s", sqlStatements).isZero();

            cacheTicker.advance(Duration.ofMinutes(2));
            List<CategoryResponse> categories = categoryService.findCategories();
            assertThat(sqlStatements.count()).as("after 61 minutes: %s", sqlStatements).isEqualTo(1);
            assertThat(categories).hasSize(6);
        }

        @Test
        @DisplayName("has exactly the caches listed in CacheNames.ALL, and no cache for an unknown name")
        void hasExactlyTheListedCaches() {
            CacheManager manager = cacheManager();

            assertThat(manager.getCacheNames()).containsExactlyInAnyOrderElementsOf(CacheNames.ALL);
            assertThat(manager.getCache("bokos")).as("a cache name with a typo").isNull();
        }

        @ParameterizedTest(name = "{0}", quoteTextArguments = false)
        @MethodSource("com.example.bookstore.book.CatalogCacheTest#cacheNames")
        @DisplayName("every cache keeps at most 1,000 entries and records statistics")
        void limitsEveryCacheAndRecordsStatistics(String name) {
            var cache = nativeCache(name);

            assertThat(cache.policy().eviction()).as("a size limit").hasValueSatisfying(
                    eviction -> assertThat(eviction.getMaximum()).isEqualTo(1_000));
            assertThat(cache.policy().isRecordingStats()).as("records statistics").isTrue();
        }

        @Test
        @DisplayName("records hits and misses, so that metrics can show how well the cache works")
        void recordsStatistics() {
            var books = nativeCache(CacheNames.BOOKS);
            CacheStats before = books.stats();

            bookService.findBook(1);
            bookService.findBook(1);

            CacheStats stats = books.stats().minus(before);
            assertThat(stats.missCount()).as("misses").isEqualTo(1);
            assertThat(stats.hitCount()).as("hits").isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("a price change that fails keeps the book in the cache")
        void keepsTheBookWhenTheChangeFails() {
            bookService.findBook(1);
            assertThatThrownBy(() -> bookService.changePrice(1, 0)).isInstanceOf(IllegalArgumentException.class);
            sqlStatements.reset();

            BookResponse book = bookService.findBook(1);

            assertThat(book.price()).isEqualTo(36_000);
            assertThat(sqlStatements.count()).as("still cached: %s", sqlStatements).isZero();
        }
    }

    static List<String> cacheNames() {
        return CacheNames.ALL;
    }

    private CacheManager cacheManager() {
        CacheManager manager = cacheManagerProvider.getIfAvailable();
        assertThat(manager).as("the CacheManager bean; is caching turned on?").isNotNull();
        return manager;
    }

    /** The Caffeine cache behind one of Spring's caches. */
    private com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache(String name) {
        Cache cache = cacheManager().getCache(name);
        assertThat(cache).as("the cache %s", name).isInstanceOf(CaffeineCache.class);
        return ((CaffeineCache) cache).getNativeCache();
    }
}
