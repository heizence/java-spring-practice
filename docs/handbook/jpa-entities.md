# JPA entities and repositories

JPA (with Hibernate as the implementation) maps Java objects to database tables. It saves a lot of SQL, but it also hides a lot. These rules keep it predictable.

## Writing entities

```java
@Entity
@Table(name = "inventories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA needs it; nobody else should use it
public class Inventory {

    @Id
    private Long bookId;

    private int quantity;

    @Version
    private long version;

    public void decrease(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive, but was " + amount);
        }
        if (amount > quantity) {
            throw new BusinessException(ErrorCode.OUT_OF_STOCK,
                    "Only %d copies of book %d are left, but %d were requested.".formatted(quantity, bookId, amount));
        }
        quantity -= amount;
    }
}
```

- **A protected no-argument constructor** for JPA; create real objects through a constructor or static factory that validates.
- **No setters.** State changes go through methods named after what happens in the business: `decrease`, `cancel`, `markPaid`. Each one checks its rules.
- **`@Getter` is fine; `@Setter` and `@Data` are not** (see [Lombok](lombok.md)).
- **Enums with `@Enumerated(EnumType.STRING)`.** The default, `ORDINAL`, stores positions, and reordering the enum silently corrupts data.
- **Timestamps as `Instant`**, stored in UTC. Spring Data's auditing (`@CreatedDate`, `@LastModifiedDate`) fills `createdAt`/`updatedAt`.
- **`@Version`** turns on optimistic locking: an update that started from an outdated version fails instead of overwriting someone else's change (S08).
- **Generated ids with `GenerationType.IDENTITY`** for MySQL `AUTO_INCREMENT`.

## Relationships

- **Prefer `@ManyToOne(fetch = FetchType.LAZY)`.** The default for `@ManyToOne` is `EAGER`, which loads the related row every time, often with extra queries you never asked for.
- **Add `@OneToMany` only when the parent owns the children**, as an order owns its lines. Then use `mappedBy`, `cascade = CascadeType.ALL` and `orphanRemoval = true`, and change the children only through the parent (`order.addLine(…)`). A parent and its owned children form an *aggregate*: saved, loaded and deleted together.
- **Avoid `@ManyToMany`.** Model the link table as its own entity (`CouponIssue` links coupons and members). Link tables almost always grow extra columns (`issuedAt`).
- **Avoid bidirectional relationships unless you need them.** Each one is two pointers to keep in sync.

## The N+1 problem

Loading 20 orders and then touching each order's lines runs 1 query for the orders plus 20 for the lines: **N+1 queries**. It's the most common JPA performance bug, and it's invisible until you count queries.

Fixes:

| Fix | How | Good for |
|---|---|---|
| Fetch join | `@Query("select o from Order o join fetch o.lines where …")` | one query that loads exactly what this use case needs |
| Entity graph | `@EntityGraph(attributePaths = "lines")` on a repository method | the same, declared instead of written in JPQL |
| Batch fetching | `hibernate.default_batch_fetch_size: 100` | loading lazy collections in groups: N+1 becomes 1 + N/100 |
| A projection | a query that selects directly into a record | read-only screens and reports |

Don't fetch-join a collection *and* page in the database: Hibernate then pages in memory and warns about it. Page the parents first, then batch-fetch the children.

## The persistence context

Inside a transaction, Hibernate keeps every loaded entity in the *persistence context*:

- **Each row is loaded once.** Loading the same id twice returns the same object, without a second query.
- **Dirty checking.** At commit, Hibernate compares each entity with its loaded state and writes the changes. There's no `save()` call needed for an entity you loaded.
- **Writes are delayed.** SQL runs at *flush* time: at commit, before a query that needs fresh data, or when you call `flush()`.
- **Bulk updates bypass it.** `@Modifying @Query("update …")` changes rows directly, so entities already loaded become stale. Use `@Modifying(clearAutomatically = true)` or reload.

## Open session in view: turn it off

`spring.jpa.open-in-view=true` (Spring Boot's default, with a warning) keeps the database connection open until the HTTP response is written, so lazy loading "works" in controllers and JSON serialization. It hides N+1 problems and holds connections far longer than needed. Turn it off and load what each use case needs inside the service.

## Repositories

- **Derived queries** (`findByEmail`, `existsByIsbn`) for simple lookups.
- **`@Query` with JPQL** when the name would get long, or for fetch joins.
- **Projections** (an interface or a record) for read-only data, so you don't load and track whole entities.
- **Return `Optional<T>`** for single results that may be missing.
- **Paging:** accept a `Pageable` and return a `Page<T>`, or a `Slice<T>` when the total count isn't needed (it costs an extra `count` query).

## `equals` and `hashCode` on entities

Entities have identity, so two `Order` objects are "the same order" when they have the same id. But a new entity has no id until it's saved. Two safe conventions:

- don't override `equals`/`hashCode`, and rely on the persistence context to give you one object per row; or
- compare by id when both ids are non-null, and return a constant `hashCode` (for example, the class's hash), so the value never changes after saving.

Never include lazy relationships or mutable fields.

## Schema changes go through migrations

Use Flyway (or Liquibase) for every schema change, and let Hibernate only *validate* (`ddl-auto: validate`). `ddl-auto: update` guesses at schema changes; it never belongs in production.
