package com.example.bookstore.common;

import com.github.benmanes.caffeine.cache.Ticker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring's cache abstraction, with Caffeine as the cache: the entries live in this application's memory.
 *
 * <p>A separate configuration class, like {@link JpaAuditingConfiguration}: web slice tests skip it, so they start
 * without caching.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>Caching is turned on: {@code @Cacheable} and {@code @CacheEvict} work on every bean.</li>
 *   <li>There is one {@code CacheManager}: a {@code CaffeineCacheManager} with exactly the caches in
 *       {@link CacheNames#ALL}, created at startup. Asking for any other cache name finds nothing, so a typo fails at
 *       once instead of quietly creating a new cache, and metrics (S14) can find every cache at startup.</li>
 *   <li>Every cache keeps at most 1,000 entries, keeps each entry for 10 minutes after it was written, and records
 *       hit and miss statistics, so that metrics can show how well it works.</li>
 *   <li>The one exception: the {@link CacheNames#CATEGORIES} cache keeps its entries for 1 hour after they were
 *       written, because categories rarely change. Otherwise it follows the same rules.</li>
 *   <li>Every cache reads the time from the {@link #cacheTicker()} bean, so that tests can move time forward.</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
// TODO: S10-01 — turn on Spring's caching
public class CacheConfiguration {

    /**
     * The clock Caffeine reads to find out whether an entry has expired: {@link System#nanoTime()}. Caffeine doesn't
     * use the {@code Clock} bean, so tests replace this ticker with a fake one.
     */
    @Bean
    public Ticker cacheTicker() {
        return Ticker.systemTicker();
    }

    // TODO: S10-01 — the CacheManager bean, following the rules above
}
