# S06 · Spring Data JPA

⏱ 9 sessions · weeks 9–10 · 7 exercises

In S05 you wrote every SQL statement yourself with `JdbcClient`. Most Spring Boot services at work use JPA instead: you map tables to classes once, and Hibernate writes the SQL. That saves a lot of code, and it also hides a lot. This module teaches what happens underneath: how an entity maps to a table, how a repository turns a method name into a query, how the *persistence context* tracks your objects and decides when SQL runs, why lazy loading causes the N+1 problem and `LazyInitializationException`, and when a bulk update or plain SQL is the better tool. You'll find these problems in almost every JPA codebase, usually in code that seems to work.

## Lesson

Each session, read the lesson sections for that day, then do the exercise. Sessions 3 and 4 are 60-minute weekend sessions.

| Session | Read | Then do |
|---|---|---|
| 1 | 1. ORM, JPA, Hibernate and Spring Data · 2. Mapping an entity | S06-01 |
| 2 | 3. Repositories and queries | S06-02 |
| 3 | 4. Relationships and aggregates | S06-03 |
| 4 | 5. The persistence context · 6. Lazy loading and proxies | S06-04 |
| 5 | 7. The N+1 problem | S06-05 |
| 6 | 8. Open session in view | S06-06 |
| 7 | 9. Bulk updates · 10. When plain SQL is the better tool | S06-07 |

### 1. ORM, JPA, Hibernate and Spring Data

*Object-relational mapping* (ORM) connects two models of the same data. The database stores rows in tables and links them with foreign keys. Java code works with objects that point to other objects. An ORM maps one to the other: a row of `orders` becomes an `Order` object, the foreign key `orders.member_id` becomes a reference to a `Member`, and the rows of `order_lines` become the order's list of lines.

Three names come up together, and each is one layer:

| Layer | What it is | In this course |
|---|---|---|
| **JPA** (Jakarta Persistence) | A *specification*: the annotations (`@Entity`, `@Id`, `@ManyToOne`), the `EntityManager` API and the query language JPQL. It's a set of interfaces and rules, with no code that talks to a database. | Jakarta Persistence 3.2 |
| **Hibernate** | The most widely used *implementation* of JPA. It reads your annotations, writes the SQL, runs it through JDBC and turns rows into objects. | Hibernate ORM 7.4 |
| **Spring Data JPA** | A layer on top: you declare a repository interface, and Spring Data generates the class that implements it, including queries derived from method names. | Spring Data JPA 4.1 |

```
OrderService ──> OrderRepository (an interface; Spring Data writes the implementation)
                      │
                      ▼
                 EntityManager (the JPA API) ── Hibernate ── JDBC ── MySQL
```

Here is one lookup at each level. All three run the same `SELECT`:

```java
// S05: you write the SQL and map the row yourself
String title = jdbc.sql("SELECT title FROM books WHERE id = ?").param(1L).query(String.class).single();

// JPA: you ask for an object by its id; Hibernate writes the SQL
Book book = entityManager.find(Book.class, 1L);

// Spring Data: a repository method; Spring Data calls the EntityManager for you
Book book = bookRepository.findById(1L).orElseThrow();
```

JPA doesn't replace SQL knowledge: it writes SQL that you must be able to read. To see every statement while you run the application, uncomment two lines at the end of `application.yml`: `org.hibernate.SQL: debug` prints each statement, and `org.hibernate.orm.jdbc.bind: trace` prints the values bound to it.

### 2. Mapping an entity

An *entity* is a class whose objects are stored as rows. This is the bookstore's `Book`, from `book/Book.java`:

```java
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)   // 1
@Entity                                               // 2
@Table(name = "books")                                // 3
public class Book extends BaseTimeEntity {            // 4

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // 5
    private Long id;

    private String isbn;                                  // 6
    private String title;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)  // 7
    @JoinColumn(name = "category_id")
    private Category category;

    private long price;
    private LocalDate publishedDate;                      // 8

    public static Book create(String isbn, String title, ...) { ... }   // 9
    public void changePrice(long newPrice) { ... }
}
```

1. **A no-argument constructor for Hibernate.** Hibernate creates an object first and then fills its fields from the row, so it needs a constructor without arguments. Making it `protected` keeps other code from creating half-empty objects.
2. **`@Entity`** marks the class as mapped. A class without it is invisible to Hibernate: `entityManager.persist(...)` fails with *Unknown entity type*.
3. **`@Table`** names the table. Without it, the name comes from the class name: `WishlistItem` would map to `wishlist_item`.
4. **Auditing.** `BaseTimeEntity` holds `createdAt` and `updatedAt`. Spring Data fills them when Hibernate inserts or updates the row, with the time from the application's `Clock` (`common/JpaAuditingConfiguration.java`). Entities whose table has these two columns extend it.
5. **The id.** `@Id` marks the primary key. `GenerationType.IDENTITY` means "MySQL generates it with `AUTO_INCREMENT`". The type is `Long`, not `long`, because a new object has no id until it's saved: `null` means "not saved yet".
6. **Columns.** Spring Boot configures Hibernate to turn camelCase into snake_case, so `publishedDate` maps to `published_date` without any annotation. When the names don't match that way, `@Column(name = "...")` gives the column's real name.
7. **An association** to another entity, stored as the foreign key `category_id`. Section 4 explains associations, and section 6 explains `LAZY`.
8. **Types.** `long` ↔ `BIGINT`, `int` ↔ `INT`, `String` ↔ `VARCHAR`, `LocalDate` ↔ `DATE`, `Instant` ↔ `DATETIME(6)` (stored in UTC, see `application.yml`). A column that can be `NULL` needs a type that can be `null`: `Long` instead of `long`, or an object type such as `String`.
9. **Rules live in the entity.** It has no setters. A factory checks its arguments, and methods such as `changePrice` change the state and protect the rules. The handbook page [JPA entities](../../docs/handbook/jpa-entities.md) lists all of this codebase's entity rules.

**Enums.** Always map an enum with `@Enumerated(EnumType.STRING)`, which stores the constant's name (`'PAID'`). The default, `ORDINAL`, stores its position (`1`). If someone later adds a constant in the middle of the enum, every stored number after it suddenly means something else, and nothing warns you.

```java
@Enumerated(EnumType.STRING)
private OrderStatus status;
```

**Embeddables.** Sometimes several columns form one value: an address (zip code, two lines), or an amount with its currency. An `@Embeddable` class groups them, without a table of its own. Its fields are stored as columns of the entity's table. The bookstore's schema doesn't need one, but you'll meet them at work:

```java
@Embeddable
public record Address(String zipCode, String line1, String line2) { }   // records work since Jakarta Persistence 3.2

@Entity
public class Store {
    @Embedded
    private Address address;                          // columns zip_code, line1, line2

    @Embedded
    @AttributeOverride(name = "zipCode", column = @Column(name = "return_zip_code"))
    @AttributeOverride(name = "line1", column = @Column(name = "return_line1"))
    @AttributeOverride(name = "line2", column = @Column(name = "return_line2"))
    private Address returnAddress;                    // the same type twice needs other column names
}
```

**The schema belongs to Flyway.** `ddl-auto: validate` in `application.yml` tells Hibernate to compare every entity with the tables when the application starts, and never to change the schema. If an entity doesn't match, the application doesn't start. In a test run, every test of the module then fails with `IllegalStateException: Failed to load ApplicationContext`. The real reason is further down: in IntelliJ's test output, or in the HTML report (`build/reports/tests/test/index.html` in the module's folder), search for `Schema validation`, or look for the first `ERROR` line, *Failed to initialize JPA EntityManagerFactory*. These are the messages you'll see most often:

| Message | Cause |
|---|---|
| `Schema validation: missing table [wishlist_item]` | No `@Table`: the default name comes from the class name. |
| `Schema validation: missing column [note] in table [wishlist_items]` | The field's name doesn't match the column: add `@Column(name = "...")`. |
| `Schema validation: wrong column type encountered in column [priority] in table [wishlist_items]; found [varchar (Types#VARCHAR)], but expecting [tinyint (Types#TINYINT)]` | An enum without `@Enumerated(EnumType.STRING)`: Hibernate expects a number. |
| `Schema validation: missing table [wishlist_items_seq]` | `@GeneratedValue` without `strategy = IDENTITY`: Hibernate expects a sequence table. |

`@Column(nullable = false, length = 200)` changes nothing here: Hibernate only uses those attributes when it creates tables itself, which it never does in this codebase. Validation checks that tables and columns exist and that their types fit.

### 3. Repositories and queries

A repository interface that extends `JpaRepository<Book, Long>` (entity type, id type) gets these methods without any code of its own: `save`, `saveAll`, `findById`, `findAll` (also with a `Sort` or a `Pageable`), `existsById`, `count`, `getReferenceById`, `delete`, `deleteById`, `flush` and more. Spring Data creates the implementing class when the application starts.

**Derived queries.** Spring Data reads a method's name and writes the query:

```java
Optional<Member> findByEmail(String email);                                  // where m.email = ?
boolean existsByCouponIdAndMemberId(Long couponId, Long memberId);           // a path: coupon.id
long countByBookId(Long bookId);                                             // select count(...)
List<Book> findByCategoryNameAndPriceLessThanEqualOrderByPriceDesc(String category, long maxPrice);
Page<Order> findByMemberId(Long memberId, Pageable pageable);                // a page, plus a count query
List<Book> findTop5ByOrderByPriceDesc();                                     // limit 5
```

The name starts with `find…By`, `count…By`, `exists…By` or `delete…By`. After `By` come property paths: `bookId` means `book.id`, and `categoryName` means `category.name` (a join). Operators include `And`, `Or`, `Between`, `LessThan`, `GreaterThanEqual`, `Containing`, `StartingWith`, `IgnoreCase`, `In`, `IsNull` and `OrderBy…Asc/Desc`. Spring Data checks every name when the application starts, so a typo fails at once: *No property 'titel' found for type 'Book'*. Derived queries are good for short lookups. When the name grows long, write the query.

**`@Query` with JPQL.** JPQL looks like SQL, but it talks about entities and fields, not tables and columns:

```java
@Query("select avg(r.rating) from Review r where r.book.id = :bookId")
Optional<Double> findAverageRatingByBookId(Long bookId);
```

`Review` is the entity, `r.book.id` follows the association, and `:bookId` is a named parameter filled from the method parameter with the same name. (That needs the compiler flag `-parameters`, which this repository's build sets. Without it, write `@Param("bookId")`.) Hibernate checks every `@Query` when the application starts, too.

**Native queries.** `@Query(value = "SELECT ...", nativeQuery = true)` runs MySQL SQL as it is. Use it for features JPQL doesn't have. You lose the startup check, and with paging you often have to write the `countQuery` yourself.

**Projections.** A query doesn't have to return entities. A *projection* returns only the columns a screen needs, as a record:

```java
@Query("""
        select new com.example.bookstore.order.BookSales(b.id, b.title, sum(l.quantity))
        from OrderLine l join l.book b
        group by b.id, b.title""")
List<BookSales> findBookSales();
```

`select new` calls the record's constructor for each row; it needs the full class name. Projections load less data, and the persistence context doesn't track them (section 5), so they can't trigger lazy loading. Use them for read-only screens and reports. Spring Data also supports *interface projections*, such as `interface TitleOnly { String getTitle(); }` as the return type of a derived query.

**Paging and sorting.** A `Pageable` parameter adds `order by` and `limit` to any query. Spring MVC builds it from the request: `?page=0&size=5&sort=createdAt,desc`. `@PageableDefault` and `@SortDefault` set what applies when the client sends nothing.

| Return type | Runs | Use it when |
|---|---|---|
| `Page<T>` | the page query, and a `count` query for `totalElements` and `totalPages` | the client shows page numbers |
| `Slice<T>` | one query that asks for `size + 1` rows, to know whether there is a next slice | infinite scrolling: no count, so it's cheaper |
| `List<T>` with `Pageable` | the page query only | you need neither |

Spring Data skips the count query when it can compute the total itself, for example on a last page that isn't full. Controllers return `new PagedModel<>(page)`, whose JSON shape is stable (BOOKSTORE.md shows it).

**Sort by a unique column last.** If two reviews have the same `createdAt`, the database may return them in any order, and paging can show one review twice and another never. Add the id as the last sort key: `createdAt desc, id desc`.

### 4. Relationships and aggregates

**`@ManyToOne` owns the foreign key.** Many order lines belong to one order, and `order_lines.order_id` stores that. The side with the foreign key is the *owning side*:

```java
@Entity
public class OrderLine {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")          // the foreign key column
    private Order order;
}
```

**`@OneToMany(mappedBy = ...)` mirrors it.** The order's list of lines is the *inverse side*: `mappedBy = "order"` says "the field `order` in `OrderLine` maps this". Hibernate writes `order_id` only from the owning side. If you add a line to `order.getLines()` but the line's `order` field is `null`, Hibernate has no `order_id` to write: with a `NOT NULL` column, as here, the insert fails, and with a nullable one the line is saved without its order. So the code that adds a child sets both sides in one place, and in this codebase that's always a method of the parent.

**Cascades.** `cascade = CascadeType.ALL` on the parent's collection makes operations on the parent apply to the children too: `persist(cart)` also persists its new items, and `remove(cart)` also removes them. Put cascades only on a parent → children association. Never put them on a `@ManyToOne` to a shared entity: with `cascade = ALL` on `OrderLine.book`, deleting a line would try to delete the book.

**Orphan removal.** `orphanRemoval = true` deletes a child's row when the child leaves the parent's collection. A cascaded `REMOVE` only deletes children when the parent itself is deleted.

Put together, with a shopping cart as the example:

```java
@Entity
public class Cart {
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")                                    // the list's order, in SQL
    private List<CartItem> items = new ArrayList<>();

    public void add(Book book) {
        items.add(new CartItem(this, book));          // sets the owning side: item.cart = this
    }

    public void remove(Book book) {
        items.removeIf(item -> item.isFor(book));     // orphanRemoval deletes the row at the next flush
    }

    public List<CartItem> getItems() {
        return Collections.unmodifiableList(items);   // changes go through the cart's methods
    }
}
```

`cartRepository.save(cart)` now saves the cart and its items, and the service never touches a `CartItem` directly.

**Aggregates.** A parent and the children it owns form an *aggregate*: they are loaded, changed and saved as one unit, and only through the parent, the *aggregate root*. The root checks the rules that span its children: "lines can only change while the order is `PENDING`", "the total is the sum of the lines". In this codebase, `Order` with its `OrderLine`s is an aggregate. That's why `OrderLineRepository` exists only for reading: lines are created and removed by `Order`. Other entities, such as `Book` or `Member`, are separate aggregates, which an order refers to but never saves or deletes.

**Avoid `@ManyToMany`.** A link table almost always grows its own columns (when, by whom), and then it needs an entity anyway. `CouponIssue` is the link between coupons and members, as an entity with `issuedAt` and `usedAt`.

### 5. The persistence context

Inside a transaction, Hibernate keeps every entity it has loaded or saved in the *persistence context*: a map from "entity type and id" to the one object for that row. (Hibernate's documentation also calls it the *first-level cache*.) With `open-in-view` off (section 8), a persistence context lives exactly as long as its transaction. It explains most of JPA's behavior:

**One object per row.** `findById` looks in the persistence context first, and runs SQL only if the row isn't there. A query (JPQL or a derived query) always runs its SQL, but for each row it finds, it returns the object that the persistence context already holds. So inside one transaction, `==` between two `Book`s means "same row". Two transactions have two persistence contexts, and each gets its own object.

**Entity states.** An object is *new* (created with `new` or a factory, unknown to Hibernate), *managed* (in a persistence context), *detached* (it was managed, but its persistence context has closed) or *removed* (scheduled for deletion).

**Dirty checking.** When Hibernate loads an entity, it keeps a copy of its state, the *snapshot*. Before a commit, it compares every managed entity with its snapshot and writes an `UPDATE` for each one that differs. So this is complete:

```java
@Transactional
public void cancel(long orderId) {
    Order order = orderRepository.findById(orderId).orElseThrow();
    order.cancel();          // no save(): the UPDATE runs before the commit
}
```

Change a value and change it back, and there's nothing to write. Hibernate's `UPDATE` writes every column, not only the changed ones (`@DynamicUpdate` changes that).

**Flushing.** Writing pending changes to the database is called *flushing*. It isn't committing: the database applies the statements, and the commit makes them permanent. Hibernate flushes:

- before the transaction commits;
- before a JPQL query that reads a table with pending changes, so the query sees them. A query that reads only other tables doesn't trigger a flush;
- before every native query, because Hibernate can't know which tables it reads;
- when you call `entityManager.flush()` (or a repository's `flush()`).

**`IDENTITY` ids are the exception.** Only MySQL knows the next `AUTO_INCREMENT` value, so Hibernate runs the `INSERT` as soon as you persist a new entity, to learn its id. The price: Hibernate can't batch inserts of such entities.

**What `save()` does.** Spring Data's `save(entity)` calls `persist` when the entity is new (its id is `null`), and `merge` otherwise. `merge` loads the row (a `SELECT`, unless the persistence context has it), copies your object's state onto the managed object, and returns the managed one. The object you passed in stays detached. For an entity you loaded in the same transaction, `save()` does nothing useful: dirty checking already covers it.

**Detached entities.** After the transaction ends, an entity is detached: nobody tracks it, so changing it changes nothing in the database. Another transaction loads its own, separate object.

**Read-only transactions.** In `@Transactional(readOnly = true)`, Spring sets Hibernate's session to read-only: Hibernate keeps no snapshots and doesn't flush at all. Changes to entities are silently ignored. That saves memory and time on reads, and it's one more reason to mark query methods `readOnly`.

### 6. Lazy loading and proxies

**`LAZY` and `EAGER`.** An association marked `LAZY` is loaded when it's first used; `EAGER` loads it together with its owner. JPA's defaults are `EAGER` for `@ManyToOne` and `@OneToOne`, and `LAZY` for collections (`@OneToMany`, `@ManyToMany`). This codebase makes every association `LAZY` and loads what each use case needs on purpose (section 7). An `EAGER` association is loaded everywhere, even where nobody needs it, and often with extra queries.

**Proxies.** For a lazy `@ManyToOne`, Hibernate puts a *proxy* in the field: an object of a generated subclass, such as `Book$HibernateProxy`, that only knows the id from the foreign key column. The first call to a getter other than `getId()` loads the row with one `SELECT`. A lazy collection is a Hibernate list (`PersistentBag`) that loads its rows the first time you use it, for example with `size()` or a loop.

```java
Book book = bookRepository.getReferenceById(1L);   // a proxy, no SQL
book.getId();                                      // still no SQL: the proxy knows its id
book.getTitle();                                   // SELECT ... FROM books WHERE id = 1

Order order = orderRepository.findById(1L).orElseThrow();   // SELECT ... FROM orders
order.getLines().size();                                    // SELECT ... FROM order_lines WHERE order_id = 1
```

`getReferenceById` is useful when you only need a reference to set a foreign key, without loading the row. `Hibernate.isInitialized(order.getMember())` tells you whether a proxy or collection has been loaded.

A proxy is a subclass, so `book.getClass() == Book.class` is `false` for a proxy. That's one reason this codebase doesn't override `equals` on entities (`BaseTimeEntity`'s Javadoc explains the others).

**Lazy loading needs an open persistence context.** After the transaction has ended, a proxy or collection that wasn't loaded can't be loaded anymore. Section 8 shows what happens then.

### 7. The N+1 problem

Take a page of 10 orders, and show each order's lines with their books' titles:

```java
Page<Order> orders = orderRepository.findByMemberId(memberId, pageable);   // 1 query (+ 1 count)
orders.map(OrderResponse::from);   // reads order.getLines() and line.getBook().getTitle()
```

The SQL log shows the problem:

```
select ... from orders o1_0 where o1_0.member_id=? order by ... limit ?    -- the page
select count(*) from orders o1_0 where o1_0.member_id=?                     -- the total
select ... from order_lines l1_0 where l1_0.order_id=?                      -- once per order: 10 times
select ... from books b1_0 where b1_0.id=?                                  -- once per book: up to 30 times
```

One query for the list, then N more for the items: the *N+1 problem*. Each query is fast, but the round trips add up, and the count grows with the data. It's the most common JPA performance bug, and it's invisible until you count statements. In this module's tests, `SqlStatementCounter` does the counting.

The fixes, from the most specific to the most general:

**1. A fetch join** loads associations in the same query:

```java
@Query("""
        select o from Order o
        left join fetch o.lines l
        left join fetch l.book
        where o.member.id = :memberId
        order by o.orderedAt desc, o.id desc""")
Page<Order> findHistory(Long memberId, Pageable pageable);
```

Use `left join fetch`. An inner `join fetch` drops orders without lines from the result, and the count query that Spring Data derives from it then counts joined rows instead of orders. And when a `@Query` replaces a derived query, the method's name no longer sorts anything: the `order by` must be in the query.

**2. An entity graph** declares the same thing on a repository method and keeps the derived query:

```java
@EntityGraph(attributePaths = {"lines", "lines.book"})
Page<Order> findByMemberIdOrderByOrderedAtDescIdDesc(Long memberId, Pageable pageable);
```

Entity graphs always use left outer joins.

**3. Batch fetching** keeps the separate queries, but loads many associations at once: `spring.jpa.properties.hibernate.default_batch_fetch_size: 100` in `application.yml` (or `@BatchSize(size = 100)` on one collection) turns ten `where order_id=?` queries into one `where order_id in (?, ?, ...)`, and the same for the books. A page then costs four queries (the page, the count, the lines, the books), whatever its size. This module's `application.yml` leaves the setting out on purpose, so that you see N+1 problems instead of having them hidden.

**4. A projection** (section 3) selects exactly the columns a read-only screen needs.

**Fetch joins and paging.** A `limit` on a query that joins the lines would cut an order's lines apart. Hibernate 7.4 solves this by selecting the page of orders in a subquery and joining the lines to it:

```sql
select ... from (select ... from orders where member_id=? order by ordered_at desc, id desc limit ?, ?) o
left join order_lines l on o.id=l.order_id left join books b on b.id=l.book_id
order by o.ordered_at desc, o.id desc, l.id
```

Older Hibernate versions, which you'll find in most codebases today (Spring Boot 2 and 3 use Hibernate 5 and 6), can't do that. They load *every* matching row, page in memory and log the warning *HHH90003004: firstResult/maxResults specified with collection fetch; applying in memory*. With a big table, that can fill the heap. There, page the parents first and batch-fetch the children. This codebase's `application.yml` makes Hibernate fail instead of paging in memory, so the problem shows up in tests.

**Two collections at once.** Fetching two `List` collections in one query fails with `MultipleBagFetchException`: the result would be the product of both lists. Fetch one collection and batch-fetch the other.

### 8. Open session in view

By default, Spring Boot opens an `EntityManager` at the start of each web request and closes it when the response is written. This is called *open session in view* (OSIV) or *open EntityManager in view*, and Spring Boot logs a warning about it at startup. With OSIV, lazy loading also works in controllers and during JSON serialization. That's convenient, and it's why many codebases have it on. But every lazy load after the service returns runs outside any transaction, in auto-commit mode; N+1 problems hide in the web layer, where nobody looks for them; and the request keeps its database connection until the response is written.

This codebase turns it off: `spring.jpa.open-in-view: false`. The persistence context now closes with the service's transaction, and touching a lazy association that wasn't loaded throws `LazyInitializationException`:

```
LazyInitializationException: Cannot lazily initialize collection of role 'com.example.bookstore.order.Order.lines' with key '17' (no session)
LazyInitializationException: Could not initialize proxy [com.example.bookstore.member.Member#13] - no session
```

The first comes from a lazy collection, the second from a proxy. Both mean: "this object's persistence context has closed, so I can't load the rest".

**The fix: build the response inside the transaction.** The service method that loads the entities also maps them to a response record, while its persistence context is open. If the mapping needs associations, load them in the same query (section 7). Controllers receive records and never see entities, as [Spring layering](../../docs/handbook/spring-layering.md) says.

```java
@Transactional(readOnly = true)
public OrderResponse findOrder(long orderId) {
    Order order = orderRepository.findById(orderId).orElseThrow(...);
    return OrderResponse.from(order);   // reads the lazy lines while the persistence context is open
}
```

Fixes that only seem to work: turning OSIV back on, `@Transactional` on a controller, `FetchType.EAGER`, or Hibernate's `enable_lazy_load_no_trans` setting. Each one moves the hidden queries somewhere else.

**A service without `@Transactional`** has the same problem. Each repository call then runs in its own short transaction, so `paymentRepository.findById(...)` returns an object that is already detached.

**The test transaction hides all of this.** A test class with `@Transactional` (like this course's `@IntegrationTest`) runs each test, and the MockMvc requests it sends, in one transaction. Its persistence context stays open for the whole test, so lazy loading works in controllers there, but not in production. Test such endpoints with `@Transactional(propagation = Propagation.NOT_SUPPORTED)` on the test class: then each request runs as it would in production.

### 9. Bulk updates

To change many rows at once, an entity loop is slow: it loads every entity and writes one `UPDATE` each. A *bulk update* is one statement:

```java
@Modifying
@Query("update Book b set b.price = b.price + :amount where b.category.id = :categoryId")
int raisePrices(Long categoryId, long amount);   // the number of rows the UPDATE matched
```

`@Modifying` tells Spring Data to run the query with `executeUpdate()`. The statement needs a transaction (without one, it fails with *No active transaction for update or delete query*), and it returns the number of rows. (MySQL Connector/J counts the rows that matched the `WHERE` clause, even if a row's value didn't change.)

A bulk update goes straight to the database, so it skips everything JPA does for entities:

- **The persistence context.** Entities loaded before the update keep their old values. Worse, if such an entity changes and is flushed later, its `UPDATE` writes the old values back over the bulk change.
- **The entity's rules.** `Book.changePrice` rejects a price below 1, but the `UPDATE` doesn't run it.
- **Callbacks and auditing.** `@LastModifiedDate` isn't filled: set `updatedAt` in the query yourself if the column matters.
- **Optimistic locking.** The `version` column doesn't change, unless you write `update versioned Inventory ...` (S08 explains versions).

Two attributes of `@Modifying` handle the persistence context:

- `clearAutomatically = true` empties the persistence context after the update, so later reads in the same transaction load the new values.
- `flushAutomatically = true` flushes before the update. Without it, the clear can throw away changes that weren't flushed yet: Hibernate flushes automatically before the update only if those changes touch the updated table.

Use both when the update runs in a transaction that also uses entities. The same applies to `@Modifying @Query("delete from ...")`. Note that a derived `deleteBy…` method is no bulk delete: it loads the entities and deletes them one by one.

### 10. When plain SQL is the better tool

JPA is at its best for loading an aggregate, changing it through its methods and saving it. For other jobs, SQL is often simpler and faster:

- **Reports and statistics** over many rows: `GROUP BY`, window functions, `WITH` queries.
- **MySQL-specific SQL**, such as `INSERT ... ON DUPLICATE KEY UPDATE`.
- **Large inserts.** With `IDENTITY` ids, Hibernate inserts one row per statement; `JdbcClient` can send one multi-row `INSERT`.
- **Read-only lists** where entities add nothing but overhead.

`JdbcClient` (S05) and JPA work together in one application, and even in one transaction: they share the connection. But plain SQL bypasses the persistence context in both directions, as bulk updates do: flush before you read with SQL what JPA changed, and don't trust entities you loaded before writing with SQL. Many teams use JPA for commands and SQL (with `JdbcClient`, jOOQ, MyBatis or QueryDSL) for complex queries.

## TypeScript → Spring

If you've used TypeORM or Prisma, most ideas carry over. The big difference is the persistence context: TypeORM and Prisma give you plain objects, and nothing is saved until you call `save()` or `update()`. JPA gives you *managed* objects, and every change to them is saved when the transaction commits. Prisma has no lazy loading at all, so it can't run hidden queries.

| TypeORM / Prisma | Spring Data JPA | Watch out for |
|---|---|---|
| `@Entity()` class / a `model` in `schema.prisma` | `@Entity` class with `@Table(name = "books")` | JPA needs a no-argument constructor; this codebase makes it `protected` |
| `@PrimaryGeneratedColumn()` / `@id @default(autoincrement())` | `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)` | without `IDENTITY`, Hibernate expects a sequence table |
| `@Column({ name: 'memo' })` / `@map("memo")` | `@Column(name = "memo")` | only needed when the snake_case name doesn't match |
| `@ManyToOne(() => Book)` / a relation field | `@ManyToOne(fetch = FetchType.LAZY)` | JPA's default for to-one associations is `EAGER` |
| `@OneToMany(..., { cascade: true })` | `@OneToMany(mappedBy = "order", cascade = ALL, orphanRemoval = true)` | the `@ManyToOne` side writes the foreign key |
| `repo.findOneBy({ email })` / `prisma.member.findUnique({ where: { email } })` | `Optional<Member> findByEmail(String email)` | Spring Data parses the name at startup |
| query builder / `prisma.$queryRaw` | `@Query` with JPQL, or `nativeQuery = true` | JPQL names entities and fields, not tables and columns |
| `relations: ['lines']` / `include: { lines: true }` | `left join fetch` or `@EntityGraph` | without it, each lazy association costs another query |
| `repo.save(order)` after each change / `prisma.order.update(...)` | nothing: dirty checking writes managed entities at commit | changes to a detached entity are lost without an error |
| `skip` and `take` | `Pageable`, `Page`, `Slice` | `Page` runs a second query for the total |
| `update().set(...).where(...)` / `prisma.book.updateMany(...)` | `@Modifying @Query("update ...")` | entities already loaded keep their old values |
| `dataSource.transaction(...)` / `prisma.$transaction(...)` | `@Transactional` on a service method | the persistence context lives as long as the transaction |
| TypeORM migrations / `prisma migrate` | Flyway, and Hibernate set to `validate` | never let Hibernate change the schema |
| `synchronize: true` | `ddl-auto: update` | never in production |

## In real projects

- **Entities protect their rules.** A protected no-argument constructor, a factory that validates, methods instead of setters, `LAZY` associations, enums by name. → [JPA entities](../../docs/handbook/jpa-entities.md)
- **Controllers never see entities.** Services map entities to response records inside their transaction. → [Spring layering](../../docs/handbook/spring-layering.md)
- **Keep `open-in-view` off**, and load what each use case needs with a fetch join, an entity graph or a projection. → [JPA entities](../../docs/handbook/jpa-entities.md#open-session-in-view-turn-it-off)
- **Mark query methods `@Transactional(readOnly = true)`**, and keep one transaction per use case. → [Transactions](../../docs/handbook/transactions.md)
- **No `@Data`, `@Setter`, `@EqualsAndHashCode` or `@ToString` on entities.** A generated `toString` that touches a lazy association runs a query, or throws, whenever something logs the entity. → [Lombok](../../docs/handbook/lombok.md)
- **Count SQL statements in tests** of list endpoints, and test lazy loading without the test transaction. → [Testing Spring applications](../../docs/handbook/spring-testing.md)
- **Read the SQL.** Turn on `org.hibernate.SQL: debug` locally when you work on data access; never leave it on in production. → [Logging and observability](../../docs/handbook/logging-and-observability.md)
- **The schema changes only through migrations**; Hibernate validates. → [JPA entities](../../docs/handbook/jpa-entities.md#schema-changes-go-through-migrations)

## Exercises

The exercises are tickets from the bookstore's backlog. The tests are the acceptance criteria: `./score s06` runs them, and `./score s06-02` lists every failing test of one exercise. The tests run against MySQL in Docker, so Docker Desktop must be running.

### S06-01 · Mapping entities

**Build** · 30 min · [`WishlistItem.java`](src/main/java/com/example/bookstore/wishlist/WishlistItem.java)

> **BOOK-301 · Map the wishlist table**
>
> Members will soon be able to save books for later. The database team has added the table `wishlist_items` (migration [`V3__create_wishlist_items.sql`](src/main/resources/db/migration/V3__create_wishlist_items.sql)). Map the entity `WishlistItem` to it, so the wishlist API can use it in the next sprint.

`WishlistItem` has its fields and the Javadoc that describes the table and the rules, but no mapping yet, and its methods throw. There is no repository for it yet, so the tests in `WishlistItemMappingTest` use the `EntityManager` directly and check the rows with plain SQL.

**Acceptance criteria:**

- A new item can be persisted, and MySQL generates its id.
- Each field is stored in its column: `member_id`, `book_id`, `priority` (the enum's name) and `memo` (the note). A row written with plain SQL reads back with the same values.
- `created_at` and `updated_at` come from the clock, like in the other entities. A later change moves only `updated_at`.
- Loading an item loads neither its member nor its book.
- Removing an item deletes its row, and nothing else.
- The rules in the class's Javadoc: required arguments, and how notes are stored.

**Watch out:** a mapping that doesn't match the table stops the application from starting, and then *every* test of the module fails with the same error. Lesson section 2 shows how to find the real message and what the common ones mean.

**You'll practice:** `@Entity`, `@Table`, `@Id` with `IDENTITY`, `@ManyToOne(fetch = LAZY)`, `@Enumerated(EnumType.STRING)`, `@Column(name = ...)`, auditing through `BaseTimeEntity`, the protected constructor and a validating factory.

<details>
<summary>Hint 1</summary>

Start from `Book`: copy its class annotations and its id. Then look at the table: which field's name doesn't lead to its column's name?
</details>

<details>
<summary>Hint 2</summary>

Auditing needs no code of its own here. What do `Book` and `Order` extend?
</details>

<details>
<summary>Hint 3</summary>

Write the note rule once, in a private method that both the constructor and `changeNote` call: `null` or blank gives `null`, otherwise strip, then check the length.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Which of your annotations change the SQL that Hibernate writes, and which only document the table? What would the stored priorities mean if someone added `URGENT` before `HIGH` in `WishlistPriority` with the default `ORDINAL` mapping?

### S06-02 · Repositories and queries

**Build** · 30 min · [`ReviewService.java`](src/main/java/com/example/bookstore/review/ReviewService.java) · [`ReviewRepository.java`](src/main/java/com/example/bookstore/review/ReviewRepository.java)

> **BOOK-302 · Ratings and reviews on the book page**
>
> The book page should show a book's average rating, its number of reviews, and its reviews, five at a time. [`ReviewController`](src/main/java/com/example/bookstore/review/ReviewController.java) and the records `ReviewResponse` and `RatingResponse` are done. Implement `ReviewService`, and add the queries it needs to `ReviewRepository`.

**Acceptance criteria** (the tests in `ReviewApiTest`):

- `GET /api/books/{bookId}/reviews` returns a page of `ReviewResponse`s: the review's id, the reviewer's name, the rating, the text and the time. By default 5 per page, newest first; the client can ask for another page, size or sort.
- The page's metadata reports the total: book 1 has 15 reviews, so 3 pages of 5.
- A page costs at most 3 SQL statements, however many reviewers it shows: no query per review.
- `GET /api/books/{bookId}/rating` returns `{ "bookId": 1, "reviewCount": 15, "averageRating": ... }`. The database computes the count and the average, and the reviews themselves aren't loaded. A book without reviews has a count of 0 and an average of `null`.
- An unknown book gives `404` with the code `BOOK_NOT_FOUND`, for both endpoints.
- Use at least once each: a derived query, a JPQL `@Query`, a projection into a record, and paging with `Pageable`.

The average is MySQL's: `AVG` of whole numbers gives a decimal number with four decimal places, such as `3.6667`.

**You'll practice:** derived query names, JPQL with named parameters, `Optional` from an aggregate query, `select new` into a record, `Page` and its count query.

<details>
<summary>Hint 1</summary>

A count of reviews per book fits in a method name. The review's book is the field `book`, and its id is `book.id`.
</details>

<details>
<summary>Hint 2</summary>

What does `avg` return when the book has no reviews? Choose a return type that can say "no value". `double` can't.
</details>

<details>
<summary>Hint 3</summary>

For the page, select straight into `ReviewResponse` with `select new com.example.bookstore.review.ReviewResponse(...)`, and join the member to get the name. Add a `Pageable` parameter and return `Page<ReviewResponse>`: Spring Data adds the `order by`, the `limit` and the count query.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: How many statements does your rating endpoint run, and could it be one? Why does the controller sort by `id` after `createdAt`?

### S06-03 · Relationships and aggregates

**Build** · 60 min · [`Order.java`](src/main/java/com/example/bookstore/order/Order.java) · [`OrderService.java`](src/main/java/com/example/bookstore/order/OrderService.java)

> **ORDER-12 · Place orders and remove order lines**
>
> Members can't order yet. Implement `POST /api/orders` and `DELETE /api/orders/{orderId}/lines/{bookId}`. [`OrderController`](src/main/java/com/example/bookstore/order/OrderController.java) and the records `CreateOrderRequest` and `OrderResponse` are done. An order is an aggregate: the service changes its lines only through `Order`, and saving the order saves them.

```json
POST /api/orders
{ "memberId": 1, "lines": [ { "bookId": 1, "quantity": 2 }, { "bookId": 2, "quantity": 1 } ] }
```

**Acceptance criteria** (the tests in `OrderTest` and `OrderApiTest`):

- `Order.addLine` and `Order.removeLine` follow their Javadoc: a new line belongs to its order, the same book twice gives one line, the total stays right, and only a `PENDING` order's lines can change.
- Saving an order with `orderRepository.save(order)` saves its new lines too, and a line removed from a loaded order is deleted from the database, without any call for the line itself.
- `POST /api/orders` answers `201 Created` with the order and a `Location` header. The order is `PENDING`, ordered now, and every line has the book's current price.
- Placing an order takes the copies out of stock. Ordering the last copies of a book works.
- All or nothing: if one book is out of stock (`409 OUT_OF_STOCK`) or doesn't exist (`404 BOOK_NOT_FOUND`), no order is saved and no stock changes. An unknown member gives `404 MEMBER_NOT_FOUND`.
- `DELETE /api/orders/{orderId}/lines/{bookId}` answers `204 No Content`, deletes the line's row, lowers the total and puts the copies back into stock. Removing the last line leaves an empty `PENDING` order with a total of 0.
- Removing a line of an order that isn't `PENDING` gives `409 INVALID_ORDER_STATUS` and changes nothing. An unknown order gives `404 ORDER_NOT_FOUND`, and a book that isn't in the order `404 ORDER_LINE_NOT_FOUND`.

`OrderApiTest` runs without the test transaction: each request commits or rolls back on its own, as in production, and the test puts the seed data back afterwards. So a missing transaction shows up in the tests too.

**You'll practice:** `@OneToMany(mappedBy)` with `cascade` and `orphanRemoval`, keeping both sides of an association in sync, aggregate methods, dirty checking, one transaction per use case.

<details>
<summary>Hint 1</summary>

In `addLine`, check everything first (arguments, then `checkLinesCanChange()`), so that nothing changes when it throws. Then look for an existing line with `line.isFor(book)`: add to it, or create `new OrderLine(this, book, quantity)`. Finish with `recalculateTotal()`.
</details>

<details>
<summary>Hint 2</summary>

Two attributes of `@OneToMany` match the two sentences in the Javadoc of `Order.lines`. Lesson section 4 names them.
</details>

<details>
<summary>Hint 3</summary>

In `placeOrder`, which annotation makes all the steps one unit that succeeds or fails together? And why does `inventory.decrease(...)` need no `save()` call?
</details>

<details>
<summary>Hint 4</summary>

In `removeLine`, find the line first: you need its quantity to put the copies back. Then let the order remove it.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Where would a rule such as "an order has at most 20 lines" go? What would go wrong if another service deleted a line with `orderLineRepository.delete(line)` instead of calling `order.removeLine(book)`?

### S06-04 · Predict: the persistence context

**Predict** · 60 min · [`Puzzles.java`](src/test/java/com/example/bookstore/persistencecontext/Puzzles.java) → answers in [`Answers.java`](src/test/java/com/example/bookstore/persistencecontext/Answers.java)

Nineteen small scenarios that run against the real database: loading the same book twice, changing an entity without `save()`, a query that forces a flush, lazy loading inside and outside a transaction, detached entities, and a bulk update that leaves stale entities behind. Each puzzle returns one observation: a number of SQL statements, a value read back with plain SQL, whether two variables point to the same object, or how the scenario ends.

**How:** read each method in `Puzzles` without running it, and write your prediction in the matching constant in `Answers`. Then run `PuzzlesTest` (or `./score s06-04`). Each failing test shows what you predicted and what really happened.

Both files are in the test sources, because they need the application context and aren't part of the application. The puzzles start and commit their own transactions with `inTransaction(...)`, so `PuzzlesTest` turns the test transaction off (otherwise one transaction would wrap every puzzle), and it puts the seed data back after each puzzle.

<details>
<summary>Explanations (open after you've run the tests)</summary>

**One object per row**

1. **`findById(1L)` twice: 1 statement.** The first call loads book 1 into the persistence context; the second finds it there and runs no SQL.
2. **The same object: `true`.** A persistence context holds one object per row, so `first == second`.
3. **`findById` then `findByIsbn`: 2 statements.** A query always runs its SQL; only `findById` looks in the persistence context first. The query's result is still the same object as the first one.
4. **Two transactions, the same object: `false`.** Each transaction has its own persistence context, which loads its own copy of the row.

**Dirty checking**

5. **The price after a change without `save()`: 40,000.** At commit, Hibernate compares the managed book with its snapshot and writes the `UPDATE` itself.
6. **Change and change back: 1 statement.** Only the `SELECT`. Dirty checking compares the final state with the snapshot, and they're equal, so there is nothing to write.
7. **The price after a read-only transaction: 36,000.** In a read-only transaction, Spring makes Hibernate keep no snapshots and never flush, so the change is ignored without an error.

**When the SQL runs**

8. **`save()` a new member: 1 statement before the commit.** With `IDENTITY` ids, only MySQL knows the next id, so the `INSERT` runs at once.
9. **Change a book, then count the books: 3 statements.** The `SELECT`, then the `UPDATE`, then the `COUNT`. The query reads `books`, which has a pending change, so Hibernate flushes first; otherwise the count could be wrong.
10. **Change a book, then count the members: 2 statements.** The `SELECT` and the `COUNT`. The query reads only `members`, so Hibernate doesn't need to flush yet. The `UPDATE` runs at commit.

**Lazy loading and proxies**

11. **`getReferenceById(1L).getId()`: 0 statements.** `getReferenceById` returns a proxy without SQL, and the proxy already knows its id.
12. **Load an order and read its lines: 2 statements.** One `SELECT` for the order, and one for its lines when the lazy list is first used.
13. **The lines after the transaction: `THROWS_LAZY_INITIALIZATION_EXCEPTION`.** The lines were never loaded, and the persistence context that could load them closed with the transaction.
14. **The member's id after the transaction: `RETURNS_NORMALLY`.** The member is a proxy that holds its id, which came from `orders.member_id`, so `getId()` needs no database.

**Detached entities**

15. **The price after changing a detached book: 36,000.** Nobody tracks a detached entity. The second transaction loads its own copy, finds nothing changed, and writes nothing.
16. **`save()` a changed detached book: 2 statements.** `save()` of an entity with an id calls `merge()`: a `SELECT` to load the managed copy, then the `UPDATE` at commit for the copied change.

**Bulk updates**

17. **The price after the bulk update: 36,000.** The `UPDATE` changed the row, but not the book in the persistence context, and the second `findById` returns that stale object without SQL.
18. **With `clear()` first: 37,000.** After `clear()`, the persistence context is empty, so `findById` runs a new `SELECT` and sees the new price.
19. **The stock after the restock and the sale: 9.** The loaded inventory still holds 10 copies. `decrease(1)` makes it 9, and at commit its `UPDATE` writes 9 over the 15 that the bulk update stored: the delivery of 5 copies is lost. The bulk update didn't change the `version` column either, so optimistic locking didn't notice. With `update versioned Inventory ...`, the second `UPDATE` would have failed instead.
</details>

### S06-05 · The N+1 problem

**Fix** · 30 min · [`OrderRepository.java`](src/main/java/com/example/bookstore/order/OrderRepository.java), and wherever else you decide to fix it

> **ORDER-31 · The order history is slow**
>
> "Members with many orders wait seconds for their order history." `GET /api/members/{memberId}/orders` returns the right orders, but `MemberOrdersApiTest` counts 26 SQL statements for one page of 10 orders, and the number grows with every order and every book.

**Acceptance criteria** (the tests in `MemberOrdersApiTest`):

- The response stays the same: a page of the member's orders, newest first, each with its lines and the books' titles, with the right page metadata.
- A page of 10 orders costs at most 5 SQL statements.
- An order whose lines were all removed is still listed, with no lines.
- A member without orders gets an empty page; an unknown member gets `404 MEMBER_NOT_FOUND`.

**You'll practice:** reading an SQL log, `@EntityGraph`, fetch joins, batch fetching, and checking that a fix keeps the behavior.

<details>
<summary>Hint 1</summary>

Run the failing test and read the statements in its message (IntelliJ shows all of them). Which statements repeat, and what differs between them?
</details>

<details>
<summary>Hint 2</summary>

Lesson section 7 shows several fixes. The smallest is one annotation on the repository method.
</details>

<details>
<summary>Hint 3</summary>

If you write your own `join fetch` query, run all of the exercise's tests: is it an inner or a left join? Is the page's `totalElements` still 14? And does your query still sort?
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Which fix would you choose if the endpoint also had to show each order's payment? What would your fix do with Hibernate 6, which pages fetch joins in memory?

### S06-06 · Lazy loading outside a transaction

**Fix** · 30 min · [`OrderController.java`](src/main/java/com/example/bookstore/order/OrderController.java) · [`OrderQueryService.java`](src/main/java/com/example/bookstore/order/OrderQueryService.java) · [`PaymentService.java`](src/main/java/com/example/bookstore/payment/PaymentService.java)

> **ORDER-35 · The order page and receipts answer 500**
>
> Customer support reports that opening an order (`GET /api/orders/17`) or a receipt (`GET /api/payments/5`) shows *An unexpected error occurred*. The log has two errors:
>
> ```
> org.hibernate.LazyInitializationException: Cannot lazily initialize collection of role 'com.example.bookstore.order.Order.lines' with key '17' (no session)
> org.hibernate.LazyInitializationException: Could not initialize proxy [com.example.bookstore.order.Order#6] - no session
> ```
>
> The developer who wrote both endpoints says that all their tests passed. They did: those tests ran inside the test transaction.

**Acceptance criteria** (the tests in `OrderDetailApiTest` and `PaymentApiTest`, which run without the test transaction):

- `GET /api/orders/{orderId}` answers `200` with the order, its lines and the books' titles.
- `GET /api/payments/{paymentId}` answers `200` with the payment and its order's id and status.
- `404` for an unknown order or payment, as before.
- `spring.jpa.open-in-view` stays `false`. Fix the code, not the configuration: entities are mapped to records inside a service transaction, and controllers only see records.

**You'll practice:** reading `LazyInitializationException`, finding where a transaction begins and ends, mapping inside the service.

<details>
<summary>Hint 1</summary>

For each endpoint, find the line that throws, and ask: is a transaction active on that line? Which method started it, and when did it end?
</details>

<details>
<summary>Hint 2</summary>

The two endpoints fail for different reasons. In one, the mapping happens after the transaction. In the other, there never was a transaction around the mapping.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Why does `order.getMember().getId()` work after the transaction, but `order.getStatus()` on a proxy doesn't? Which test in your project at work would pass inside a test transaction and fail in production?

### S06-07 · Bulk updates

**Build** · 30 min · [`PriceChangeService.java`](src/main/java/com/example/bookstore/book/PriceChangeService.java) · [`BookRepository.java`](src/main/java/com/example/bookstore/book/BookRepository.java)

> **ADMIN-77 · Change the prices of a whole category**
>
> The purchasing team wants to change the prices of a whole category at once, for example "+10 % for Science", instead of editing books one by one. [`CategoryController`](src/main/java/com/example/bookstore/book/CategoryController.java) and the records `PriceChangeRequest` and `PriceChangeResponse` are done. Implement `PriceChangeService.changePrices` with a bulk update.

```json
POST /api/categories/5/price-changes
{ "percent": 10 }
```

**Acceptance criteria** (the Javadoc of `changePrices`, and the tests in `PriceChangeApiTest`):

- Every book of the category gets the price `old × (100 + percent) / 100`; books of other categories don't change.
- One `UPDATE` statement changes all the prices, however many books the category has.
- The response has the number of changed books and every book of the category with its old and new price.
- The `updated_at` of every changed book is the current time, from the `Clock`.
- Code that runs later in the same transaction sees the new prices, even for books it loaded before the change.
- An unknown category gives `404 CATEGORY_NOT_FOUND`; a percentage outside -50 to 50 gives `400` (the request's validation does that already).
- The change is committed when the request ends: one test runs without the test transaction.

**You'll practice:** `@Modifying` queries, arithmetic in JPQL, what bulk updates skip, `clearAutomatically` and `flushAutomatically`.

<details>
<summary>Hint 1</summary>

Add an `int` method with `@Modifying` and `@Query("update Book b set b.price = ... where b.category.id = :categoryId")`. JPQL can compute the new price from the old one: `b.price * (100 + :percent) / 100`.
</details>

<details>
<summary>Hint 2</summary>

Auditing doesn't run for a bulk update. The field in `BaseTimeEntity` is called `updatedAt`, and the service has a `Clock`.
</details>

<details>
<summary>Hint 3</summary>

To answer with old and new prices, load the books before the update and again after it. What does the second load return when the persistence context still holds the first one's objects?
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Which rule of `Book.changePrice` does your bulk update skip, and what protects it instead? When would you rather loop over the entities?

## Review questions

1. What are JPA, Hibernate and Spring Data JPA, and which of them writes the SQL?
2. Why does this codebase store enums with `EnumType.STRING`?
3. The application doesn't start: *Schema validation: missing column [note] in table [wishlist_items]*. What does that tell you, and what are two ways to fix it?
4. In a bidirectional `Order`–`OrderLine` relationship, which side writes `order_id`? What happens if you only add the line to `order.getLines()`?
5. What's the difference between `cascade = CascadeType.REMOVE` and `orphanRemoval = true`?
6. A service method loads an order and calls `order.cancel()`, but never calls `save()`. Is the change saved? When does the SQL run?
7. Why does saving a new entity with an `IDENTITY` id run an `INSERT` at once, when other changes wait for the flush?
8. What does `getReferenceById` return, and when does it run a query?
9. A list of 20 orders runs 21 SQL statements, and a list of 50 orders runs 51. What's going on, and how would you fix it?
10. Why is `spring.jpa.open-in-view` off in this codebase, and what's the right fix for a `LazyInitializationException` in a controller?
11. Why did the buggy endpoints of S06-06 pass tests that ran inside a test transaction?
12. Name three things a `@Modifying` bulk update skips.
13. Why should every paged list sort by a unique column last?
14. When would you use `JdbcClient` instead of a repository?

<details>
<summary>Answers</summary>

1. JPA is the specification: the annotations, the `EntityManager` API and JPQL. Hibernate implements it and writes and runs the SQL. Spring Data JPA builds repositories on top of the `EntityManager`, including queries derived from method names.
2. `STRING` stores the constant's name. The default, `ORDINAL`, stores its position, so adding or reordering constants silently changes the meaning of stored rows.
3. Hibernate looked for a column named after the field `note` and didn't find it. Either the field needs `@Column(name = "memo")`, or the column should be renamed with a new migration. Never let Hibernate change the table.
4. The owning side, `OrderLine.order` (the `@ManyToOne`), writes `order_id`. If only the list changes, the line's `order` is `null`, so Hibernate has no `order_id` to write. Set both sides in one method of the parent.
5. A cascaded `REMOVE` deletes the children when the parent is deleted. `orphanRemoval` also deletes a child as soon as it's removed from the parent's collection.
6. Yes, if the method runs in a transaction: dirty checking compares the managed order with its snapshot and writes the `UPDATE` at the flush before the commit.
7. Only MySQL can generate the next `AUTO_INCREMENT` value, and Hibernate needs the id to put the entity into the persistence context. So it runs the `INSERT` at once, which also means it can't batch those inserts.
8. A proxy that knows only the id. It runs a `SELECT` when you call a getter other than `getId()`, which also needs an open persistence context.
9. The N+1 problem: one query for the list, then one per order for its lazy lines. Load the lines with the list: a `left join fetch`, an `@EntityGraph`, or batch fetching.
10. With it on, lazy loading in the web layer runs outside any transaction and hides N+1 queries where nobody looks, and requests keep their connection longer. The fix is to map entities to records inside the service's transaction, and to load what the mapping needs in the same query.
11. The test transaction kept one persistence context open for the whole test, MockMvc requests included, so lazy loading worked in the controller.
12. The persistence context (loaded entities become stale), the entity's own rules, JPA callbacks such as auditing (`updated_at`), and the optimistic-locking `version` column.
13. Rows with equal values in the sort columns can come back in any order, so the same row can appear on two pages and another on none. A unique column at the end makes the order complete.
14. For reports and aggregations over many rows, MySQL-specific SQL, large inserts, and read-only lists where entities add nothing.
</details>

## Further reading

- Spring Data JPA reference: [JPA query methods](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html) (derived queries, `@Query`, `@Modifying`, entity graphs) · [Projections](https://docs.spring.io/spring-data/jpa/reference/repositories/projections.html)
- Spring Boot reference: [JPA and Spring Data JPA](https://docs.spring.io/spring-boot/reference/data/sql.html#data.sql.jpa-and-spring-data), including open EntityManager in view · [The Hibernate naming strategy](https://docs.spring.io/spring-boot/how-to/data-access.html#howto.data-access.configure-hibernate-naming-strategy)
- Hibernate 7.4: [A Short Guide to Hibernate 7](https://docs.hibernate.org/orm/7.4/introduction/html_single/) (persistence contexts, proxies and lazy fetching, association fetching) · [A Guide to Hibernate Query Language](https://docs.hibernate.org/orm/7.4/querylanguage/html_single/) · [User Guide](https://docs.hibernate.org/orm/7.4/userguide/html_single/) (batch fetching)
- [Jakarta Persistence 3.2](https://jakarta.ee/specifications/persistence/3.2/), the specification
- Vlad Mihalcea: [N+1 query problem with JPA and Hibernate](https://vladmihalcea.com/n-plus-1-query-problem/) · [The Open Session In View Anti-Pattern](https://vladmihalcea.com/the-open-session-in-view-anti-pattern/)
- [Hibernate 7.4 New Features](https://blog.jetbrains.com/idea/2026/05/hibernate-7-4-new-features/) (JetBrains), including pagination with fetch joins
