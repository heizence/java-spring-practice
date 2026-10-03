package com.example.bookstore.common;

import java.time.Duration;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
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
@EnableCaching // creates the proxies that run @Cacheable and @CacheEvict, like the aspects of S03
public class CacheConfiguration {

    private static final long MAXIMUM_SIZE = 1_000;
    private static final Duration TIME_TO_LIVE = Duration.ofMinutes(10);
    private static final Duration CATEGORIES_TIME_TO_LIVE = Duration.ofHours(1);

    /**
     * The clock Caffeine reads to find out whether an entry has expired: {@link System#nanoTime()}. Caffeine doesn't
     * use the {@code Clock} bean, so tests replace this ticker with a fake one.
     */
    @Bean
    public Ticker cacheTicker() {
        return Ticker.systemTicker();
    }

    /**
     * Defining our own {@code CacheManager} makes Spring Boot's cache auto-configuration back off, so the
     * {@code spring.cache.*} properties don't apply: everything is configured here.
     */
    @Bean
    public CacheManager cacheManager(Ticker cacheTicker) {
        var cacheManager = new CaffeineCacheManager();
        // The settings of the caches that the manager creates itself.
        cacheManager.setCaffeine(caffeine(TIME_TO_LIVE, cacheTicker));
        // Creates these caches now, and from now on no others. (Without a list, the manager would create a cache for
        // any name it's asked for, typos included.)
        cacheManager.setCacheNames(CacheNames.ALL);
        // A cache with its own settings, replacing the categories cache created above. It must come after
        // setCacheNames, which gives every listed name a cache with the common settings.
        cacheManager.registerCustomCache(CacheNames.CATEGORIES,
                caffeine(CATEGORIES_TIME_TO_LIVE, cacheTicker).build());
        return cacheManager;
    }

    private static Caffeine<Object, Object> caffeine(Duration timeToLive, Ticker ticker) {
        return Caffeine.newBuilder()
                // A limit is a must: an unbounded cache grows until the application runs out of memory.
                .maximumSize(MAXIMUM_SIZE)
                // "After write", not "after access": an entry that is read all the time must still expire, or a
                // change made elsewhere (another instance, an SQL script) would never show up.
                .expireAfterWrite(timeToLive)
                // Every cache needs the ticker, also the custom one; otherwise it would read System.nanoTime().
                .ticker(ticker)
                .recordStats();
    }
}
