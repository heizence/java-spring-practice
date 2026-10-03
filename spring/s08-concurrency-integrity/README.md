# S08 · Concurrency and data integrity

⏱ 6 sessions · week 11 · 6 exercises

In J08 you made objects safe for many threads. In a backend, the shared state lives in the database instead, and the "threads" are requests: many of them at the same moment, often on several servers. `synchronized` can't protect a database row. A flash sale sells more books than the warehouse has, a double click pays an order twice, two members who send each other money at the same moment get an error. This module shows how such bugs happen in a Spring Boot service on MySQL, and the tools that prevent them: atomic `UPDATE` statements, optimistic locking with retries, pessimistic locking, consistent lock order, unique constraints and idempotency keys. You also learn to read the errors that MySQL and Spring report when transactions collide.

Each exercise names the lesson sections to read first. The tests send real concurrent requests to a real MySQL, so they need Docker; the module's tests take about a minute.

## Lesson

### 1. Races in a database-backed service

The two race patterns from J08 come back in the database:

- **Read-modify-write (lost update).** A transaction reads a value, computes a new value in Java, and writes it back. Another transaction does the same in between, and one of the two changes disappears.
- **Check-then-act.** A transaction checks a condition ("are copies left?", "does the member have this coupon yet?") and then acts on it. Another transaction changes the situation between the check and the action.

Here is the lost update with the stock of a book that has 10 copies. Each transaction runs `SELECT quantity`, subtracts in Java, and writes the result:

| Order A | Order B | `quantity` in the database |
|---|---|---|
| reads 10 | | 10 |
| | reads 10 | 10 |
| writes 9, commits | | 9 |
| | writes 9, commits | 9 ← two copies sold, one subtracted |

MySQL's default isolation level, `REPEATABLE READ` (S07), doesn't prevent this. Each transaction reads from its own snapshot, and nothing stops B from writing a value that it computed from an old read.

`synchronized` on the service method is not a fix, for three reasons:

1. Production runs several instances of the application. Each JVM has its own locks.
2. The transaction commits after the method returns (the transactional proxy commits it), so the next thread enters the method before the previous change is committed, and reads the old value.
3. A lock in Java makes every request for every book wait, also the ones that don't conflict.

The database has to decide. It offers four tools, and the next sections show each one: a statement that checks and changes in one step (section 2), a version check (section 3), a row lock (section 5) and a unique constraint (section 7).

### 2. Atomic `UPDATE` statements

Let the database check and change the row in one statement:

```sql
UPDATE inventories SET quantity = quantity - 2 WHERE book_id = 1 AND quantity >= 2
```

MySQL locks the row, evaluates the `WHERE` clause on the latest committed value, changes the row, and keeps the lock until the transaction ends. A second transaction that runs the same statement waits for the lock, and then sees the new value. The number of changed rows tells you whether the condition held: 1 means "the copies were taken", 0 means "not enough copies" (or no such row).

In Spring Data JPA, an `UPDATE` query needs `@Modifying` and returns the number of changed rows. Here is the same idea for a wallet that pays with one statement:

```java
@Modifying
@Query("""
        update Wallet w set w.balance = w.balance - :amount, w.version = w.version + 1
        where w.id = :walletId and w.balance >= :amount""")
int withdraw(long walletId, long amount);
```

```java
if (walletRepository.withdraw(walletId, amount) == 0) {
    throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE, "…");
}
```

Know what such a statement skips:

- **The persistence context.** The statement changes the row directly. A `Wallet` entity that this transaction loaded before keeps its old values. Load it again if you need it (or use `@Modifying(clearAutomatically = true)`).
- **`@Version`.** Hibernate increases the version only when it writes an entity. Increase it in the statement yourself (`w.version = w.version + 1`), so that code that uses optimistic locking on the same row notices the change (section 3).
- **The entity's rules.** `wallet.withdraw()` is not called, so the rule must be in the `WHERE` clause, and the history row (`WalletTransaction`) must be saved separately.

An atomic statement is the simplest and fastest fix whenever the whole rule fits into one row: counters, stock, balances, "at most N".

### 3. Optimistic locking with `@Version`

A field annotated `@Version` makes Hibernate check, on every update, that nobody changed the row since it was read:

```sql
UPDATE wallets SET balance = 110000, version = 8 WHERE id = 1 AND version = 7
```

If another transaction changed the wallet in between, the version is no longer 7, no row matches, and Hibernate throws. Spring translates it into an `ObjectOptimisticLockingFailureException`, a subclass of `OptimisticLockingFailureException`. Hibernate writes changes at flush time, usually when the transaction commits, so the exception comes from the commit, after your service method has returned.

"Optimistic" means that nothing is locked while the transaction runs: you hope that nobody else changes the row, and you detect it when somebody did. That fits rows that change rarely, and long editing cycles (an administrator edits a book for five minutes; the version travels to the browser and back). Under heavy contention, most attempts fail and must be repeated.

When the conflict happens, either report it to the client (`409`, "try again"), or try again yourself, in a new transaction that reads the new value (section 4).

**A MySQL detail that matters here: foreign keys take shared locks.** When you insert a row with a foreign key, such as a `wallet_transactions` row that points to its wallet, MySQL takes a *shared* lock on the parent row (the wallet) to make sure that it exists. A shared lock lets other transactions read the row, and take shared locks too, but nobody can change the row until it is released. Now take two charges of the same wallet:

| Charge A | Charge B |
|---|---|
| reads the wallet (version 7) | reads the wallet (version 7) |
| inserts its history row: shared lock on the wallet | inserts its history row: shared lock on the wallet |
| commits: `UPDATE wallets …` needs an exclusive lock, waits for B | commits: `UPDATE wallets …` waits for A |

Each waits for the other: a **deadlock** (section 6). MySQL notices it at once and rolls back one of the two. So with MySQL, two charges that collide often end in a deadlock, which Spring reports as `CannotAcquireLockException`, not in a failed version check. Both mean the same thing for you: the transaction collided with another one and can be tried again. They share a parent class:

```
DataAccessException
├── NonTransientDataAccessException          the same request will fail again
│   └── DataIntegrityViolationException      unique key, foreign key, NOT NULL
└── TransientDataAccessException             another attempt may work
    └── ConcurrencyFailureException          the transaction collided with another one
        ├── OptimisticLockingFailureException     a @Version check failed
        │   └── ObjectOptimisticLockingFailureException
        └── PessimisticLockingFailureException
            └── CannotAcquireLockException        deadlock, lock wait timeout, NOWAIT
```

(With plain JDBC, through `JdbcClient`, a duplicate key arrives as `DuplicateKeyException`, a subclass of `DataIntegrityViolationException`. Through JPA it arrives as `DataIntegrityViolationException`.) In this module's checkpoint, `GlobalExceptionHandler` answers every `ConcurrencyFailureException` with `409 CONCURRENT_UPDATE`, so a client knows that it may send the request again.

### 4. Retrying with `@Retryable`

Spring Framework 7 has retries built in. Switch them on once, on a configuration class:

```java
@Configuration(proxyBeanMethods = false)
@EnableResilientMethods
public class ResilienceConfiguration {
}
```

Then annotate the method, for example an administrator's restock of a book:

```java
@Retryable(includes = OptimisticLockingFailureException.class, maxRetries = 4, delay = 50, jitter = 25, multiplier = 2)
@Transactional
public void restock(long bookId, int copies) { … }
```

- **What to retry.** `includes` lists the exceptions that a new attempt can fix, and their subclasses match too. (Section 3 explains why, in MySQL, a version check is often not the only kind of collision.) Without `includes`, Spring retries every exception, also `BOOK_NOT_FOUND`: the request fails anyway, only five times slower. `excludes` lists exceptions that are never retried.
- **How often.** `maxRetries = 4` means up to 5 attempts in total. The default is 3.
- **How long to wait.** `delay` (in milliseconds) is the wait before the first retry. The default is a full second. `multiplier = 2` doubles the wait each time (exponential backoff), `maxDelay` caps it, and `jitter` adds a random part (up to ±25 ms here, growing with the multiplier, but never below `delay`). Without jitter, requests that collided once wait exactly as long and collide again.
- **When it gives up.** The last exception reaches the caller.
- **Values from configuration.** Each attribute has a `…String` twin that accepts placeholders: `delayString = "${bookstore.stock.retry-delay:50ms}"`.

**Retry outside the transaction.** A retry is only useful if every attempt starts a new transaction that reads the data again. When `@Retryable` and `@Transactional` sit on the same method, Spring puts the retry around the transaction, so that's what happens. Two traps:

- A retry *inside* a transaction can't work: the version conflict appears at the commit, after the method has returned, and the persistence context would keep the stale entity anyway.
- Like `@Transactional`, `@Retryable` works through a proxy. A call from another method of the same class bypasses the proxy and isn't retried (the self-invocation trap from S03).

**Retry only what is safe to repeat.** A failed attempt must leave nothing behind. A transaction that was rolled back is fine. An email that was sent, or a call to another service, is not: those belong after the commit (S09), or need an idempotency key of their own (section 8).

For retries in code without an annotation, Spring Framework 7 has `RetryTemplate` (`org.springframework.core.retry`).

### 5. Pessimistic locking with `SELECT … FOR UPDATE`

Sometimes you want transactions to wait in line instead of failing: a popular book during a sale, or a rule that needs several reads before the write. A locking read takes an exclusive lock on the rows it returns and holds it until the transaction ends. `WalletRepository` has one:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select w from Wallet w where w.member.id = :memberId")
Optional<Wallet> findByMemberIdForUpdate(Long memberId);
```

Hibernate adds `for update` to the SQL. Another transaction that wants to lock or change the same row waits until this one commits or rolls back. Plain reads (`findById`) don't wait: they read their snapshot. `PESSIMISTIC_READ` gives `FOR SHARE`, a shared lock that only blocks writers; you rarely need it.

**Lock first, then read.** Under `REPEATABLE READ`, a transaction's plain reads see a snapshot that is taken at its **first plain read**. A locking read always sees the latest committed row. So if you read something before you take the lock, the snapshot is older than the lock: while you waited, other transactions committed changes that your later plain reads in this transaction don't see.

```java
Member member = memberRepository.findById(memberId).orElseThrow();        // snapshot taken here, at 10:00:00.000
Inventory stock = inventoryRepository.findForUpdate(bookId).orElseThrow(); // waits until 10:00:00.040 for the lock
long held = reservationRepository.sumQuantity(bookId, memberId);           // still sees the data of 10:00:00.000
```

Swap the first two lines and the sum is correct. (The persistence context has a similar trap: an entity loaded before the locking query keeps its old field values.)

**Lock timeouts.** A transaction waits for a row lock at most for MySQL's `innodb_lock_wait_timeout`, which is **50 seconds** by default: far too long for a web request. This module's `application.yml` sets it to 2 seconds for every connection:

```yaml
spring.datasource.hikari.data-source-properties:
  sessionVariables: innodb_lock_wait_timeout=2
```

After the timeout, MySQL fails the statement with "Lock wait timeout exceeded" (error 1205), and Spring throws `CannotAcquireLockException`. Catch it where you know what was locked, and tell the client what happened: `409 STOCK_BUSY`, "Another request is changing the stock of book 1. Try again in a moment."

You may read that the JPA hint `jakarta.persistence.lock.timeout` sets a lock timeout per query. On MySQL, Hibernate supports only two values: `0` becomes `FOR UPDATE NOWAIT` (fail at once if the row is locked; MySQL error 3572, also a `CannotAcquireLockException`), and `-2` becomes `FOR UPDATE SKIP LOCKED` (skip locked rows, useful for job queues). A positive value is ignored, because MySQL has no `FOR UPDATE WAIT n`. On PostgreSQL and Oracle the hint works.

A lock holds everyone else up, so keep locking transactions short: lock as late as the rules allow, no remote calls while you hold it, and commit.

### 6. Deadlocks

A **deadlock** happens when transactions wait for each other in a circle, as with Java locks in J08. A transfer that locks the sender's wallet first and the receiver's second deadlocks with a transfer in the other direction:

| Transfer Minji → Jun | Transfer Jun → Minji |
|---|---|
| locks Minji's wallet | locks Jun's wallet |
| waits for Jun's wallet … | waits for Minji's wallet … |

Unlike a JVM, MySQL detects deadlocks at once. It rolls back one of the transactions (the *victim*, usually the one that changed fewer rows), and the other one continues. The victim gets error 1213, "Deadlock found when trying to get lock; try restarting transaction", which Spring throws as `CannotAcquireLockException`. `SHOW ENGINE INNODB STATUS` shows the latest deadlock in its "LATEST DETECTED DEADLOCK" section: both transactions, their statements, and the locks they held and wanted.

How to avoid them:

- **Lock in a consistent order.** If every transaction locks wallets in the order of their ids, two transfers between the same members both start with the same wallet, and one of them waits for the other. The same holds for any transaction that changes several rows: `OrderService` takes the stock of an order's books in book-id order.
- **Take the strongest lock first.** Section 3 showed the second classic: two transactions take shared locks on the same row (here, through a foreign key) and then both want an exclusive lock. Lock or change the parent row first (`SELECT … FOR UPDATE`, or the `UPDATE`), then insert the children.
- **Keep transactions short and small.** Fewer locks, held for less time, collide less often.

Some deadlocks can't be designed away: MySQL also locks index ranges and gaps, and the details depend on the indexes and the plan. The victim's transaction was rolled back completely, so it's safe to run the whole transaction again. Production code therefore often retries `CannotAcquireLockException` a few times (section 4), as a safety net. It's not a fix for a lock order that you control: a deadlock that keeps happening costs time on every collision, and logs a warning each time.

### 7. Unique constraints as concurrency control

"Is this email address registered yet? If not, register it" is check-then-act. Two sign-ups with the same address at the same moment both see "no", and both insert. A **unique constraint** is the one check that can't race: the database checks and inserts in one step, and makes a second insert of the same key wait until the first transaction has ended. If the first one committed, the second fails with a duplicate-key error:

```java
if (memberRepository.existsByEmail(email)) {          // the friendly check, for the usual case
    throw new BusinessException(ErrorCode.DUPLICATE_EMAIL, "…");
}
try {
    memberRepository.saveAndFlush(Member.register(email, name, passwordHash)); // uk_members_email
} catch (DataIntegrityViolationException e) {           // the race that the check can't see
    throw new BusinessException(ErrorCode.DUPLICATE_EMAIL, "…");
}
```

- Keep the friendly check in front: it answers the usual case without an exception. The constraint is the guarantee for the rare case where two requests race.
- Throw after the catch; don't continue in the same transaction. Spring has marked it for rollback, and Hibernate's session shouldn't be used after an exception. The rollback also undoes everything else the transaction did before the insert.
- With JPA and `IDENTITY` ids, `save()` runs the `INSERT` at once. `saveAndFlush()` makes that visible in the code, and also writes any other pending changes, so the violation surfaces inside your `try`.
- Catch only the violation you expect. A real application can check the constraint name in the exception before it decides which error the client gets.

### 8. Idempotency keys

A client that sends `POST /api/orders/201/payments` and gets a timeout doesn't know whether the payment went through. If it sends the request again, the member may pay twice. (`POST` isn't idempotent: repeating it can have a different effect from sending it once. See [REST API design](../../docs/handbook/rest-api-design.md).)

The fix is an **idempotency key**: the client creates a unique key for each payment it wants to make, such as a UUID, and sends it with every attempt:

```
POST /api/orders/201/payments
Idempotency-Key: 4f7b9a52-8c1e-4a8e-9d3f-2b6c1e0a7d55
```

The server stores the key with the result, under a unique constraint (`uk_payments_idempotency_key`), and answers a repeated key with the stored result instead of paying again:

```java
public PaymentResult pay(long orderId, String key) {
    Optional<PaymentResult> earlier = earlierPayment(orderId, key);      // its own transaction
    if (earlier.isPresent()) {
        return earlier.get();                                            // a repeated request
    }
    try {
        return transactionTemplate.execute(status -> payOnce(orderId, key)); // saves the payment with the key
    } catch (DataIntegrityViolationException duplicateKey) {
        return earlierPayment(orderId, key).orElseThrow(() -> duplicateKey); // lost the race: a NEW transaction
    }
}
```

Two requests with the same key at the same moment both find no earlier payment. The unique key lets only one of them save its payment; the other one fails with a `DataIntegrityViolationException`. It must read the winner's payment **in a new transaction**: the failed transaction is rolled back, and under `REPEATABLE READ` its snapshot is older than the winner's commit anyway. That's why `pay` itself isn't `@Transactional` and uses `TransactionTemplate` (S07-06) for each step.

Design choices to make and document:

- **A key used for a different request.** Answer with an error (here `409 IDEMPOTENCY_KEY_REUSED`), never with the other request's result.
- **A request that failed.** This bookstore records nothing, so the client may use the same key again later. Some providers store the failure too, and replay it.
- **A request that is still running.** Here the second request waits for the first one and then returns its result. The IETF draft "The Idempotency-Key HTTP Header Field" suggests answering `409` while the first one runs, and `422` for a key that is reused with a different request; the bookstore uses `409` for every broken rule.
- **How long to keep keys.** Payments are kept forever here. A separate key table would delete keys after, say, 24 hours.

### 9. Choosing a strategy

| Strategy | How | Good for | Watch out for |
|---|---|---|---|
| Atomic `UPDATE` | `UPDATE … SET x = x - ? WHERE … AND x >= ?`, check the row count | counters, stock, balances: rules that fit in one row | skips the persistence context, `@Version` and the entity's rules |
| Optimistic locking | `@Version`, then retry or answer `409` | rows that rarely collide; long editing cycles | retries pile up under contention; in MySQL, collisions often end as deadlocks |
| Pessimistic locking | `@Lock(PESSIMISTIC_WRITE)` (`SELECT … FOR UPDATE`) | hot rows; rules that read several rows before writing | lock first; set a short lock wait timeout; deadlocks if the order isn't fixed |
| Consistent lock order | sort the ids, then lock | every transaction that locks several rows | every code path must follow the same order |
| Unique constraint | `UNIQUE KEY`, catch `DataIntegrityViolationException` | "at most one" rules: one coupon per member | throw after the catch; the transaction rolls back |
| Idempotency key | the client's key under a unique constraint | repeated non-idempotent requests (`POST`) | read the winner in a new transaction |

They combine. The coupon in S08-05 needs an atomic counter (or a lock) for "at most 10 copies" and a unique constraint for "one per member"; the payment in S08-06 needs a row lock and an idempotency key.

### 10. Testing concurrent requests

A test inside a rolled-back transaction can't show races: all its requests share one transaction. So every test in this module commits, like production requests (`@Transactional(propagation = NOT_SUPPORTED)`), and calls `seedData.restore()` afterwards, which recreates the seed data in about a quarter of a second.

The concurrency tests send many requests at the same moment with `Responses.sendAtOnce` (in `support/`): it starts one thread per request with J08's starting gate (`practice.testkit.Concurrently`), and collects the answers. Then they check what each request got, and the invariants in the database:

```java
Responses responses = Responses.sendAtOnce(40, i -> placeOrder(1, 1));

assertThat(responses.countByOutcome()).isEqualTo(Map.of("201", 10L, "409 OUT_OF_STOCK", 30L));
assertThat(stockOf(1)).isZero();
```

A failing message like `expected {201=10, 409 OUT_OF_STOCK=30} but was {201=40}` tells you what went wrong. When you see `409 CONCURRENT_UPDATE` or `500 INTERNAL_ERROR`, read the warning or error that the test logged: it names the collision, such as "Deadlock found when trying to get lock".

## TypeScript/NestJS → Spring

| TypeScript / NestJS | Spring | Watch out for |
|---|---|---|
| TypeORM `@VersionColumn()` | `@Version` | Hibernate checks it when it writes, usually at commit |
| TypeORM `setLock("pessimistic_write")`; Prisma `$queryRaw` with `FOR UPDATE` | `@Lock(LockModeType.PESSIMISTIC_WRITE)` | the timeout hint doesn't work on MySQL |
| Prisma `updateMany({ where: { quantity: { gte: n } }, data: { quantity: { decrement: n } } })` and its `count` | `@Modifying @Query("update … where … >= :n")` returning `int` | skips the persistence context and `@Version` |
| catching Prisma error `P2002` (unique constraint failed) | catching `DataIntegrityViolationException` | throw afterwards; the transaction rolls back |
| a retry helper such as `p-retry` | `@Retryable` + `@EnableResilientMethods` | the retry must wrap the transaction |
| an interceptor that stores `Idempotency-Key` in Redis | the key in a table with a unique constraint | read the winner in a new transaction |

## In real projects

- Never compute a new value in Java from a value you read without a lock, and write it back. Use an atomic `UPDATE`, `@Version` or a row lock. [Transactions](../../docs/handbook/transactions.md)
- Put `@Version` on entities that several requests may change, and map the conflict to `409`, so that the client can try again. [JPA entities](../../docs/handbook/jpa-entities.md) · [REST API design](../../docs/handbook/rest-api-design.md)
- Retry only transient failures (`ConcurrencyFailureException`), a bounded number of times, with backoff and jitter, and only around a whole transaction without side effects outside the database. [Transactions](../../docs/handbook/transactions.md)
- Lock rows in one global order, take the strongest lock first, and keep locking transactions short. Set `innodb_lock_wait_timeout` to a few seconds. [Transactions](../../docs/handbook/transactions.md)
- Back every "at most one" rule with a unique constraint, even when the code checks first. The database is the only place where the check can't race. [JPA entities](../../docs/handbook/jpa-entities.md)
- Accept an `Idempotency-Key` on every `POST` that moves money or can't be repeated safely. [REST API design](../../docs/handbook/rest-api-design.md)
- Log collisions at `WARN` with the database's message, and count retries and deadlocks in your metrics: a sudden increase shows a contention problem before users report it. [Logging and observability](../../docs/handbook/logging-and-observability.md)
- Test concurrency with real commits against the real database, many requests at once, and assertions on invariants ("copies sold + copies left = copies there were"). [Testing Spring applications](../../docs/handbook/spring-testing.md)

## Exercises

### S08-01 · Lost updates

**Fix** · 30 min · [`StockService.java`](src/main/java/com/example/bookstore/inventory/StockService.java) · [`InventoryRepository.java`](src/main/java/com/example/bookstore/inventory/InventoryRepository.java)

**Before you start:** lesson sections 1 and 2.

Bug report from the warehouse: "In Friday's flash sale, *Effective Java* had 10 copies in stock, and the shop accepted orders for 37. We had to cancel 27 orders." Some history: during the sale before, many orders failed with `409 CONCURRENT_UPDATE`, because `inventory.decrease()` collided on the inventory's `@Version`. So a developer replaced it with an `UPDATE` statement that doesn't check the version. The errors are gone, and so is the protection.

**Ticket:** fix `StockService.takeOut` (called by `POST /api/orders`) with an atomic conditional `UPDATE`, where the number of changed rows decides whether the order gets its copies.

**Acceptance criteria** (the tests in `order/PlaceOrderTest`):

- 40 orders for one copy each, sent at the same moment, sell exactly the 10 copies in stock; 30 get `409 OUT_OF_STOCK`.
- Orders of different sizes, sent at the same moment, never take more copies than there were.
- Every order increases the inventory's `version`, so code that uses optimistic locking on the stock notices the change.
- An order for more copies than are left fails with `409 OUT_OF_STOCK` and the usual message, and changes nothing. When one book of an order is missing, no stock changes.

**You'll practice:** recognizing a lost update, `@Modifying @Query` with a condition, using the row count, keeping `@Version` up to date.

<details>
<summary>Hint 1</summary>

Read `takeOut` line by line and ask: what happens if another order changes the stock between this line and the next one? Then look at the repository method it calls. Which value does it write?
</details>

<details>
<summary>Hint 2</summary>

Replace `updateQuantity` with a method whose `UPDATE` subtracts from the current value and has the stock check in its `WHERE` clause. `takeOut` then needs no `findById` before the update, only after it, when it changed nothing and you need the numbers for the error message.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Why did the old `inventory.decrease()` give `409` errors instead of overselling? Would `synchronized` on `takeOut` have fixed the bug? (Try it: the tests tell you.)

### S08-02 · Optimistic locking

**Build** · 30 min · [`WalletService.java`](src/main/java/com/example/bookstore/wallet/WalletService.java) · [`ResilienceConfiguration.java`](src/main/java/com/example/bookstore/common/ResilienceConfiguration.java)

**Before you start:** lesson sections 3 and 4.

Members charge their wallets from the app, and an automatic top-up charges them when the balance runs low. Both can hit the same wallet at the same moment. `Wallet` already has a `@Version`, so a collision can't lose money, but today it would fail with an error.

**Ticket:** implement `WalletService.charge` for `POST /api/members/{memberId}/wallet/charges` with `{ "amount": 10000 }`. It answers `200` with the wallet after the charge. A charge that collides with another one is tried again until it goes through, a bounded number of times.

**Acceptance criteria** (the tests in `wallet/WalletChargeTest`):

- A charge adds the amount, records a `CHARGE` wallet transaction at the clock's time, and increases the wallet's version.
- 10 charges sent at the same moment all answer `200`, and each one is applied exactly once: the balance grows by the sum, with one `CHARGE` per charge. The same for 4 clients that each send 5 charges in a row.
- A charge for a member who doesn't exist answers `404 MEMBER_NOT_FOUND` at once, without retries.
- All rules are in the Javadoc of `charge`.

**You'll practice:** `@Version`, `@Retryable` and `@EnableResilientMethods`, choosing what to retry, backoff and jitter, the Spring exception hierarchy.

<details>
<summary>Hint 1</summary>

`charge` itself is three lines: find the wallet, save `wallet.deposit(amount, WalletTransactionType.CHARGE)`, return `WalletResponse.from(wallet)`. The work is in the annotations. If no attempt is retried at all, check `ResilienceConfiguration`.
</details>

<details>
<summary>Hint 2</summary>

If the concurrency tests show `409 CONCURRENT_UPDATE`, read the warnings that the test logged. "Deadlock found" means that your retry doesn't cover deadlocks: lesson section 3 explains why MySQL reports most of these collisions as deadlocks, and which parent class covers both kinds.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: How many attempts can one charge make, and how long can it wait in total? What would happen with `@Retryable` on a private method that `charge` calls?

### S08-03 · Pessimistic locking

**Build** · 30 min · [`ReservationService.java`](src/main/java/com/example/bookstore/reservation/ReservationService.java) · [`InventoryRepository.java`](src/main/java/com/example/bookstore/inventory/InventoryRepository.java)

**Before you start:** lesson section 5.

New feature: members reserve books online and pick them up in the store. The copies leave the online stock at once. The stores want fairness for books with few copies: a member may hold at most 2 copies of a book in reservations. The rule needs two reads (the stock and the member's reservations) before the write, so one atomic `UPDATE` can't express it; reservations of the same book should wait in line instead. Everything except `reserve` exists: the `reservations` table (`V3__create_reservations.sql`), the entity, the controller, and the sum query in `ReservationRepository`.

**Ticket:** implement `ReservationService.reserve` for `POST /api/books/{bookId}/reservations` with `{ "memberId": 1, "quantity": 2 }`, and add a locking query to `InventoryRepository`. It answers `201` with the reservation and its `Location`.

**Acceptance criteria** (the tests in `reservation/ReservationTest`):

- A reservation takes its copies out of stock, up to the last one, and is saved with the clock's time.
- A member may hold 2 copies of a book (and 2 of every other book). More fails with `409 RESERVATION_LIMIT_EXCEEDED`.
- A book that is out of stock: `409 OUT_OF_STOCK`. An unknown book or member: `404 BOOK_NOT_FOUND` or `404 MEMBER_NOT_FOUND`.
- 20 members who reserve one copy each at the same moment get exactly the 10 copies in stock. A member who sends 10 reservations at the same moment gets exactly 2.
- While another transaction holds the stock's lock, a reservation gives up after the lock wait timeout (2 seconds) with `409 STOCK_BUSY` and the message "Another request is changing the stock of book 1. Try again in a moment."
- A reservation that fails changes nothing. All rules are in the Javadoc of `reserve`.

**You'll practice:** `@Lock(LockModeType.PESSIMISTIC_WRITE)`, the order of locking and reading, lock wait timeouts, turning `CannotAcquireLockException` into a clear error.

<details>
<summary>Hint 1</summary>

`WalletRepository.findByMemberIdForUpdate` shows the shape of a locking query. Name yours `findForUpdate(long bookId)`. In `reserve`, call it first, before you load the member.
</details>

<details>
<summary>Hint 2</summary>

`Inventory.decrease()` already checks the stock and throws `OUT_OF_STOCK`; Hibernate writes the new quantity at commit. For the timeout, put the locking call in a small private method with a `try`/`catch` for `PessimisticLockingFailureException` (`CannotAcquireLockException` is a subclass).
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: What do the concurrency tests do if you load the member before the lock? Why? Could an atomic `UPDATE` have done this job?

### S08-04 · Deadlocks

**Fix** · 30 min · [`TransferService.java`](src/main/java/com/example/bookstore/wallet/TransferService.java)

**Before you start:** lesson section 6.

Bug report from customer support: "Two friends split a dinner bill and sent each other money at the same moment. One of the transfers failed with `409 CONCURRENT_UPDATE`. It happens every few days." The logs show `Deadlock found when trying to get lock; try restarting transaction` for `POST /api/members/{memberId}/wallet/transfers`.

**Ticket:** fix `TransferService.transfer` so that transfers can't deadlock, without giving up the row locks.

**Acceptance criteria** (the tests in `wallet/WalletTransferTest`):

- Two members who send each other money at the same moment, 30 times in a row, never deadlock: all 60 transfers answer `200`, and both balances end where they started.
- Random transfers among four members on 8 threads never collide (only `200`, or `409 INSUFFICIENT_BALANCE` when a member runs out of money), and the total of the four balances doesn't change.
- A transfer still records `TRANSFER_OUT` for the sender and `TRANSFER_IN` for the receiver, in both directions, and the errors (`409 INSUFFICIENT_BALANCE`, `404 MEMBER_NOT_FOUND`, `400` for a transfer to oneself) stay as they are.

**You'll practice:** reading a deadlock, a global lock order, retries as a safety net.

<details>
<summary>Hint 1</summary>

Imagine the two transfers of the bug report side by side, as in the table in lesson section 6. Which wallet does each one lock first?
</details>

<details>
<summary>Hint 2</summary>

Lock the wallet of the lower member id first, whatever the direction. After locking, be careful to take the money from the right wallet: the tests send money to lower and to higher ids.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Would a retry of `CannotAcquireLockException` alone have made the tests pass? Why is lock order still the better fix? Look at how `OrderService` handles orders with several books.

### S08-05 · First-come coupons

**Build** · 30 min · [`CouponService.java`](src/main/java/com/example/bookstore/coupon/CouponService.java) · [`CouponRepository.java`](src/main/java/com/example/bookstore/coupon/CouponRepository.java)

**Before you start:** lesson sections 7 and 9, and section 2 or 5 for the counter.

Marketing announces "10,000 won off for the first 10 members" with the coupon `FLASH10000` (10 copies, valid in October 2026). At the announced minute, thousands of members tap the button at once.

**Ticket:** implement `CouponService.issue` for `POST /api/coupons/{code}/issues` with `{ "memberId": 1 }`. It answers `201` with the coupon issue and its `Location`. Choose your strategy for "at most 10 copies" (section 9); the tests accept any correct one.

**Acceptance criteria** (the tests in `coupon/CouponIssueTest`):

- 100 members who ask at the same moment get exactly 10 copies; the other 90 get `409 COUPON_SOLD_OUT`. The coupon's `issuedQuantity` ends at 10, with 10 issues.
- A member who asks 10 times at the same moment gets one copy; the other 9 requests get `409 COUPON_ALREADY_ISSUED`, never `500`.
- A member who already has the coupon (members 1–15 have `WELCOME5000`): `409 COUPON_ALREADY_ISSUED`. When all copies are gone: `409 COUPON_SOLD_OUT`.
- The validity period counts, to the second, both ends included: `409 COUPON_NOT_YET_VALID` before it, `409 COUPON_EXPIRED` after it (`EXPIRED2000`).
- An unknown coupon or member: `404 COUPON_NOT_FOUND` or `404 MEMBER_NOT_FOUND`. A request that fails changes nothing. All rules are in the Javadoc of `issue`.

**You'll practice:** combining strategies: an atomic counter or a row lock for the quantity, a unique constraint for "one per member", handling `DataIntegrityViolationException`.

<details>
<summary>Hint 1</summary>

`Coupon.issue()` checks the period and the copies left in Java, on the value this transaction read: alone, that's check-then-act. Either lock the coupon row before you read it, or count the copy with a conditional `UPDATE` (add it to `CouponRepository`) and create the issue with `new CouponIssue(coupon, member, now)`. `coupon.checkValidAt(now)` checks the period on its own.
</details>

<details>
<summary>Hint 2</summary>

Mind the order of the writes: change the coupon row (or lock it) before you insert the issue, because the issue's foreign key takes a shared lock on the coupon (lesson section 3). Then catch `DataIntegrityViolationException` around the insert and throw `COUPON_ALREADY_ISSUED`.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Which strategy did you choose, and what would the other one change? What happens to the copy you counted when the insert fails on the unique key?

### S08-06 · Idempotent payments

**Build** · 30 min · [`PaymentService.java`](src/main/java/com/example/bookstore/payment/PaymentService.java)

**Before you start:** lesson section 8, and the foreign-key lock in section 3.

The mobile app retries a payment request when the network times out, and some members tap "Pay" twice. Both have charged members twice. From now on, the app sends an `Idempotency-Key` header with every payment, the same key for every attempt of the same payment.

**Ticket:** implement `PaymentService.pay` for `POST /api/orders/{orderId}/payments` with an `Idempotency-Key` header. The controller answers `201` with the new payment and its `Location`, or `200` with the earlier payment for a repeated key (`PaymentResult.created`). The order must be `PENDING`; the amount is the order's total, taken from the member's wallet.

**Acceptance criteria** (the tests in `payment/PaymentTest`):

- A payment takes the total from the wallet (a `PAYMENT` wallet transaction), marks the order `PAID` and answers `201`.
- The same request again, now or an hour later, answers `200` with the same payment, and charges nothing.
- 10 requests with the same key at the same moment: one `201`, nine `200`, all with the same payment id; the wallet is charged once.
- 5 requests with different keys for the same order at the same moment: one pays, the others get `409 INVALID_ORDER_STATUS`.
- A new key for an order that is already paid: `409 INVALID_ORDER_STATUS`. A key that was used for another order: `409 IDEMPOTENCY_KEY_REUSED`.
- Not enough money: `409 INSUFFICIENT_BALANCE`, and nothing is recorded, so the same key works later. An unknown order: `404 ORDER_NOT_FOUND`. A request without the header: `400 INVALID_INPUT`.
- All rules are in the Javadoc of `pay`.

**You'll practice:** idempotency keys, a unique constraint as the final guard, handling the race in a new transaction with `TransactionTemplate`, locking the parent row first.

<details>
<summary>Hint 1</summary>

Split the work into three private methods: one that looks for an earlier payment with the key (in a `transactionTemplate.execute(…)` of its own), one that pays (the body of the main transaction), and `pay`, which calls them in the order of the code in lesson section 8.
</details>

<details>
<summary>Hint 2</summary>

Inside the paying transaction: lock the order (`orderRepository.findByIdForUpdate`), save the payment with `saveAndFlush` (this claims the key), then `order.markPaid()`, then withdraw from the wallet. If you check the status before you claim the key, a repeated request that waited for the lock sees a `PAID` order and answers `409` instead of the first payment.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Why must the winner's payment be read in a new transaction? What would the client see if the server answered a concurrent repeat with `409`, as the IETF draft suggests?

## Review questions

1. Two requests read a book's stock (10), each subtracts 1 in Java and writes the result. What is in the database afterwards, and why doesn't MySQL's default isolation level prevent it?
2. Why doesn't `synchronized` on a `@Transactional` service method prevent lost updates, even with only one server?
3. What does `UPDATE … WHERE quantity >= :n` give you that `findById` plus `if` plus `UPDATE … SET quantity = :newValue` doesn't? What does the returned number mean?
4. A bulk `UPDATE` statement changes a row of a versioned entity. What goes wrong if it doesn't increase the version?
5. When does Hibernate notice an optimistic-locking conflict, and why does that matter for where you put the retry?
6. Two transactions charge the same wallet, and one of them gets "Deadlock found" instead of a version conflict. Explain the shared lock involved.
7. Which exceptions should a retry include, and which must it never include? What does `@Retryable` do without `includes`?
8. Under `REPEATABLE READ`, why must a transaction take its lock before it reads anything else?
9. How long does a MySQL transaction wait for a row lock by default? How do you shorten it, and why does the JPA hint `jakarta.persistence.lock.timeout = 1000` not help on MySQL?
10. Describe how two money transfers deadlock, how MySQL resolves it, and two ways to prevent it.
11. A code path checks `existsByCouponIdAndMemberId` before inserting. Why do you still need the unique constraint, and what must the code do when it fires?
12. A payment request with an idempotency key fails on the unique key because a concurrent request won. Why can't it read the winner's payment in its own transaction?

<details>
<summary>Answers</summary>

1. 9: one decrement is lost. Each transaction reads 10 from its own snapshot and writes 9. `REPEATABLE READ` guarantees that a transaction keeps seeing the same data; it doesn't stop a transaction from writing a value that it computed from data that is no longer current.
2. The proxy commits the transaction after the method returns, so the lock is released before the change is visible: the next thread reads the old value. And production runs several JVMs, each with its own locks.
3. The database checks and changes the row in one step, while it holds the row's lock, so nobody can change the row between the check and the change, and the subtraction starts from the current value. The returned number is the number of changed rows: 1 if the condition held, 0 if it didn't (or the row doesn't exist).
4. Code that loaded the entity before the statement and saves it afterwards still finds the old version, so its `WHERE version = ?` matches, and it overwrites the statement's change: a lost update again.
5. When it writes the entity: at flush time, usually during the commit, after the service method has returned. So the retry must wrap the whole transaction, and each attempt must be a new transaction that reads the row again.
6. Inserting the wallet transaction checks its foreign key, which takes a shared lock on the wallet row. Both transactions hold that shared lock, and then both want the exclusive lock for `UPDATE wallets`, which each one's shared lock blocks. MySQL rolls one back: `CannotAcquireLockException`.
7. Transient failures that a new attempt can fix: `ConcurrencyFailureException` (version conflicts, deadlocks, lock timeouts). Never business errors such as `MEMBER_NOT_FOUND` or `OUT_OF_STOCK`, which fail again, nor failures after side effects outside the transaction. Without `includes`, `@Retryable` retries every exception.
8. Plain reads see a snapshot taken at the first plain read. If that happens before the lock, the transaction can't see what other transactions committed while it waited for the lock, and makes its decision on old data. A locking read always sees the latest committed row.
9. 50 seconds (`innodb_lock_wait_timeout`). Set it per connection, for example with Connector/J's `sessionVariables` (this module uses 2 seconds). MySQL has no `FOR UPDATE WAIT n`, so Hibernate can express only `NOWAIT` (0) and `SKIP LOCKED` (-2) there, and ignores other values.
10. Transfer A→B locks A, transfer B→A locks B, then each waits for the other's wallet. MySQL detects the cycle at once and rolls back one transaction (error 1213, `CannotAcquireLockException`); the other one continues. Prevent it by locking in a fixed order (lower id first) in every code path; keep transactions short; retry the victim as a safety net.
11. Two concurrent requests both pass the check, because neither sees the other's uncommitted insert. The unique constraint is checked by the database at the insert itself, so the second insert fails. The code must catch the `DataIntegrityViolationException`, throw a business error (`COUPON_ALREADY_ISSUED`), and let the transaction roll back.
12. Its transaction failed and is rolled back (and Hibernate's session shouldn't be used after the exception). Even before that, its snapshot under `REPEATABLE READ` was taken before the winner committed, so its plain reads couldn't see the winner's payment. A new transaction takes a new snapshot.
</details>

## Further reading

- Spring Framework reference, Resilience Features (`@Retryable`, `@ConcurrencyLimit`): https://docs.spring.io/spring-framework/reference/core/resilience.html
- Spring Data JPA reference, Locking: https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html
- Spring Framework reference, consistent exception hierarchy (`DataAccessException`): https://docs.spring.io/spring-framework/reference/data-access/dao.html
- MySQL reference manual: InnoDB locking (https://dev.mysql.com/doc/refman/9.7/en/innodb-locking.html), Locks set by different SQL statements, including foreign keys and duplicate keys (https://dev.mysql.com/doc/refman/9.7/en/innodb-locks-set.html), Deadlocks in InnoDB (https://dev.mysql.com/doc/refman/9.7/en/innodb-deadlocks.html)
- IETF draft, The Idempotency-Key HTTP Header Field: https://datatracker.ietf.org/doc/draft-ietf-httpapi-idempotency-key-header/
- Martin Kleppmann, *Designing Data-Intensive Applications*, chapter 7 "Transactions": lost updates, write skew and the ways to prevent them.
