package com.example.bookstore.support;

import org.flywaydb.core.Flyway;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Puts the database back into the state that the Flyway migrations create: the schema, the seed data, and whatever
 * the module's own later migrations add.
 *
 * <p>Most integration tests don't need this: {@link IntegrationTest} runs each test in a transaction that is rolled
 * back. Use it in tests that must commit, for example to test concurrent requests or after-commit listeners:
 *
 * <pre>{@code
 * @IntegrationTest
 * @Transactional(propagation = Propagation.NOT_SUPPORTED) // these tests commit
 * class StockConcurrencyTest {
 *
 *     @Autowired SeedData seedData;
 *
 *     @AfterEach
 *     void restoreSeedData() {
 *         seedData.restore();
 *     }
 * }
 * }</pre>
 *
 * <p>{@link #restore()} drops every table and runs the migrations again (Flyway clean, then migrate), which takes
 * well under a second. Afterwards ids start again from the seed data's next values. If the module caches data (for
 * example with {@code @Cacheable}), clear those caches too.
 */
public final class SeedData {

    private final Flyway flyway;

    /**
     * Copies the application's Flyway configuration. Spring Boot keeps {@code clean} disabled, so that nobody can
     * drop a real database by mistake; only this test-only copy enables it.
     */
    public SeedData(Flyway applicationFlyway) {
        this.flyway = Flyway.configure()
                .configuration(applicationFlyway.getConfiguration())
                .cleanDisabled(false)
                .load();
    }

    /**
     * Drops every table and runs all migrations again.
     *
     * @throws IllegalStateException if called inside a transaction. That transaction could hold locks on the tables
     *                               that {@code clean} drops, and the two would wait for each other forever.
     *                               Annotate the test class with
     *                               {@code @Transactional(propagation = Propagation.NOT_SUPPORTED)}.
     */
    public void restore() {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("SeedData.restore() can't run inside a transaction. Annotate the test "
                    + "class with @Transactional(propagation = Propagation.NOT_SUPPORTED), so its tests commit.");
        }
        flyway.clean();
        flyway.migrate();
    }
}
