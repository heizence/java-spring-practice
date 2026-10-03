# S10 · Caching

⏱ 2 sessions · week 12 · 2 exercises

Some reads happen thousands of times a minute and return the same answer each time: a popular book's details, the category menu on every page. A *cache* keeps such answers in memory, so the database doesn't compute them again. Done well, it reduces the load on the database and makes pages fast. Done carelessly, it shows customers stale prices, mixes up data, and hides bugs until the cache expires. In this module you cache the bookstore's catalog with Spring's cache abstraction and Caffeine, the standard in-memory cache for Java, and you fix three stale-cache bugs of the kind that reach production. The caching annotations are advice in a proxy, so everything you learned in S03 applies.

## Lesson

Each session, read the lesson sections for that day, then do the exercise.

| Session | Read | Then do |
|---|---|---|
| 1 | 1. Why cache · 2. Cache-aside · 3. Spring's cache abstraction · 4. Keys · 5. Caffeine | S10-01 |
| 2 | 6. Invalidation and expiry · 7. What not to cache · 8. Proxies again · 9. Local and shared caches · 10. Cache metrics | S10-02 |

### 1. Why cache

A cache stores the results of expensive work so that the next request can reuse them. Reading a `BookResponse` from the application's memory takes well under a microsecond; reading it from MySQL takes a network round trip, a query and the mapping to objects, about a millisecond when the database is idle, and much more under load.

A cache pays off when:

- the same data is **read much more often than it changes**, such as book details and categories;
- the result is **expensive to produce**: a query with joins, an aggregation, a call to another service;
- the business can **accept slightly stale data** for a short time.

It costs memory, it adds code that can go wrong, and every cached value can be out of date. So measure first: cache the few reads that really are hot, not everything.

### 2. The cache-aside pattern

Most application caches follow the *cache-aside* pattern. The application asks the cache first, and only on a *miss* (the value isn't there) does it read the database and put the result into the cache. The next request is a *hit*:

```java
public BookResponse findBook(long id) {
    BookResponse cached = cache.get(id);
    if (cached != null) {
        return cached;                              // hit
    }
    BookResponse book = loadFromDatabase(id);       // miss
    cache.put(id, book);
    return book;
}

public void changePrice(long id, long newPrice) {
    updateDatabase(id, newPrice);
    cache.evict(id);                                // the next read loads the new price
}
```

Writes go to the database, and then the cached entry is removed: it's *evicted* (or *invalidated*). Why evict instead of putting the new value into the cache? Because the database decides what's true. Two concurrent price changes can reach the cache in the opposite order to their commits, and a transaction that puts its value into the cache can still roll back. After an eviction, the next read loads whatever was really committed.

### 3. Spring's cache abstraction

Writing the code above around every method would repeat the same lines everywhere, which is the problem AOP solves (S03). Spring's *cache abstraction* does it with annotations. `@EnableCaching` on a configuration class turns it on: Spring then wraps every bean that has caching annotations in a proxy, whose advice runs the cache-aside steps:

```java
@Configuration(proxyBeanMethods = false)
@EnableCaching
public class CacheConfiguration { ... }
```

```java
@Cacheable(CacheNames.BOOKS)                                // look in "books" first; on a miss, run and store
public BookResponse findBook(long id) { ... }

@CacheEvict(cacheNames = CacheNames.BOOKS, key = "#id")     // after the method returns: remove that entry
public BookResponse changePrice(long id, long newPrice) { ... }
```

| Annotation | What the advice does |
|---|---|
| `@Cacheable` | Returns the cached value if there is one; otherwise runs the method and stores its result. If the method throws, nothing is stored. |
| `@CacheEvict` | Runs the method, then removes the entry with the key (`key = …`) or every entry of the cache (`allEntries = true`). If the method throws, nothing is removed, unless `beforeInvocation = true`. |
| `@CachePut` | Always runs the method and stores the result, replacing the cached one. Use it only when the stored value can't be wrong, which is rare inside a transaction (section 6). |
| `@Caching` | Groups several annotations of the same kind on one method: `@Caching(evict = {@CacheEvict(…), @CacheEvict(…)})`. |

Behind the annotations are two interfaces. A `CacheManager` holds named caches, and a `Cache` is a key-value store: `get`, `put`, `evict`, `clear`. Spring Boot creates a `CacheManager` for you when `@EnableCaching` is on and a cache library such as Caffeine is on the classpath, configured with `spring.cache.*` properties. When you define your own `CacheManager` bean, Boot's version backs off. Put `@EnableCaching` on a separate configuration class, not on the application class, so that test slices such as `@WebMvcTest` start without it.

### 4. Keys, conditions and sync

Each cached value is stored under a *key*. By default, Spring builds the key from the method's arguments only:

| Parameters | Default key |
|---|---|
| none | `SimpleKey.EMPTY`: the whole cache holds one value |
| one | the argument itself: `findBook(1)` stores under `1L` |
| several | a `SimpleKey` of all of them: `changePrice(1, 38000)` means `SimpleKey(1, 38000)` |

Two consequences cause real bugs:

- **The method name is not part of the key.** Two methods that share a cache and take the same arguments share their entries: one reads what the other stored.
- **With several parameters, name the one you mean.** `@CacheEvict(CacheNames.BOOKS)` on `changePrice(long id, long newPrice)` evicts the key `SimpleKey(1, 38000)`, which matches nothing, so the old price stays in the cache.

The attributes take *SpEL* expressions (Spring Expression Language), in which `#name` is a parameter:

```java
@Cacheable(cacheNames = "bestsellers", key = "#categoryId")              // only the category, not the limit
@Cacheable(cacheNames = "prices", condition = "#quantity < 100")         // decided before the call: big orders skip the cache
@Cacheable(cacheNames = "quotes", unless = "#result.isEmpty()")          // decided after the call: don't store empty results
@Cacheable(cacheNames = "exchangeRates", sync = true)                    // on a miss, one thread loads; the others wait
```

`sync = true` matters for expensive values that many requests want at once: without it, when the entry expires, every request that misses at that moment runs the method (a *cache stampede*). It can't be combined with `unless`.

### 5. Caffeine: a cache in the application's memory

[Caffeine](https://github.com/ben-manes/caffeine) is the usual cache for one Java process. The Spring adapter is the `CaffeineCacheManager`:

```java
@Bean
public CacheManager cacheManager(Ticker cacheTicker) {
    var cacheManager = new CaffeineCacheManager();
    cacheManager.setCaffeine(Caffeine.newBuilder()          // the settings of every cache it creates
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofMinutes(5))
            .ticker(cacheTicker)
            .recordStats());
    cacheManager.setCacheNames(List.of("orders", "exchangeRates"));   // create these now, and no others
    cacheManager.registerCustomCache("exchangeRates", Caffeine.newBuilder()   // its own settings, replacing the above
            .maximumSize(200)
            .expireAfterWrite(Duration.ofSeconds(30))
            .ticker(cacheTicker)
            .recordStats()
            .build());
    return cacheManager;
}
```

- **`maximumSize`**: the most entries the cache keeps. When it's full, Caffeine evicts the entries least likely to be used again. Every cache needs a limit; an unbounded one grows until the application runs out of memory.
- **`expireAfterWrite`**: an entry expires a fixed time after it was stored, however often it's read. That time is the *TTL* (time to live): the longest a reader can see stale data.
- **`expireAfterAccess`**: an entry expires a fixed time after it was last read or written. An entry that is read all the time never expires, so changes made elsewhere never show up. Use it for data that can't go stale, such as computed values that depend only on the key.
- **`recordStats`**: counts hits, misses and evictions, for metrics (section 10).
- **`setCacheNames`**: creates the listed caches right away, and no others later. Without it, the manager creates a cache for any name it's asked for, including names with typos.
- **`registerCustomCache`**: adds a cache with its own settings. Call it after `setCacheNames`, which gives every listed name a cache with the common settings, replacing a custom cache of the same name.
- **`ticker`**: Caffeine reads the time from a `Ticker`, by default `System.nanoTime()`. It never uses your `Clock` bean, so moving the `TestClock` doesn't expire anything. Make the ticker a bean, and tests can replace it with a fake one that moves when the test says so.

The TTL belongs to the cache, not to the annotation: Spring's abstraction has no per-entry expiry. A cache that needs another TTL is a separate cache with its own settings.

### 6. Invalidation and expiry

Entries leave a cache in two ways, and you need both:

- **Eviction on write** is exact and immediate, but it only covers the writes your application sees, through the methods that carry `@CacheEvict`.
- **Expiry after a TTL** covers everything else: a bulk `UPDATE` (S06-07), an SQL script, another service, or another instance of your own application (section 9). The TTL is the longest any of these can stay invisible.

Check every write path when you add a cache: admin tools, batch jobs, bulk updates and other methods that change the same data. A forgotten eviction shows up as "the change only appears after ten minutes", which is hard to reproduce.

**Caches and transactions.** A cache is not part of the database transaction. Rolling back doesn't undo a `put`, and an eviction that runs before the commit leaves a short window: another request can read the old row and cache it again. Two safeguards:

- Evict rather than put (section 2), and keep TTLs short.
- If needed, make the cache wait for the commit: Spring's `TransactionAwareCacheManagerProxy` wraps a `CacheManager` so that puts and evictions happen after the transaction commits.

**Caches and tests.** For the same reason, the rollback at the end of a test doesn't empty the caches. The bookstore's `IntegrationTestListener` clears every cache before each test.

### 7. What not to cache

- **Values that a decision depends on.** The stock when an order is placed, a wallet's balance before a payment, a coupon's remaining copies. Read them from the database, inside the transaction, with the locks of S08. A cached stock number on the book page is fine; a cached stock number in `placeOrder` oversells.
- **Per-user data**, such as a member's orders or cart. Each entry serves one person, so the hit rate is low and the memory use high. And a key mistake shows one member's data to another.
- **JPA entities.** A cached entity outlives the persistence context that loaded it: lazy associations then throw `LazyInitializationException`, and saving it later can overwrite newer data. Cache response records instead.
- **Mutable objects, including lists.** Caffeine stores a reference to the object and hands that same object to every caller; it doesn't copy. If one caller sorts or changes a cached `ArrayList`, every other caller sees the change. Cache records and unmodifiable lists (`stream().toList()`, `List.copyOf`).
- **Things that are cheap anyway**, or that are rarely asked for twice.

### 8. Proxies again

`@Cacheable` and `@CacheEvict` are advice in a proxy, so the rules of S03 apply:

- **Self-invocation skips the cache.** If `topReviews()` calls `findReviews()` of the same class, the call never reaches the proxy, and the database is queried every time. Move one of the methods into another bean.
- `private` and `final` methods, and objects created with `new`, are never cached.
- **Order with `@Transactional`.** A method can carry both annotations. If the cache advice is the outer layer, a cache hit doesn't even start a transaction, and an eviction happens after the commit. By default both have the lowest precedence, so their order is undefined; `@EnableCaching(order = …)` sets it.

### 9. Local caches and shared caches

Caffeine lives in the memory of one Java process. Run three instances of the bookstore behind a load balancer, and each has its own cache:

```text
admin ──► instance A: price changed, A evicts book 1      → A shows 38,000 won
customer ──► instance B: still has book 1 cached           → B shows 36,000 won until its TTL ends
```

A local cache is fast and simple, but other instances only see a change when their entry expires. Keep its TTL as short as that staleness is acceptable. A *shared cache* such as Redis keeps one copy of each entry for all instances, so an eviction reaches all of them. It costs a network call per read and needs values that can be serialized; in return, every read gets a fresh copy, so the shared-object bug of section 7 can't happen. (Redis is an elective after the core course.) Many systems combine both: a short local cache in front of a shared one.

### 10. Cache metrics

A cache is worth its memory only if it's hit often. The *hit ratio*, hits divided by all lookups, tells you whether it does. With `recordStats()`, Caffeine counts them:

```java
CacheStats stats = caffeineCache.stats();   // hitCount(), missCount(), evictionCount(), hitRate()
```

With Spring Boot Actuator (S14), these become Micrometer metrics per cache, such as `cache.gets` with a tag `result` of `hit` or `miss`, `cache.evictions` and `cache.size`. Actuator only finds the caches that exist at startup, which is one more reason to create them up front with `setCacheNames`. A low hit ratio means the cache costs memory and adds stale data without saving much work. To see hits and misses in the log, set `logging.level.org.springframework.cache` to `trace`.

## TypeScript/NestJS → Spring

| TypeScript/Node | Spring | Watch out for |
|---|---|---|
| a `Map` used as a cache, or the `lru-cache` package | Caffeine | Like `lru-cache`, Caffeine limits the size and expires entries; it also records statistics. A plain `HashMap` grows forever and isn't thread-safe. |
| NestJS `CacheInterceptor` on a controller | `@Cacheable` on a service method | NestJS caches `GET` responses by URL. Spring caches any bean method by its arguments. |
| `@CacheTTL(5000)` on a route | a TTL per cache, in the cache's configuration | Spring's annotations have no TTL attribute. |
| `cacheManager.get(key)`, `set`, `del` from `cache-manager` | `cacheManager.getCache(name)` with `get`, `put`, `evict` | Use the annotations; use the API only in special cases. |
| lodash `memoize(fn)` | `@Cacheable` | Both key by arguments only. `memoize` even uses only the first argument by default. |
| one Node process per container, each with its own memory | one JVM per instance, each with its own Caffeine cache | Instances don't see each other's evictions (section 9). |

## In real projects

- **Measure before you cache, and after.** Cache the few hot, rarely changing, expensive reads; watch the hit ratio. See [Logging and observability](../../docs/handbook/logging-and-observability.md).
- **Cache in the service layer**, where the use case and its transaction live, not in controllers or repositories. See [Spring layering](../../docs/handbook/spring-layering.md).
- **Cache response records, never entities or mutable collections.** See [JPA entities](../../docs/handbook/jpa-entities.md).
- **Name caches with constants, list them up front, and give each one a size limit and a TTL.** One cache per kind of value.
- **Evict on every write path**, including bulk updates and admin tools, and let the TTL catch the rest. Name the key (`key = "#id"`) whenever a method has more than one parameter.
- **Never base a decision on cached data:** stock, balances and limits are read in the transaction. See [Transactions](../../docs/handbook/transactions.md).
- **Remember the proxy:** self-invocation, `private` and `final` methods skip the cache.
- **Keep TTLs and sizes in configuration** (`@ConfigurationProperties`), so operations can tune them without a release. See [Configuration](../../docs/handbook/configuration.md).
- **Test caching:** count SQL statements to prove hits, control the cache's ticker, and clear caches between tests. See [Testing Spring applications](../../docs/handbook/spring-testing.md).

## Exercises

### S10-01 · Caching with Spring Cache

**Build** · 30 min · [`CacheConfiguration.java`](src/main/java/com/example/bookstore/common/CacheConfiguration.java) · [`BookService.java`](src/main/java/com/example/bookstore/book/BookService.java) · [`CategoryService.java`](src/main/java/com/example/bookstore/book/CategoryService.java)

**Ticket: cache the book details and the category menu.** At lunchtime, the database spends most of its time answering the same two questions: the details of the popular books, and the category list that every page shows. Both change rarely. Cache them in memory with Caffeine.

Acceptance criteria:

- Caching is turned on with one `CaffeineCacheManager`, configured as the Javadoc of `CacheConfiguration` says: exactly the caches in `CacheNames.ALL`, at most 1,000 entries each, kept 10 minutes after they were written, with statistics. The categories are kept for 1 hour. Every cache reads the time from the `cacheTicker` bean.
- `BookService.findBook` caches each `BookResponse` by book id: the second request for a book runs no SQL. A book that doesn't exist isn't cached.
- `BookService.changePrice` removes exactly that book from the cache after a successful change, so the next read shows the new price.
- `CategoryService.findCategories` caches the category list.

**Before you start:** lesson sections 1 to 5.

**You'll practice:** `@EnableCaching`, `CaffeineCacheManager` and Caffeine's builder, `@Cacheable`, `@CacheEvict` with a SpEL key, a ticker for testable expiry.

**Rules:** in the Javadoc of `CacheConfiguration`, `BookService` and `CategoryService`. The tests prove hits by counting SQL statements: a read that the cache answers runs none. They replace the `cacheTicker` with a `FakeTicker`, so they can move the cache's time forward.

<details>
<summary>Hint 1</summary>

`@EnableCaching` goes on the configuration class. Then add a `@Bean` method that returns a `CacheManager`, with the `Ticker` as a parameter. Lesson section 5 shows the three calls you need, and which one must come last.
</details>

<details>
<summary>Hint 2</summary>

Write a small private method that returns a configured `Caffeine.newBuilder()` for a given time to live. Then the default caches and the categories cache can't drift apart, and neither can forget the ticker. A custom cache needs `.build()`.
</details>

<details>
<summary>Hint 3</summary>

`changePrice(long id, long newPrice)` has two parameters. What's the default key then, and does it match the key under which `findBook` stored the book? (Lesson section 4.)
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Why does the cache hold `BookResponse` records and not `Book` entities? If someone changes a price directly in the database, how long can a customer see the old one?

### S10-02 · Stale cache bugs

**Fix** · 30 min · [`ReviewService.java`](src/main/java/com/example/bookstore/review/ReviewService.java) · [`InventoryService.java`](src/main/java/com/example/bookstore/inventory/InventoryService.java) · [`BookPageService.java`](src/main/java/com/example/bookstore/book/BookPageService.java) · [`CacheNames.java`](src/main/java/com/example/bookstore/common/CacheNames.java)

This ticket needs the caching of S10-01: without it nothing is cached, so nothing can be stale. Finish S10-01 first.

**Bugs: the book page shows wrong and stale data.** Since caching went live, support has received three reports about the page of *Effective Java* (book 1):

1. "The page says 15 copies are in stock, but the warehouse has 10." The page also says 15 reviews, which is right.
2. "The reviews tab should list the newest reviews first. Since the page started showing its three best reviews, the tab lists them by rating."
3. "I wrote a review, but the page still says 15 reviews, and mine isn't in the list."

Fix them without giving up the caching: the tests in "Still cached" check that the page still reads from the cache. The Javadoc of `ReviewService`, `InventoryService` and `BookPageService` describes the intended behavior. Each failing test shows one symptom. Fix the code, not the tests.

**Before you start:** lesson sections 4, 6, 7 and 8.

**You'll practice:** default cache keys, one cache per kind of value, immutable cached values, `@CacheEvict` with `@Caching` and SpEL keys.

<details>
<summary>Hint 1</summary>

Bug 1: which cache do `countReviews` and `copiesInStock` use, and what is the key of each of their entries? `BookPageService.stats` calls `countReviews` first. A new cache name also needs its place in `CacheNames.ALL`.
</details>

<details>
<summary>Hint 2</summary>

Bug 2: Caffeine doesn't copy what it stores. What does every caller of `findReviews` get, and what does `topReviews` do with it? Fix both sides: the cache should hold a list that nobody can change, and `topReviews` should build a new list.
</details>

<details>
<summary>Hint 3</summary>

Bug 3: which cached entries describe a book's reviews? `writeReview` must evict each of them; `@Caching` groups several `@CacheEvict` annotations.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Which of the three bugs could also happen with a shared Redis cache, where every read returns a fresh copy? Why is the stock risky to cache at all, and why is it acceptable on this page?

## Review questions

1. Describe the cache-aside pattern in three steps.
2. Why do applications usually evict an entry on a write instead of putting the new value into the cache?
3. What is the default key of a `@Cacheable` method with no parameters, with one, and with two? Is the method name part of it?
4. What's the difference between `condition` and `unless`?
5. When does `sync = true` help?
6. `expireAfterWrite` or `expireAfterAccess`: which one limits how stale an entry can get, and why?
7. Why shouldn't you cache JPA entities?
8. A `@Cacheable` method is called from another method of the same class. What happens?
9. Three instances of the bookstore use Caffeine with a 10-minute TTL. An admin changes a price on instance A. What do customers see on instances B and C, and for how long?
10. How do you find out whether a cache is worth its memory?
11. Why must tests of cached code clear the caches between tests, even though each test's transaction is rolled back?

<details>
<summary>Answers</summary>

1. Look in the cache first. On a miss, read the database and store the result in the cache. On a write, update the database and evict the entry, so that the next read loads the new value.
2. The database decides what's true. Concurrent writers can put their values into the cache in the opposite order to their commits, and a transaction can roll back after its put. After an eviction, the next read loads what was committed.
3. No parameters: `SimpleKey.EMPTY`, so one entry for the whole cache. One parameter: the argument itself. Two or more: a `SimpleKey` of all of them. The method name is not part of the key, so two methods that share a cache share entries.
4. `condition` is checked before the call and decides whether the cache is used at all for this call. `unless` is checked after the call, with the result available as `#result`, and decides whether the result is stored.
5. When a value is expensive and many requests ask for it at the same moment, such as right after it expired. With `sync = true`, one thread computes the value while the others wait for it, instead of all of them running the method (a cache stampede).
6. `expireAfterWrite`: an entry expires a fixed time after it was stored, so no reader sees data older than the TTL. With `expireAfterAccess`, an entry that is read often never expires, so it can stay stale forever.
7. A cached entity outlives the persistence context that loaded it. Lazy associations throw `LazyInitializationException`, callers can change the shared object, and saving it later can overwrite newer data. Cache response records instead.
8. The call doesn't go through the proxy, so the cache advice doesn't run: the method executes every time, and nothing is stored. Move the cached method into another bean.
9. Instance A evicts its own entry and shows the new price. B and C still have the old price cached and show it until their entries expire, up to 10 minutes after they were stored. A shared cache, or a shorter TTL, narrows that window.
10. Record statistics and watch the hit ratio, hits divided by all lookups, for example through Actuator's `cache.gets` metrics. Compare the database load and response times before and after.
11. The cache isn't part of the database transaction, so the rollback doesn't undo what a test put into it. A price that one test changed and cached would otherwise be what the next test reads.
</details>

## Further reading

- Spring Framework reference: [Cache Abstraction](https://docs.spring.io/spring-framework/reference/integration/cache.html), especially [Declarative annotation-based caching](https://docs.spring.io/spring-framework/reference/integration/cache/annotations.html) (keys, `condition`, `unless`, `sync`, and proxies)
- Spring Boot reference: [Caching](https://docs.spring.io/spring-boot/reference/io/caching.html) (the auto-configuration and the Caffeine properties) and [Cache metrics](https://docs.spring.io/spring-boot/reference/actuator/metrics.html#actuator.metrics.supported.cache)
- Caffeine wiki: [Eviction](https://github.com/ben-manes/caffeine/wiki/Eviction) (size, expiry, and testing with a `Ticker`) and [Efficiency](https://github.com/ben-manes/caffeine/wiki/Efficiency) (how it chooses what to evict)
- NestJS: [Caching](https://docs.nestjs.com/techniques/caching), to compare with Spring's approach
