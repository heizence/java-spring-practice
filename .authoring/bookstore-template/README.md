# Bookstore template (maintainers only)

The shared starting point of every Part 2 module (S01–S14). Each module is a copy of this application at its own
checkpoint, so **copy from here and keep packages, names and style identical**. The domain, schema, seed facts and API
conventions are specified in [`spring/BOOKSTORE.md`](../../spring/BOOKSTORE.md); this file explains the code.

The template exists only on the `authoring` branch, as the Gradle project `:bookstore-template`.

## What it contains

```
build.gradle.kts            the starters (test JVM settings come from practice.spring-exercises)
compose.yaml                MySQL 9.7 for bootRun (Spring Boot's Docker Compose support connects to it)
src/main/java/com/example/bookstore/
├── BookstoreApplication.java
├── common/
│   ├── BaseTimeEntity.java              createdAt/updatedAt by JPA auditing; Javadoc: entity conventions
│   ├── ClockConfiguration.java          the Clock bean (system UTC, whole microseconds)
│   ├── JpaAuditingConfiguration.java    @EnableJpaAuditing + a DateTimeProvider backed by the Clock
│   └── error/
│       ├── ErrorCode.java               every API error: HTTP status + default message; the name is the `code`
│       ├── BusinessException.java       ErrorCode + detail message
│       ├── FieldViolation.java          one entry of `errors` ({field, message})
│       └── GlobalExceptionHandler.java  everything → RFC 9457 problem detail with `code`
├── member/      Member, Role, MemberRepository
├── book/        Book, Category, their repositories, and a small BookController/BookService/BookResponse
├── inventory/   Inventory (decrease/increase), InventoryRepository
├── order/       Order (addLine/removeLine, markPaid/ship/cancel), OrderLine, OrderStatus, repositories
├── wallet/      Wallet (deposit/withdraw), WalletTransaction, WalletTransactionType, repositories
├── coupon/      Coupon (issue), CouponIssue (use), repositories
├── review/      Review, ReviewRepository
└── payment/     Payment (approved/failed/refund), PaymentStatus, PaymentRepository
src/main/resources/
├── application.yml
└── db/migration/V1__create_schema.sql, V2__seed_data.sql   (V2 is generated: see "Seed data")
src/test/java/com/example/bookstore/
├── support/     test helpers that modules copy (below), plus their own tests (*Test.java)
└── …            the template's own tests, display names "Template · …" (the score report ignores them)
```

Test support (`src/test/java/com/example/bookstore/support/`):

| Class | What it does |
|---|---|
| `IntegrationTest` | Meta-annotation: `@SpringBootTest` + `@AutoConfigureMockMvc` (inject `MockMvcTester`) + MySQL + the helpers below + `@Transactional` (each test rolls back). |
| `TestcontainersConfiguration` | One MySQL 9.7 container per test JVM, connected through `@ServiceConnection`, about 140 MB of memory. |
| `IntegrationTestConfiguration` | The helper beans: `TestClock` (`@Primary`), `SqlStatementCounter` (registered with Hibernate), `SeedData`. |
| `IntegrationTestListener` | Puts the clock back before each test; empties the counter right before each test method. |
| `TestThreadGuard` | Fails a test whose methods run on another thread than its transaction (see "Must know"). |
| `TestClock` | A `Clock` that stands still at `2026-10-15T00:00:00Z` until a test moves it (`setInstant`, `advance`). |
| `SqlStatementCounter` | Counts Hibernate's SQL statements: `count()`, `statements()`, `reset()`, and a numbered `toString()` for messages. |
| `SeedData` | `restore()`: Flyway clean + migrate (clean is enabled only in this test-only copy of the configuration). |

## What each kind of module copies

| Module | Copy | Leave out |
|---|---|---|
| **S01–S04** (no database) | `BookstoreApplication`, `common/error/*`, `common/ClockConfiguration` if the module reads the time, `support/TestClock` if its tests control time, and from `application.yml` only `spring.application.name` and `spring.web.locale*`. | Entities and repositories (JPA annotations need JPA: model what the module needs as plain classes or records in the same packages), migrations, `compose.yaml`, the other test support. In `GlobalExceptionHandler`, delete `handlePropertyReference` (it needs Spring Data). |
| **S05** (JDBC) | As S01–S04, plus `db/migration/*`, `compose.yaml`, the datasource, Flyway and logging parts of `application.yml`, and `support/` without `SqlStatementCounter` (delete its two beans in `IntegrationTestConfiguration` and its reset in `IntegrationTestListener`). | Entities, repositories, `BaseTimeEntity`, `JpaAuditingConfiguration`, the `jpa` part of `application.yml`, `handlePropertyReference`. |
| **S06+** (JPA) | Everything except the template's own tests. | `BookController`/`BookService`/`BookResponse` if the module's exercises build them. |

The starters in `build.gradle.kts` follow the same split: S01–S04 need `webmvc` and `validation` (and their `-test`
starters); S05 adds `jdbc`, `flyway`, `flyway-mysql`, the MySQL driver, Docker Compose support and Testcontainers; S06+
use the template's list as it is.

## How to copy

From the repository root, for a JPA module:

```bash
m=spring/s06-spring-data-jpa
mkdir -p "$m"
cp .authoring/bookstore-template/{build.gradle.kts,compose.yaml} "$m"/
# The helpers, without the template's own tests (IntegrationTest.java is a helper despite its name).
rsync -a --include 'IntegrationTest.java' --exclude '*Test.java' .authoring/bookstore-template/src/ "$m"/src/
```

Then delete what the module doesn't need (table above), replace the classes that the exercises build with stubs, and
add the module's own tests and migrations (`V3__…`). Keep every package, class, method and property name. When a
module improves something that every module needs, change it here first and tell the other authors.

## Running the template

Docker must be running.

```bash
./gradlew :bookstore-template:test            # about 15 s: one container, three application contexts
./gradlew :bookstore-template:test --rerun    # run again even though nothing changed (hunting flaky tests)
./gradlew :bookstore-template:bootRun         # http://localhost:8080/api/books
```

## Data isolation in tests

- **`@IntegrationTest` rolls back.** Each test, its `@BeforeEach` methods and its MockMvc requests run in one
  transaction that is rolled back. Nothing to clean up.
- **Rolling back hides some behavior.** The test transaction keeps the persistence context open for the whole test:
  lazy loading in a controller works although `open-in-view` is off (no `LazyInitializationException`), entities
  loaded earlier don't hit the database again, and constraint violations surface only at a flush. Call
  `entityManager.flush()` and `clear()` before what you measure, and test such behavior without the test transaction.
- **Tests that must commit** (concurrent requests, `REQUIRES_NEW`, after-commit listeners, `@Async`) annotate the class
  or method with `@Transactional(propagation = Propagation.NOT_SUPPORTED)` and call `seedData.restore()` in
  `@AfterEach` (about 0.1–0.3 s; ids start again after the seed data). Deleting the rows the test created works too.
- **Seed data:** rely only on the facts in `BOOKSTORE.md`; read the ids of rows a test creates from the response.
- **Time:** application code injects `Clock`; tests start at `TestClock.DEFAULT_INSTANT` and may move the clock. Never
  call `Instant.now()` in application code, and remember that libraries with their own clocks (Spring Security's JWT
  validation, Caffeine) don't follow the `TestClock`.
- **Few contexts:** every class annotated exactly `@IntegrationTest` shares one cached context. `@MockitoBean`,
  `@TestPropertySource`, `@Import` or `@DirtiesContext` on a class create another one (seconds each). Slices such as
  `@DataJpaTest` must `@Import(TestcontainersConfiguration.class)` and, to save entities, `JpaAuditingConfiguration`
  and `ClockConfiguration` (`SharedContainerTest` shows one).
- **One container:** never declare the MySQL container with `@Testcontainers`/`@Container` and never stop it: the
  JUnit extension stops it after each class, while cached contexts still use it.

## Must know

- **Test methods run on the calling thread.** testkit's `junit-platform.properties` runs every test method in its own
  thread (`SEPARATE_THREAD`). Spring binds the test transaction to the thread that starts it, so in that mode
  `@Transactional` tests would commit instead of rolling back, and MockMvc requests would run outside the test
  transaction. `practice.spring-exercises` therefore switches every Spring module to `SAME_THREAD` (the 10-second
  timeout still applies after a method returns; the whole test run is limited to 10 minutes), and also silences a
  JDK 25 warning about `sun.misc.Unsafe` and skips Docker's credential helper. If the setting is ever missing,
  `TestThreadGuard` fails every `@IntegrationTest` with a message that says what to add.
- **Spring Boot 4 packages:** `org.springframework.boot.webmvc.test.autoconfigure.{AutoConfigureMockMvc, WebMvcTest}`,
  `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`,
  `org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer`,
  `org.springframework.data.core.PropertyReferenceException`, Testcontainers 2's `org.testcontainers.mysql.MySQLContainer`.
- **Problem details have no `type`** (Spring Framework 7 leaves `about:blank` out) and Jackson 3 writes their
  properties in alphabetical order, with `code` and `errors` last. `errors` is sorted by field, then message.
- **`GlobalExceptionHandler` catches every exception**, including Spring Security's `AccessDeniedException` and
  `AuthenticationException` thrown inside controllers. S12 must let those through (or map them to 401/403), or
  they become 500.
- **Foreign keys take locks.** Inserting a child row (wallet history, coupon issue, payment) takes a shared lock on its parent row; a later update of that parent in the same transaction can then deadlock with a concurrent request doing the same. Code that runs concurrently should lock the parent first (`SELECT … FOR UPDATE`) or retry on `ConcurrencyFailureException` (S08 shows both).
- **Business rules throw `BusinessException`** with an `ErrorCode`; invalid arguments that request validation should
  have stopped throw `IllegalArgumentException` (a 500). Add codes to `ErrorCode`; never rename one.
- **Entities:** protected no-argument constructor, no setters, factories that check their arguments, `LAZY`
  to-one associations, enums by name, `@Version` where the column exists, and identity `equals`/`hashCode` (the
  reasons are in `BaseTimeEntity`'s Javadoc). `Order` owns its lines; wallet history is saved by the caller
  (`walletTransactionRepository.save(wallet.withdraw(…))`).
- **Times** are `Instant`s in UTC everywhere: Hibernate (`hibernate.jdbc.time_zone`) and plain JDBC
  (`connectionTimeZone`) both read and write `DATETIME` columns as UTC. Read a `DATETIME` with JDBC as
  `rs.getTimestamp(…).toInstant()`; `JdbcClient` can't map it to `Instant` directly.
- **Validation messages are always English** (`spring.web.locale-resolver: fixed`), whatever `Accept-Language` says.
- **Lists return `PagedModel`** (`{"content": […], "page": {…}}`), never `Page`, whose JSON shape isn't stable.

## Seed data

`V2__seed_data.sql` is generated; never edit it by hand. Change `.authoring/bookstore/generate_seed.py`, run
`python3 .authoring/bookstore/generate_seed.py` from the repository root, and update `spring/BOOKSTORE.md` if a
documented fact changes. The script asserts the documented facts and the business rules the data must follow, and
keeps the order of its random draws, so that one change doesn't shuffle every other row.

## Troubleshooting

- **Testcontainers waits 30 s at the start** ("Failure when attempting to lookup auth config"): Docker's credential
  helper doesn't answer. For a run, export `DOCKER_AUTH_CONFIG='{"auths":{}}'` (the images are public).
- **`bootRun` fails with "port 8080 in use":** `./gradlew :bookstore-template:bootRun --args='--server.port=8081'`.
