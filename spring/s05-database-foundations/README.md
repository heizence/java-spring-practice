# S05 · Database foundations

⏱ 3 sessions · week 9 · 3 exercises

From this module on, the bookstore keeps its data in MySQL. Before Spring Data JPA (S06) writes SQL for you, this module shows the layer underneath: how the application gets database connections, how SQL runs through Spring's `JdbcClient`, how Flyway creates and changes the schema, and how to keep user input out of your SQL. Every JPA problem you'll debug at work (a slow query, a connection pool that runs dry, a migration that fails in production, an injection hole in a native query) is easier to understand once you've written the SQL yourself. Read lesson sections 1–3 before S05-01, sections 4–7 before S05-02, and section 8 before S05-03.

## Lesson

### 1. The bookstore's database

MySQL 9.7 runs in Docker. [`spring/BOOKSTORE.md`](../BOOKSTORE.md) describes the tables and the seed data; [`V1__create_schema.sql`](src/main/resources/db/migration/V1__create_schema.sql) is the schema itself.

- **`./gradlew :spring:s05-database-foundations:bootRun`** reads [`compose.yaml`](compose.yaml): Spring Boot's Docker Compose support starts the MySQL container, points the application at it, and stops it when the application stops. The data lives in a Docker volume, so it survives restarts. Try `http://localhost:8080/api/books/search?q=java`, and compare `?q=O'Neil` before and after S05-03.
- **Tests** start their own, empty MySQL container with [Testcontainers](https://testcontainers.com/) ([`TestcontainersConfiguration`](src/test/java/com/example/bookstore/support/TestcontainersConfiguration.java)), one per test run. Flyway creates the schema and loads the seed data, and every test runs in a transaction that is rolled back at its end.
- **Why not an in-memory database such as H2?** It speaks a different SQL dialect and locks differently. A test that passes on H2 tells you little about MySQL, and S07 and S08 test exactly the locking behavior.

To look inside the `bootRun` database, open a MySQL shell in the container from the module folder:

```bash
cd spring/s05-database-foundations
docker compose exec mysql mysql -ubookstore -pbookstore bookstore
```

```sql
SHOW TABLES;
SHOW CREATE TABLE orders;
SELECT version, script, checksum, success FROM flyway_schema_history;
```

### 2. Flyway: the schema in versioned files

A *migration* is a SQL file that moves the schema one step forward. [Flyway](https://documentation.red-gate.com/flyway) runs the migrations in version order when the application starts, and records each one in the table `flyway_schema_history`.

```
src/main/resources/db/migration/
├── V1__create_schema.sql
├── V2__seed_data.sql
└── V3__create_wishlist_items.sql        ← S05-01
```

The file name is the contract: `V`, a version, **two** underscores, a description, `.sql`. Versions compare as numbers, part by part: `V2` < `V10`, and `V2_1` is version 2.1, between 2 and 3. In the description, underscores become spaces in the history table.

At every start, Flyway:

1. reads `flyway_schema_history` (and creates it in an empty database),
2. *validates*: every migration that ran before must still exist, unchanged,
3. runs the new ones in order, each recorded with a *checksum* of its content.

| installed_rank | version | description | script | checksum | success |
|---|---|---|---|---|---|
| 1 | 1 | create schema | V1__create_schema.sql | -1255204535 | 1 |
| 2 | 2 | seed data | V2__seed_data.sql | 1097266758 | 1 |

The rules that keep migrations safe:

- **Never edit a migration that has run anywhere:** on a teammate's laptop, in CI, in production. At the next start, its checksum no longer matches, and Flyway refuses to start: `Migration checksum mismatch for migration version 3`. And even without the check, the edit would never reach the databases that already ran the old version. To change the schema again, write the next migration, `V4__…`. This is called *fixing forward*.
- **Keep each migration small, with one change.** MySQL commits every `CREATE`, `ALTER` and `DROP` statement at once, so it can't roll back a migration. If the third statement fails, the first two stay done, Flyway marks the migration as failed, and someone repairs the database by hand. (Flyway has "undo" migrations only in its paid edition; teams fix forward instead.)
- **Migrations meet real data.** A migration that works on your empty test database can fail on a production table with a million rows. Adding a `NOT NULL` column needs a value for the existing rows:
  ```sql
  -- V4__add_books_review_count.sql: a new column with a value for every existing row
  ALTER TABLE books ADD COLUMN review_count INT NOT NULL DEFAULT 0;
  UPDATE books b SET b.review_count = (SELECT COUNT(*) FROM reviews r WHERE r.book_id = b.id);
  ```
  For changes that old and new application versions must survive together (renaming a column, for example), teams use *expand and contract*: add the new column, copy the data, deploy code that uses it, and drop the old column in a later migration.
- **Name your keys and constraints** (`uk_…`, `idx_…`, `fk_…`, as V1 does). Error messages show the names, and later migrations need them to change or drop a key.
- **Migrations are the only way the schema changes.** From S06 on, Hibernate runs with `ddl-auto: validate`: it checks that the entities match the tables and never changes them.

A *repeatable* migration has no version: `R__book_sales_view.sql`. Flyway runs it after the versioned ones, and again whenever its checksum changes. Use it for objects that you replace as a whole, such as views.

**In development:** if you edited a migration after `bootRun` ran it, Flyway stops with the checksum error above. Throw the local database away and start over: `docker compose down --volumes` in the module folder. Tests always start with an empty database, so they don't have this problem.

### 3. Constraints and indexes

Let the database enforce the rules it can: a check in Java can't see a second request that runs at the same moment, but a unique key stops the second insert whatever the code does.

```sql
CREATE TABLE coupon_issues (
    id        BIGINT      NOT NULL AUTO_INCREMENT,
    coupon_id BIGINT      NOT NULL,
    member_id BIGINT      NOT NULL,
    issued_at DATETIME(6) NOT NULL,
    used_at   DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_coupon_issues_coupon_member (coupon_id, member_id),
    KEY idx_coupon_issues_member (member_id),
    CONSTRAINT fk_coupon_issues_coupon FOREIGN KEY (coupon_id) REFERENCES coupons (id),
    CONSTRAINT fk_coupon_issues_member FOREIGN KEY (member_id) REFERENCES members (id)
);
```

| Clause | Means |
|---|---|
| `NOT NULL` | the column always has a value |
| `PRIMARY KEY (id)` + `AUTO_INCREMENT` | each row has a unique id, and MySQL generates it |
| `UNIQUE KEY uk_… (a, b)` | no two rows have the same `(a, b)`; also an index |
| `KEY idx_… (a)` | an *index*: a sorted copy of the column that makes `WHERE a = ?` fast; it enforces nothing |
| `CONSTRAINT fk_… FOREIGN KEY (a) REFERENCES t (id)` | every `a` must be an existing `t.id`, and that row can't be deleted while it's referenced |
| `DATETIME(6)` | a date and time with microseconds; without `(6)`, MySQL rounds them away |

An index on `(a, b)` also serves queries on `a` alone, but not on `b` alone, so the order of the columns matters. MySQL needs an index on every foreign key column and quietly creates one if you don't; V1 declares them, so their names and purpose are visible.

To check a schema with SQL, query MySQL's own description of it, the `information_schema` (the S05-01 tests do this):

```sql
SELECT column_name, column_type, is_nullable
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'coupon_issues';
```

`information_schema.statistics` lists the indexes, and `information_schema.key_column_usage` the foreign keys.

### 4. DataSource and the connection pool

Opening a connection to MySQL takes milliseconds: a TCP connection, TLS, a login, session setup. A request that opened and closed its own connection would spend more time on that than on its query. A *connection pool* opens a few connections once and lends them out.

`javax.sql.DataSource` is the JDK's interface for "something that hands out connections". With a JDBC starter on the classpath, Spring Boot creates a `DataSource` bean that is a [HikariCP](https://github.com/brettwooldridge/HikariCP) pool:

```java
try (Connection connection = dataSource.getConnection()) { // borrowed from the pool
    // ...
}                                                          // close() returns it to the pool; the TCP connection stays open
```

You rarely touch the `DataSource` yourself: `JdbcClient`, Spring's transactions and Hibernate all borrow from it. What you do decide is its size:

| Property | Default | Meaning |
|---|---|---|
| `spring.datasource.hikari.maximum-pool-size` | 10 | at most this many connections |
| `spring.datasource.hikari.connection-timeout` | 30s | how long a thread waits for a free connection before it fails |
| `spring.datasource.hikari.max-lifetime` | 30m | how long a connection lives before the pool replaces it |

A bigger pool isn't a faster one. MySQL works on each connection with its own thread, so beyond a few connections per CPU core of the *database server*, queries only wait for each other. And every instance of your service has its own pool: 8 instances × 10 connections = 80 connections, against MySQL's default limit of 151 (`max_connections`). Start small, measure, and read HikariCP's [About Pool Sizing](https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing).

A request holds a connection for as long as its transaction runs. When transactions are slow (a slow query, or a call to another service inside a transaction), all 10 connections are soon in use, and every other request waits until it fails: `Connection is not available, request timed out after 30000ms`. That's why transactions must stay short (S07).

### 5. JDBC in one minute

*JDBC* (Java Database Connectivity) is the JDK's low-level database API. Every Java database library uses it underneath: Hibernate, Spring's `JdbcClient`, jOOQ. The *driver*, here MySQL Connector/J (`com.mysql:mysql-connector-j`), implements it for one database.

```java
String sql = "SELECT id, title FROM books WHERE category_id = ? ORDER BY id";
try (Connection connection = dataSource.getConnection();
     PreparedStatement statement = connection.prepareStatement(sql)) {
    statement.setLong(1, categoryId);                    // parameters are numbered from 1
    try (ResultSet rows = statement.executeQuery()) {
        List<String> titles = new ArrayList<>();
        while (rows.next()) {                             // one row at a time
            titles.add(rows.getString("title"));
        }
        return titles;
    }
} catch (SQLException e) {                                // checked, with a vendor-specific error code
    throw new IllegalStateException("Could not read the books of category " + categoryId, e);
}
```

That's a lot of ceremony for one query: three resources to close, a checked `SQLException` whose meaning depends on the database vendor, and no connection to Spring's transactions. Spring's JDBC support takes care of all three.

### 6. JdbcClient

Spring Framework 6.1 added `JdbcClient`, a fluent API for SQL. Spring Boot creates a `JdbcClient` bean when `spring-boot-starter-jdbc` is on the classpath, so a repository injects it:

```java
@Repository
@RequiredArgsConstructor
public class CategoryJdbcRepository {

    private final JdbcClient jdbc;

    public Optional<String> findName(long id) {
        return jdbc.sql("SELECT name FROM categories WHERE id = :id")
                .param("id", id)
                .query(String.class)
                .optional();
    }
}
```

**Parameters.** Write `:name` in the SQL and bind a value with `.param("name", value)`. Positional `?` parameters work too (`.param(value)`), but names are easier to read and to get right. The same name may appear several times in one statement. A collection expands to a list: `WHERE id IN (:ids)` with `.param("ids", List.of(1L, 2L, 3L))`.

**Results.**

| Call | Returns | No row | Several rows |
|---|---|---|---|
| `.query(Long.class).single()` | the value | `EmptyResultDataAccessException` | `IncorrectResultSizeDataAccessException` |
| `.query(Long.class).optional()` | an `Optional` | `Optional.empty()` | `IncorrectResultSizeDataAccessException` |
| `.query(Book.class).list()` | a `List` | an empty list | all of them |
| `.query().listOfRows()` | `List<Map<String, Object>>` | an empty list | all of them |

**Rows into records.** `.query(Book.class)` creates one record per row and fills each component from the column of the same name, after turning `snake_case` into `camelCase`: the column `published_date` fills `publishedDate`. Use SQL aliases where the names differ (`c.name AS category`). A component without a matching column is an error, not a silent `null`. For anything else, pass a `RowMapper`, a lambda that builds one object from the current row:

```java
List<BookTitle> titles = jdbc.sql("SELECT id, title FROM books ORDER BY id")
        .query((row, rowNumber) -> new BookTitle(row.getLong("id"), row.getString("title")))
        .list();
```

**Types.** `BIGINT` ↔ `long`, `VARCHAR` ↔ `String` and `DATE` ↔ `LocalDate` work in both directions. `DATETIME(6)` ↔ `Instant` works when you *bind* an `Instant` (the bookstore tells the driver that the database stores UTC: `connectionTimeZone: UTC` in `application.yml`). For *reading*, `query(Instant.class)` fails with a type mismatch; read the column with `row.getTimestamp("created_at").toInstant()` in a `RowMapper`.

**Writes.** `update()` runs `INSERT`, `UPDATE` or `DELETE` and returns the number of rows the statement matched:

```java
int changed = jdbc.sql("UPDATE inventories SET quantity = quantity - :copies WHERE book_id = :bookId AND quantity >= :copies")
        .param("copies", 2)
        .param("bookId", 7L)
        .update();   // 1, or 0 when the book has fewer than 2 copies
```

To get the id that `AUTO_INCREMENT` generated, pass a `KeyHolder`:

```java
KeyHolder keyHolder = new GeneratedKeyHolder();
jdbc.sql("INSERT INTO categories (name) VALUES (:name)").param("name", "Poetry").update(keyHolder);
long id = keyHolder.getKey().longValue(); // MySQL Connector/J returns a BigInteger: convert it, don't cast it
```

**Transactions.** Inside a `@Transactional` method, `JdbcClient` uses the transaction's connection, so several statements commit or roll back together (S07). Outside one, each statement commits on its own.

**The older APIs.** `JdbcClient` runs on `JdbcTemplate` (positional `?`) and `NamedParameterJdbcTemplate` (`:name`), which you'll meet in older code: `jdbcTemplate.queryForObject("SELECT COUNT(*) FROM books", Long.class)`.

**Seeing the SQL.** Set `logging.level.org.springframework.jdbc.core.JdbcTemplate: debug` (the lines are already in `application.yml`, commented out).

### 7. DataAccessException: errors you can act on

A `SQLException` is checked and carries a vendor-specific error code: for MySQL, 1062 means "duplicate entry". `JdbcClient` translates it into an unchecked `DataAccessException` with the same meaning for every database. The most useful ones:

```
DataAccessException                           unchecked; the root of all of them
├── NonTransientDataAccessException           trying again gives the same error
│   ├── DataIntegrityViolationException       a constraint said no: NOT NULL, a foreign key, a value too long
│   │   └── DuplicateKeyException             a primary or unique key said no (MySQL error 1062)
│   ├── BadSqlGrammarException                a syntax error, an unknown table or column
│   └── IncorrectResultSizeDataAccessException  single() or optional() found the wrong number of rows
│       └── EmptyResultDataAccessException      single() found no row
└── TransientDataAccessException              trying again may work
    ├── QueryTimeoutException                 the statement ran too long
    └── PessimisticLockingFailureException    a lock wait timed out, or a deadlock (S08)
```

Don't catch them out of habit: most of them mean a bug or an outage, and the global exception handler turns them into a `500` and logs them. Catch one only when it's an expected outcome you can answer better. `DuplicateKeyException` is the usual case: two requests register the same email at the same time, both pass the "is it taken?" check, and the unique key stops the second one, which deserves a `409 Conflict` instead of a `500`.

(`@Repository` also asks Spring to translate exceptions. `JdbcClient` already throws translated ones, but the annotation matters for other data access code and documents the class's role.)

### 8. SQL injection

*SQL injection* happens when input becomes part of the SQL *text*, so it changes what the statement does:

```java
// DON'T: the request's text is copied into the SQL
String sql = "SELECT * FROM books WHERE title LIKE '%" + text + "%'";
```

| `text` from the request | The SQL that runs | Result |
|---|---|---|
| `Hitchhiker's` | `… LIKE '%Hitchhiker's%'` | a syntax error: a `500` for an honest customer |
| `' OR '1'='1` | `… LIKE '%' OR '1'='1%'` | every book |
| `x' UNION SELECT id, email, password_hash, … FROM members -- -` | `… LIKE '%x' UNION SELECT … FROM members -- -%'` | every member's email and password hash, shown as books |

(`-- ` starts a comment in MySQL, so everything after it is ignored. The last `-` keeps the space after `--` from being trimmed away.)

**Bound parameters** close the hole. The SQL text is fixed, and the driver sends the value as data, never as SQL, whatever characters it contains:

```java
jdbc.sql("SELECT … FROM books b WHERE b.title LIKE :pattern")
        .param("pattern", "%" + text + "%")
```

Escaping quotes yourself is not a fix. MySQL also treats a backslash as an escape character, so `\'` survives quote doubling; the driver knows every such rule for the connection's character set, you don't.

**What can't be a parameter.** Parameters carry *values*. Table names, column names and keywords such as `ASC` and `DESC` are part of the statement's structure. `ORDER BY :column` runs without an error, but it sorts by the same constant string for every row, so the order doesn't change. When the client chooses a sort column, translate its choice with an *allow list*, and build the SQL only from your own constants:

```java
private static final Map<String, String> SORT_COLUMNS = Map.of(
        "title", "b.title",
        "price", "b.price");

String column = SORT_COLUMNS.get(sortProperty);   // the request's value is only a key
if (column == null) {
    throw new BusinessException(ErrorCode.INVALID_INPUT, "Can't sort by '%s'.".formatted(sortProperty));
}
String sql = "SELECT … ORDER BY " + column + ", b.id";   // only constants are concatenated
```

**`LIKE` patterns.** Bind the whole pattern, wildcards included: `"%" + text + "%"`. A `%` or `_` in the user's text still works as a wildcard. That's harmless for a search box; when it matters, escape them (`\` → `\\`, `%` → `\%`, `_` → `\_`; the backslash is MySQL's default `LIKE` escape character).

The same rules apply to every way of writing SQL: JPQL and native queries in Spring Data (S06), and `WHERE` clauses that code assembles piece by piece. Append only constant SQL fragments, and keep every value a parameter. As a second line of defense, the application's database user should have only the privileges it needs.

## TypeScript/Node → Spring

| TypeScript / Node | Spring / Java | Watch out for |
|---|---|---|
| `mysql.createPool({ connectionLimit: 10 })` (mysql2) | the `DataSource` bean: a HikariCP pool that Spring Boot configures (`spring.datasource.hikari.maximum-pool-size`) | You never open or close a connection yourself. |
| `await pool.query('SELECT … WHERE id = ?', [id])` | `jdbc.sql("SELECT … WHERE id = :id").param("id", id).query(Book.class).optional()` | The call blocks; there's no `await`. |
| `rows[0]`, `rows.map(…)` | `.single()`, `.optional()`, `.list()` | `single()` throws unless there's exactly one row. |
| `result.insertId` | `update(keyHolder)`, then `keyHolder.getKey().longValue()` | `update()` alone returns the row count, not the id. |
| `result.affectedRows` | the `int` that `update()` returns | MySQL Connector/J counts the rows that matched. |
| `` `… LIKE '%${q}%'` `` | `.param("pattern", "%" + q + "%")` | A template string is SQL injection in both languages. |
| knex migrations: `exports.up` / `exports.down` in JavaScript | Flyway: `V3__create_wishlist_items.sql` | No down migrations: fix forward with the next version. |
| `knex migrate:latest` in the deploy script | Flyway runs at application start | |
| the `knex_migrations` table | `flyway_schema_history`, with a checksum per file | Editing an applied migration stops the application. |
| `err.code === 'ER_DUP_ENTRY'` | `catch (DuplicateKeyException e)` | Spring translates the vendor's codes for you. |
| `docker compose up` + `testcontainers` (npm) | Spring Boot's Docker Compose support + Testcontainers for Java | The same tools. |

## In real projects

- Repositories that use `JdbcClient` are `@Repository` beans with constructor injection, and services own the transactions. See [Spring layering](../../docs/handbook/spring-layering.md).
- Every schema change is a Flyway migration, reviewed in the same pull request as the code that needs it. See [JPA entities and repositories](../../docs/handbook/jpa-entities.md#schema-changes-go-through-migrations).
- A transaction holds a pooled connection from start to end, so transactions stay short and never wait for other services. See [Transactions](../../docs/handbook/transactions.md#keep-transactions-short).
- Database URLs and passwords come from the environment or a secret manager, never from a file in git. See [Configuration](../../docs/handbook/configuration.md#secrets).
- Tests run against the production database engine in Testcontainers and rely only on documented seed data. See [Testing Spring applications](../../docs/handbook/spring-testing.md).
- Sort parameters are checked against a list of allowed properties. See [REST API design](../../docs/handbook/rest-api-design.md#lists-paging-and-filtering).
- SQL logging is off in production and turned on at `DEBUG` to diagnose a problem. See [Logging and observability](../../docs/handbook/logging-and-observability.md).

## Exercises

### S05-01 · Schema migrations with Flyway

**Build** · 30 min · a new migration in [`db/migration/`](src/main/resources/db/migration)

**Ticket: wishlists.** Members want to save books to buy later, and the product team plans a wishlist page. Before anyone writes the feature, the database needs a table for it. Write the migration.

**Acceptance criteria** (checked by `WishlistItemsMigrationTest`, through `information_schema` and by inserting rows):

- A new migration, version 3, in `src/main/resources/db/migration`: `V3__create_wishlist_items.sql`.
- A table `wishlist_items` with exactly these columns, all `NOT NULL`:

  | Column | Type | Meaning |
  |---|---|---|
  | `id` | `BIGINT AUTO_INCREMENT`, the primary key | |
  | `member_id` | `BIGINT` | who saved the book: a foreign key `fk_wishlist_items_member` to `members (id)` |
  | `book_id` | `BIGINT` | the saved book: a foreign key `fk_wishlist_items_book` to `books (id)` |
  | `created_at` | `DATETIME(6)` | when the member saved it, in UTC |

- A member saves each book at most once: a unique key `uk_wishlist_items_member_book` on `(member_id, book_id)`, in this order.
- An index `idx_wishlist_items_book` on `book_id`, for "how many members saved this book?"

**You'll practice:** Flyway's naming rules, MySQL column types, primary, unique and foreign keys, indexes, and checking a schema through `information_schema`.

<details>
<summary>Hint 1</summary>

`V1__create_schema.sql` already has a table with everything you need: compare `coupon_issues` with the criteria above.
</details>

<details>
<summary>Hint 2</summary>

If every test in S05 fails at once with `Error creating bean with name 'flywayInitializer'`, MySQL rejected your migration, so the application can't start. The `Caused by:` lines in the output (`./gradlew :spring:s05-database-foundations:test`) quote MySQL's error message.
</details>

<details>
<summary>Hint 3</summary>

Did `bootRun` stop with a checksum mismatch after you changed V3? That's the rule from lesson section 2 in action. Locally, throw the database away with `docker compose down --volumes` in the module folder. The tests always start from an empty database.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Your migration has run in production, and now the product team wants a note on each saved book. Which file do you write? And why does the "at most once" rule belong in the database, not only in a Java check?

### S05-02 · Queries with JdbcClient

**Build** · 30 min · [`BookJdbcRepository.java`](src/main/java/com/example/bookstore/book/BookJdbcRepository.java)

**Ticket: plain-SQL access to books.** The admin tool needs to find a book, search titles, page through a category, add a book and change a price. Implement `BookJdbcRepository` with `JdbcClient`.

**Acceptance criteria:** the rules in the Javadoc of `BookJdbcRepository`, checked by `BookJdbcRepositoryTest`. The records `Book` (what you read) and `NewBook` (what you insert) are given.

**You'll practice:** `JdbcClient`, named parameters, mapping rows to records, `LIMIT` and `OFFSET`, generated keys, row counts, and Spring's `DataAccessException`s.

<details>
<summary>Hint 1</summary>

Three methods read books with their category's name. One `SELECT … FROM books b JOIN categories c ON …`, with the aliases that `query(Book.class)` needs, serves all three. Keep it in a constant and append the `WHERE` part.
</details>

<details>
<summary>Hint 2</summary>

`LIMIT :size OFFSET :offset`: page 2 with 10 books per page starts after how many books? Then try your formula with `page = Integer.MAX_VALUE` and `size = 100`.
</details>

<details>
<summary>Hint 3</summary>

`update()` returns how many rows the statement changed, not the new id. Pass a `GeneratedKeyHolder` to `update(…)` and read the id from it.
</details>

<details>
<summary>Hint 4</summary>

You don't need a `try`/`catch` for duplicate ISBNs or unknown categories: MySQL rejects the row, and `JdbcClient` throws the right `DataAccessException` for you.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: What would your `findById` do if the query wrongly found two rows? What does `searchByTitle("%")` return, and is that a problem here?

### S05-03 · SQL injection

**Fix** · 30 min · [`BookSearchRepository.java`](src/main/java/com/example/bookstore/book/BookSearchRepository.java)

**Bug reports for `GET /api/books/search?q=…&sort=…`**, the search box of the shop:

1. "Searching for the author `O'Neil` returns a 500 error."
2. "Sorting by `publishedDate`, as the API documentation says, returns a 500 error."
3. Security: "This search returns every member's email and password hash: `q=x' UNION SELECT id, email, password_hash, name, role, 0, DATE(created_at) FROM members -- -`."
4. Security: "The `sort` parameter runs whatever SQL we put into it, for example a subquery against `members`."

Fix `BookSearchRepository` so that no request value ever becomes SQL. Its Javadoc describes the intended behavior; `BookController` and `BookService` are correct. The tests in `BookSearchApiTest` show each symptom.

**You'll practice:** bound parameters, allow lists for what can't be a parameter, and turning invalid input into a `400` with a `BusinessException`.

<details>
<summary>Hint 1</summary>

Run `./score s05-03` before you change anything. The failure messages of the "Injection attempts" tests show what an attacker gets back today.
</details>

<details>
<summary>Hint 2</summary>

The search text appears twice in the SQL. One named parameter can be used in both places.
</details>

<details>
<summary>Hint 3</summary>

A parameter can't stand for a column, so the `ORDER BY` clause has to be built in Java. Map the API's property names to columns (lesson section 8), check the direction against `asc` and `desc`, and throw `new BusinessException(ErrorCode.INVALID_INPUT, …)` for anything else. The client then gets a `400`.
</details>

<details>
<summary>Hint 4</summary>

Doubling the quotes in the text (`'` → `''`) looks like a fix. Read the test "a backslash before the quote doesn't help the UNION either" before you try it.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Where else does input from a request end up in SQL in the applications you work on? Why is a list of allowed values safer than a list of forbidden words such as `UNION` and `--`?

## Review questions

1. Why does an application use a connection pool? What happens to new requests when all 10 connections are in use?
2. Your service runs on 8 instances with HikariCP's default settings. How many connections can MySQL see from it, and which MySQL setting limits that?
3. You fix a typo in `V3__create_wishlist_items.sql`, which already ran in production. What happens at the next deployment, and what should you have done?
4. Flyway's free edition has no "down" migrations. How do you undo a schema change?
5. Why should a migration that adds a `NOT NULL` column to a table with data also say what the existing rows get?
6. What's the difference between `.query(Long.class).single()` and `.query(Long.class).optional()` when the query finds no row?
7. A teammate wraps a `JdbcClient` call in `try { … } catch (SQLException e)`. Why doesn't that compile, and what would you catch instead, if anything?
8. When is catching `DuplicateKeyException` the right thing to do?
9. Why can't `ORDER BY :column` sort by a column the client chooses?
10. `.param("pattern", "%" + text + "%")` is safe from SQL injection. What can a user still do with `%` and `_` in `text`?

<details>
<summary>Answers</summary>

1. Opening a connection costs milliseconds (TCP, TLS, login), which a request would pay every time. A pool opens connections once and lends them out. When all are in use, new requests wait for one to come back; after `connection-timeout` (30 seconds by default) they fail with `Connection is not available, request timed out`.
2. Up to 8 × 10 = 80, because every instance has its own pool. MySQL's `max_connections` (151 by default) limits the total over all clients.
3. Flyway validates checksums at startup, finds that version 3 changed, and refuses to start: the deployment fails. Leave V3 as it is and write `V4__…` with the correction (fix forward).
4. You write a new migration that reverses the change, and you deploy it like any other. Data that a `DROP` removed can only come back from a backup, so think twice before dropping.
5. MySQL needs a value for every existing row. Without a `DEFAULT` or an `UPDATE` that fills the column, the migration fails or MySQL fills in a value nobody chose. It may pass on an empty test database and fail only on production data.
6. `single()` throws `EmptyResultDataAccessException`; `optional()` returns `Optional.empty()`. Use `optional()` when "no row" is a normal answer, such as a lookup by id.
7. `SQLException` is checked, and no `JdbcClient` method declares it, so the compiler rejects a `catch` for it: `JdbcClient` translates it into an unchecked `DataAccessException`. Usually you catch nothing and let the global handler report a `500`; catch a specific subclass, such as `DuplicateKeyException`, only to answer an expected case better.
8. When the duplicate is an expected outcome that the client should hear about, such as an email that's already registered. The unique key catches the race that a check-then-insert can't, and the handler turns it into `409 Conflict`.
9. Parameters carry values, not names. `ORDER BY :column` binds a constant string, so every row gets the same sort key and the order doesn't change. Column names have to be part of the SQL text, which is why they come from an allow list.
10. They work as wildcards: `%` matches any text and `_` any single character, so `%` finds every book. That's not injection, because the query's structure doesn't change, but it can return more than intended. Escape them when the text must match literally.
</details>

## Further reading

- Spring Framework reference: [Data Access with JDBC](https://docs.spring.io/spring-framework/reference/data-access/jdbc.html), with [`JdbcClient`](https://docs.spring.io/spring-framework/reference/data-access/jdbc/core.html#jdbc-JdbcClient) and the [`DataAccessException` hierarchy](https://docs.spring.io/spring-framework/reference/data-access/dao.html)
- Spring Boot reference: [SQL databases](https://docs.spring.io/spring-boot/reference/data/sql.html), [Flyway](https://docs.spring.io/spring-boot/how-to/data-initialization.html#howto.data-initialization.migration-tool.flyway), [Docker Compose support](https://docs.spring.io/spring-boot/reference/features/dev-services.html#features.dev-services.docker-compose) and [Testcontainers](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html)
- [Flyway documentation](https://documentation.red-gate.com/flyway): migrations, the schema history table, validation
- HikariCP: [About Pool Sizing](https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing)
- MySQL reference manual: [`CREATE TABLE`](https://dev.mysql.com/doc/refman/9.7/en/create-table.html), [foreign keys](https://dev.mysql.com/doc/refman/9.7/en/create-table-foreign-keys.html), [`INFORMATION_SCHEMA`](https://dev.mysql.com/doc/refman/9.7/en/information-schema.html)
- OWASP: [SQL Injection Prevention Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/SQL_Injection_Prevention_Cheat_Sheet.html)
