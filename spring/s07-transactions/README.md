# S07 · Transactions and ACID

⏱ 6 sessions · weeks 10–11 · 6 exercises

Placing an order in the bookstore changes four tables: the stock goes down, an order is saved, a payment is recorded and the wallet pays. If the application crashes or a rule fails halfway, a customer can end up with a book that nobody paid for, or with a payment for an order that doesn't exist. A *transaction* makes such a group of changes succeed or fail as a whole. In Spring, you get one by writing `@Transactional`, and that annotation hides a lot: a proxy, a transaction manager, rollback rules that surprise most developers, propagation, and MySQL's own isolation rules. This module opens each of these up, so that you know what your transactions really do, how long they hold a database connection, and how to test them with real commits.

Each exercise names the lesson sections to read first.

## Lesson

### 1. ACID: what a transaction promises

A *transaction* is a group of database statements that the database treats as one unit. In SQL:

```sql
START TRANSACTION;
UPDATE inventories SET quantity = quantity - 2 WHERE book_id = 1;
INSERT INTO orders (member_id, status, total_amount, ...) VALUES (1, 'PAID', 72000, ...);
UPDATE wallets SET balance = balance - 72000 WHERE member_id = 1;
COMMIT;   -- or ROLLBACK, which undoes every statement since START TRANSACTION
```

A database like MySQL (with its InnoDB storage engine) gives four promises, known as **ACID**:

| Letter | Promise | In the bookstore |
|---|---|---|
| **A**tomicity | All statements take effect, or none do. | An order is never saved without its payment. |
| **C**onsistency | The data moves from one valid state to another: constraints such as unique keys and foreign keys hold at every commit. | Two members can't register the same email. Rules the database doesn't know about, such as "stock is never negative", are your code's job. |
| **I**solation | Transactions that run at the same time don't see each other's unfinished work, to the degree that the *isolation level* promises (section 6). | A report doesn't count an order that is still being placed. |
| **D**urability | Once `COMMIT` returns, the data survives a crash. | A paid order stays paid after a power cut. |

JDBC, Java's database API, runs every statement in its own transaction by default (*auto-commit*). To group statements, code turns auto-commit off, runs the statements on the **same connection**, and calls `commit()` or `rollback()` on it. A transaction belongs to one connection, which matters for everything that follows.

### 2. How Spring runs a transaction

You don't write `commit()` yourself. You annotate a service method:

```java
@Transactional
public OrderResponse placeOrder(long memberId, CreateOrderRequest request) { ... }
```

Three parts work together:

- **A transaction manager** (`PlatformTransactionManager`) knows how to begin, commit and roll back. Spring Boot creates a `JpaTransactionManager` for JPA applications. When it begins a transaction, it takes a connection from the connection pool, turns auto-commit off, opens a JPA `EntityManager` (the persistence context, S06) and **binds both to the current thread**.
- **A proxy** (S03) wraps every bean that has `@Transactional` methods. Callers talk to the proxy, and the proxy talks to the transaction manager around your method.
- **Your code** calls repositories as usual. Spring Data repositories, `JdbcClient` and `JdbcTemplate` all look for a transaction bound to the current thread and use its connection. Nothing is passed around explicitly.

Roughly, the proxy does this:

```java
TransactionStatus tx = transactionManager.getTransaction(definition); // begin, or join a running one (section 5)
try {
    OrderResponse result = target.placeOrder(memberId, request);    // your method
    transactionManager.commit(tx);                                  // flush the persistence context, then COMMIT
    return result;
} catch (RuntimeException | Error e) {
    transactionManager.rollback(tx);                                // ROLLBACK
    throw e;
} catch (Exception checkedException) {
    transactionManager.commit(tx);                                  // yes, COMMIT: section 4 explains
    throw checkedException;
}
```

Consequences you meet in this module:

- **Entities you load are written at commit.** Inside the transaction, a loaded entity is *managed*: `inventory.decrease(2)` changes only the object, and Hibernate writes the change at commit (*dirty checking*, S06). New entities need `save()`. Without a transaction, a loaded entity is *detached* at once, and changing it changes nothing in the database.
- **Only calls through the proxy start a transaction.** A call from one method of a bean to another method of the same bean (`this.place(...)`) doesn't pass through the proxy, so its `@Transactional` is ignored (*self-invocation*, S03). The same goes for `private` and `final` methods, and for objects you create with `new`.
- **The transaction belongs to the thread.** Code that runs on another thread (an executor, `CompletableFuture.supplyAsync`, `@Async` in S09) is outside it, and gets a connection and a transaction of its own, if any.

### 3. `@Transactional`: where it goes and what it can say

Put `@Transactional` on **public service methods**: one use case, one transaction. Not on controllers (a request may call several use cases, and the HTTP work shouldn't hold a connection), and not on repositories you call from services.

| Attribute | Default | Meaning |
|---|---|---|
| `propagation` | `REQUIRED` | what to do when a transaction is already running (section 5) |
| `isolation` | `DEFAULT` (the database's, `REPEATABLE_READ` in MySQL) | how much the transaction sees of others (section 6) |
| `readOnly` | `false` | a hint that the method only reads: Hibernate skips dirty checking and never flushes, and the MySQL driver marks the transaction `READ ONLY`, so a write fails |
| `timeout` | none | seconds before the transaction is rolled back (`spring.transaction.default-timeout` sets one for all) |
| `rollbackFor`, `noRollbackFor` | unchecked exceptions roll back | changes the rollback rules (section 4) |

A common pattern puts `@Transactional(readOnly = true)` on the class and plain `@Transactional` on the methods that write:

```java
@Service
@Transactional(readOnly = true)
public class OrderQueryService {

    public OrderResponse findOrder(long id) { ... }      // read-only, from the class

    @Transactional                                     // replaces the class-level annotation completely
    public void cancel(long id) { ... }
}
```

A method-level `@Transactional` **replaces** the class-level one; Spring doesn't merge their attributes. If the class says `rollbackFor = X.class` and the method says plain `@Transactional`, the method has the default rules.

### 4. Rollback rules

By default, Spring rolls back when the method ends with an **unchecked exception** (`RuntimeException` or `Error`), and **commits** when it ends with a **checked exception** (any other `Exception`). The rule comes from older Java EE conventions: a checked exception was seen as an expected business outcome, after which the work done so far should be kept.

```java
@Transactional
public PaymentResponse pay(long orderId, String key) throws PaymentDeclinedException {
    paymentRepository.save(Payment.approved(order, key, amount)); // written...
    order.markPaid();                                             // ...changed...
    throw new PaymentDeclinedException(orderId, "Too little money"); // ...and COMMITTED: checked exception
}
```

This surprises most developers who come from TypeScript, where any thrown error rolls back. Ways out:

- **Make business exceptions unchecked.** The bookstore's `BusinessException` extends `RuntimeException`, so it rolls back by default. This is the usual choice in Spring code.
- **Name the checked exception in the rules:** `@Transactional(rollbackFor = PaymentDeclinedException.class)`. Use it when you can't change the exception, for example because a library throws it. Some teams write `rollbackFor = Exception.class` everywhere. Since Spring Framework 6.2, `@EnableTransactionManagement(rollbackOn = RollbackOn.ALL_EXCEPTIONS)` sets that rule for the whole application.
- `noRollbackFor = SomeRuntimeException.class` keeps the work despite an unchecked exception. You rarely need it.

Two more rules:

- **An exception you catch inside the method doesn't roll anything back.** The proxy only sees what leaves the method.
- **`TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()`** marks the current transaction for rollback without throwing. It's rarely the clearest option.

### 5. Propagation: what happens when a transaction is already running

`placeOrder` is `@Transactional`, and so are the repository methods it calls. *Propagation* decides whether such an inner call joins the running transaction or starts its own.

| Propagation | When a transaction is running | When none is running |
|---|---|---|
| `REQUIRED` (default) | join it | start one |
| `REQUIRES_NEW` | suspend it, and start a new, independent one | start one |
| `SUPPORTS` | join it | run without a transaction |
| `MANDATORY` | join it | throw an exception |
| `NOT_SUPPORTED` | suspend it, and run without a transaction | run without a transaction |
| `NEVER` | throw an exception | run without a transaction |
| `NESTED` | run in a savepoint of the running transaction | start one |

**`REQUIRED` and the rollback-only trap.** With `REQUIRED`, the outer method and everything it calls share **one** database transaction. Only the outermost method can commit it. When an inner `@Transactional` method throws an unchecked exception, its proxy can't roll back yet, so it marks the shared transaction **rollback-only**. Catching the exception in the outer method doesn't remove that mark:

```java
@Transactional
public void placeOrderWithCoupon(...) {
    orderRepository.save(order);
    try {
        couponService.use(couponCode);       // @Transactional (REQUIRED), throws COUPON_EXPIRED
    } catch (BusinessException e) {
        log.info("Coupon not valid; placing the order without the discount");
    }
}   // commit → UnexpectedRollbackException: Transaction silently rolled back because it has been marked as
    //          rollback-only. The order is gone too.
```

Fixes: check the condition before calling (`couponService.isUsable(code)`), let the exception propagate, or give the inner work its own transaction.

**`REQUIRES_NEW`** suspends the caller's transaction and runs the method in a new one, which commits when the method returns. Use it for records that must survive the caller's rollback, such as an audit log of failed attempts. It has costs:

- **A second connection.** The suspended transaction keeps its connection while the new one takes another. If all 10 pooled connections belong to callers that wait for a `REQUIRES_NEW` call, nobody gets a connection, and every caller waits until the pool's timeout.
- **A separate persistence context.** Entities loaded by the caller are unknown inside the new transaction. Pass ids, not entities.
- **Locks against its own caller.** The suspended transaction still holds its row locks. If the new transaction needs one of those rows (an update, or even a foreign key check on an inserted row), it waits for a transaction that waits for it, until the lock wait timeout (50 seconds in MySQL).

`NESTED` uses a JDBC savepoint, so the inner work can roll back on its own while the outer transaction continues. `JpaTransactionManager` refuses it by default (`NestedTransactionNotSupportedException`): a savepoint would roll back the rows, but not the entities in the persistence context. With JPA, split the work instead.

### 6. Isolation in MySQL InnoDB

When transactions run at the same time, four things can go wrong. They are called *anomalies*:

| Anomaly | What happens |
|---|---|
| **Dirty read** | You read a change that another transaction hasn't committed, and may still roll back. |
| **Non-repeatable read** | You read the same row twice and get different values, because another transaction committed a change in between. |
| **Phantom read** | You run the same query twice and get rows that weren't there before, because another transaction inserted them. |
| **Lost update** | Two transactions read the same value, both compute a new value from it and both write. The second write silently replaces the first. |

The SQL standard defines four *isolation levels*; each one rules out more anomalies. Set a level per transaction with `@Transactional(isolation = Isolation.READ_COMMITTED)`, or for every connection with `spring.datasource.hikari.transaction-isolation`. MySQL's default is `REPEATABLE READ`.

InnoDB implements the levels with two kinds of reads:

- **Consistent reads (snapshot reads).** A plain `SELECT` doesn't lock anything and doesn't wait for locks. It reads a *snapshot*: the data as it was committed at one moment. InnoDB keeps older versions of changed rows (in its undo log) for this. This technique is called *MVCC* (multi-version concurrency control). Under `READ COMMITTED`, every statement takes a fresh snapshot. Under `REPEATABLE READ`, the transaction takes one snapshot at its **first read** (not at `START TRANSACTION`) and reads from it until it ends. A transaction always sees its own changes.
- **Locking reads (current reads).** `SELECT … FOR SHARE`, `SELECT … FOR UPDATE`, `UPDATE` and `DELETE` read the **latest committed** version, not the snapshot, and lock what they read until the transaction ends. `FOR SHARE` takes a *shared* lock (others may read-lock it too, nobody may change it); `FOR UPDATE` and writes take an *exclusive* lock. In one `REPEATABLE READ` transaction, a plain `SELECT` and a locking `SELECT` can therefore return different results.

InnoDB locks **index records**, and under `REPEATABLE READ` also the **gaps** between them:

- a *record lock* locks one index entry;
- a *gap lock* locks the space between two entries, so that nobody can insert there;
- a *next-key lock* is both: a record and the gap before it.

`SELECT … FROM orders WHERE member_id = 1 FOR UPDATE` locks every index entry with `member_id = 1` and the gaps around them, so no other transaction can insert a new order for member 1 until this one ends. That's how InnoDB prevents phantoms for locking reads. Under `READ COMMITTED`, InnoDB locks records only (except for foreign key and duplicate key checks), so locking reads can see phantoms, and fewer gap locks also mean fewer lock waits and deadlocks.

| Level | A plain `SELECT` reads | Dirty read | Non-repeatable read | Phantom | Lost update |
|---|---|---|---|---|---|
| `READ UNCOMMITTED` | the newest version, committed or not | possible | possible | possible | possible |
| `READ COMMITTED` | a fresh snapshot per statement | prevented | possible | possible | possible |
| `REPEATABLE READ` (MySQL's default) | the transaction's snapshot | prevented | prevented | prevented for plain `SELECT`s | **possible** |
| `SERIALIZABLE` | like `REPEATABLE READ`, but every plain `SELECT` becomes `FOR SHARE` (with auto-commit off) | prevented | prevented | prevented | prevented by locks: one writer waits, or a deadlock aborts one |

Two MySQL details matter in practice:

- **`REPEATABLE READ` doesn't stop lost updates.** If two transactions read a stock of 10 and both write 9, MySQL accepts both writes. (PostgreSQL's `REPEATABLE READ` aborts the second writer instead.) Use an atomic `UPDATE … SET quantity = quantity - 1`, which reads the latest value; optimistic locking with `@Version`; or a locking read. S08 covers all three.
- **Waiting for a lock has limits.** A statement waits up to `innodb_lock_wait_timeout` (50 seconds by default) for a lock, then fails with error 1205. When two transactions wait for each other (a *deadlock*), InnoDB notices at once and rolls one of them back with error 1213.

### 7. Keep transactions short

With `JpaTransactionManager`, a transaction takes a pooled connection when it begins and gives it back when it ends. Spring Boot's pool, HikariCP, has **10 connections** by default, and a request that finds none free waits up to **30 seconds**, then fails:

```
SQLTransientConnectionException: HikariPool-1 - Connection is not available, request timed out after 30000ms
```

Spring wraps this in `CannotCreateTransactionException: Could not open JPA EntityManager for transaction`.

Now put a remote call that takes 2 seconds inside a transaction. Each call holds a connection for 2 seconds, so 10 connections serve at most 5 such requests per second. At the 6th request per second, requests start to queue, and soon *every* request of the application waits, including the ones that need the database for a millisecond. The service looks down, although the database is idle. The transaction also holds its row locks for those 2 seconds, so other transactions wait for them.

Rules:

- **No remote calls inside a transaction**: no HTTP calls, no emails, no message publishing, no file uploads, no sleeping. If the remote call succeeds and the transaction then rolls back, you also can't undo the call.
- **Prepare before, write after.** Read and check what you need in a short transaction, make the slow call with no transaction, then write the result in a second short transaction. The second step must check its preconditions again, because the data may have changed during the call. If it fails, you may need a *compensating action* that undoes the remote call (cancel the booked pickup).
- **Side effects that must happen after a commit** (a confirmation email) belong in an after-commit event listener or the transactional outbox (S09).
- Watch the pool: `hikaricp.connections.pending` (S14's metrics) shows requests waiting for a connection.

### 8. Programmatic transactions: `TransactionTemplate`

`@Transactional` puts a transaction around a whole method. When you need one around part of a method, or several in a loop, use `TransactionTemplate`. Spring Boot creates one for you; inject it like any bean:

```java
Receipt receipt = transactionTemplate.execute(status -> {
    Order order = orderRepository.findById(orderId).orElseThrow();
    order.markPaid();
    return Receipt.of(order);            // the transaction commits when the callback returns
});
mailer.send(receipt);                    // outside the transaction

transactionTemplate.executeWithoutResult(status -> {
    inventory.decrease(2);
    if (somethingIsWrong) {
        status.setRollbackOnly();        // roll back without throwing
    }
});
```

- `execute` returns the callback's result; `executeWithoutResult` takes a callback that returns nothing.
- The transaction **commits when the callback returns** and **rolls back when it throws** a `RuntimeException` or an `Error`. The exception then leaves `execute`.
- The template's propagation is `REQUIRED` too. Called inside a `@Transactional` method, it **joins** the caller's transaction and commits nothing on its own. For an independent transaction, call it from code without a transaction, or create a template with `setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW)`.
- For different settings, create your own: `var readOnly = new TransactionTemplate(transactionManager); readOnly.setReadOnly(true);`.

Batch jobs use it for **chunks**: one transaction per 100 or 1,000 rows, so that a bad row only rolls back its own chunk, and a crash loses at most one chunk. (Spring Batch builds a whole framework on the same idea.)

### 9. Testing transactional code

The bookstore's `@IntegrationTest` runs each test in a transaction that is rolled back at the end. That keeps the database clean, but it changes what you can observe:

- **Your service joins the test's transaction.** When `placeOrder` throws, it only marks the test's transaction rollback-only. The test still sees every change made before the exception, because nothing has been rolled back yet. You can't test "all or nothing" this way.
- **Nothing commits,** so you can't test commit behavior, `REQUIRES_NEW`, what other sessions see, or after-commit listeners (S09).
- **Lazy loading always works,** and constraint violations only show at a flush (see [Transactions](../../docs/handbook/transactions.md#testing-transactional-code)).

Tests about transactions therefore opt out and commit for real, then put the seed data back:

```java
@IntegrationTest
@Transactional(propagation = Propagation.NOT_SUPPORTED) // no test transaction: the service commits for real
class PlaceOrderTest {

    @Autowired JdbcClient jdbc;
    @Autowired SeedData seedData;

    @AfterEach
    void restoreSeedData() {
        seedData.restore();                              // Flyway clean + migrate, about 0.25 s
    }
}
```

Check the results with plain SQL through `JdbcClient`, which reads what is committed, not what some persistence context remembers. Where a test needs a caller's transaction, it starts one with `TransactionTemplate`.

## TypeScript/NestJS → Spring

| TypeScript / NestJS | Spring | Watch out for |
|---|---|---|
| TypeORM `dataSource.transaction(async manager => { … })`, Prisma `$transaction(async tx => { … })` | `@Transactional` on a service method, or `transactionTemplate.execute(status -> { … })` | In JavaScript you pass `manager` or `tx` to every call. Spring binds the transaction to the thread, and repositories find it there. |
| Any thrown error rolls back | Only unchecked exceptions roll back | A checked exception **commits** (section 4). |
| `typeorm-transactional`'s `@Transactional()` (built on `AsyncLocalStorage`) | `@Transactional` (built on a thread-bound context) | Same idea. Work on another thread doesn't join the transaction. |
| `Propagation.REQUIRES_NEW` in such libraries | `Propagation.REQUIRES_NEW` | It holds a second connection while the first waits. |
| `queryRunner.startTransaction()` … `commitTransaction()` | `TransactionTemplate` | Prefer the template to calling the transaction manager yourself. |
| `isolationLevel: "READ COMMITTED"` | `@Transactional(isolation = Isolation.READ_COMMITTED)` | MySQL's default is `REPEATABLE READ`; PostgreSQL's is `READ COMMITTED`. |
| Connection pool `max: 10` (`mysql2`, `pg`) | HikariCP `spring.datasource.hikari.maximum-pool-size` (default 10) | A transaction holds its connection until it ends, slow calls included. |

## In real projects

- **One use case, one transaction, on the service method.** `readOnly = true` on query services or methods; never `@Transactional` on controllers. See [Transactions](../../docs/handbook/transactions.md) and [Spring layering](../../docs/handbook/spring-layering.md).
- **Business exceptions are unchecked** (`BusinessException`). Where a checked exception must roll back, say so with `rollbackFor`. Some teams add `rollbackFor = Exception.class` to every `@Transactional`; know which convention your team follows.
- **Let entities enforce the rules, and let the transaction make them atomic.** `inventory.decrease(…)` checks the stock; `@Transactional` makes sure a later failure undoes the decrease. See [JPA entities](../../docs/handbook/jpa-entities.md).
- **`REQUIRES_NEW` only where a record must survive the caller's rollback** (audit logs, failure counters), with ids as parameters, and on tables that the caller doesn't lock.
- **Keep MySQL's default isolation unless you have a reason.** Many large MySQL users run `READ COMMITTED` to reduce gap locks and deadlocks. Either way, protect read-modify-write code against lost updates (S08).
- **No remote calls inside transactions.** Watch the connection pool and the transaction length; set `spring.transaction.default-timeout` as a safety net. See [Logging and observability](../../docs/handbook/logging-and-observability.md) for metrics.
- **Batch work commits in chunks**, with `TransactionTemplate` or Spring Batch, and reports what failed.
- **Test transactional behavior with real commits** against the real database (MySQL in Testcontainers, never H2), and clean up afterwards. See [Testing Spring applications](../../docs/handbook/spring-testing.md).

## Exercises

### S07-01 · Atomic operations with @Transactional

**Build** · 30 min · [`OrderService.java`](src/main/java/com/example/bookstore/order/OrderService.java)

**Before you start:** lesson sections 1, 2, 3 and 9.

**Ticket BOOK-311 · One-click checkout.** Members want to buy with one click: place the order and pay for it from the wallet in one step. Implement `OrderService.placeOrder`.

Acceptance criteria:

- The copies of every line leave stock, the order is saved as `PAID` with its lines, an `APPROVED` payment of the total is saved with the request's idempotency key, and, last, the wallet pays the total (a `PAYMENT` wallet transaction).
- **All or nothing.** If any step fails (an unknown member or book, too few copies, too little money), nothing changes: stock, orders, order lines, payments, wallet balance and wallet transactions are as before, and the exception reaches the caller unchanged.
- An order without lines is rejected with `IllegalArgumentException`, and changes nothing.

**You'll practice:** `@Transactional` on a service method, managed versus new entities, letting entity methods enforce the rules, and tests that check "nothing changed" after a real rollback.

**Rules:** in the Javadoc of `placeOrder`. `CreateOrderRequest` and `OrderResponse` are given (`OrderResponse.from(order)` builds the response). The entities do the checks: `inventory.decrease`, `order.addLine`, `order.markPaid`, `Payment.approved`, `wallet.withdraw`.

<details>
<summary>Hint 1</summary>

Load the member, then go through the lines: for each one, load the book and its inventory (`inventoryRepository.findById(bookId)`), decrease the stock and add the line to the order. After the loop, mark the order paid and save it, save the payment, and withdraw from the wallet. `wallet.withdraw` returns a `WalletTransaction`; save it.
</details>

<details>
<summary>Hint 2</summary>

Which entities need `save()`? The order, the payment and the wallet transaction are new. The inventories and the wallet were loaded in the transaction, so Hibernate writes their changes at commit.
</details>

<details>
<summary>Hint 3</summary>

When all tests pass, remove `@Transactional` for a moment and run the tests again. Which tests fail, and why do the stock and the wallet stay unchanged while the order is saved?
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Why do these tests run without a test transaction? If they ran in the usual rolled-back test transaction, what would a test see after `placeOrder` failed in its last step?

### S07-02 · Rollback rules

**Fix** · 30 min · [`PaymentService.java`](src/main/java/com/example/bookstore/payment/PaymentService.java)

**Before you start:** lesson sections 3 and 4.

**Bug BOOK-327 · Declined payments mark orders as paid.** From support: "Minji tried to pay for an order of 108,000 won with 100,000 won in her wallet. The app told her that the payment was declined. But the order now shows as `PAID`, there is an `APPROVED` payment for it, and her wallet still holds 100,000 won. The warehouse is about to ship a book nobody paid for."

Fix `PaymentService.pay` so that a declined payment leaves nothing behind. Keep the order of the steps: the payment is saved first on purpose (its Javadoc says why). Then explain to yourself both ways to fix it, and when you would choose each.

**You'll practice:** Spring's default rollback rules, checked versus unchecked exceptions, `rollbackFor`, how method-level and class-level annotations combine.

**Rules:** in the Javadoc of `pay`. The tests in the "Basic" group show the symptoms.

<details>
<summary>Hint 1</summary>

Is `PaymentDeclinedException` a checked or an unchecked exception? What does the transactional proxy do when a method ends with each kind?
</details>

<details>
<summary>Hint 2</summary>

Lesson section 4 lists two fixes. One changes an annotation; the other changes the exception's superclass, and then the method's signature.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Which fix did you choose, and in which situation would the other one be the only option? Why did "rejects an order that is already PAID" pass even before your fix?

### S07-03 · Propagation

**Build** · 30 min · [`PaymentAttemptLog.java`](src/main/java/com/example/bookstore/payment/PaymentAttemptLog.java)

**Before you start:** lesson section 5.

**Ticket BOOK-334 · Keep a record of failed payments.** Compliance needs every failed payment attempt on record: which order, which member, how much and why. A declined payment is rolled back completely (S07-02), but its record must stay. `PaymentService.pay` already calls `PaymentAttemptLog.recordFailure` when it declines a payment; the table (`V3__create_payment_attempts.sql`), the entity `PaymentAttempt` and its repository are given. Implement `recordFailure`.

Acceptance criteria, from its Javadoc:

- The record survives when the caller's transaction rolls back.
- The record is committed before `recordFailure` returns, so other database sessions see it at once.
- If writing the record fails, the caller can catch the exception and still commit its own work.

The test group "The rollback-only trap" contains a test that passes from the start: it shows what goes wrong with `REQUIRED`, using Spring Data's `saveAndFlush`. Read it, run it, and make sure you can explain where `UnexpectedRollbackException` comes from.

**You'll practice:** `REQUIRED` versus `REQUIRES_NEW`, rollback-only transactions, passing ids instead of entities across transactions.

<details>
<summary>Hint 1</summary>

The method body is one line: save `PaymentAttempt.failed(…)`. All the work is in choosing the right transaction for it.
</details>

<details>
<summary>Hint 2</summary>

Read the comment at the top of `V3__create_payment_attempts.sql`. Why would a foreign key from `payment_attempts` to `orders` make a declined payment hang for 50 seconds?
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: How many database connections does a declined payment hold at the moment the attempt is saved? What happens when all 10 pooled connections belong to payments that wait for that second connection?

### S07-04 · Predict: isolation levels in MySQL

**Predict** · 30 min · [`IsolationPuzzles.java`](src/test/java/com/example/bookstore/isolation/IsolationPuzzles.java) → answers in [`IsolationAnswers.java`](src/test/java/com/example/bookstore/isolation/IsolationAnswers.java)

**Before you start:** lesson section 6.

Twelve puzzles, each with two real MySQL sessions, A and B, that take turns, step by step. You predict what B reads (or whether a statement has to wait for a lock) under different isolation levels: dirty reads, non-repeatable reads, phantoms and lost updates, with InnoDB's snapshots, locking reads and gap locks. The puzzles live in the test sources because they talk to the database directly, without the application.

**How:** read [`DatabaseSession`](src/test/java/com/example/bookstore/isolation/DatabaseSession.java) first: it explains how two sessions run on one thread without timing problems. Then read each puzzle in `IsolationPuzzles` without running it, write your prediction in the matching constant in `IsolationAnswers`, and run `IsolationPuzzlesTest`. Two puzzles wait one second for a lock; that is part of the experiment.

<details>
<summary>Explanations (open after you've run the tests)</summary>

- **`READ UNCOMMITTED` reads 3, `READ COMMITTED` reads 10.** `READ UNCOMMITTED` reads the newest version of a row, even one that isn't committed: a dirty read of a value that A then rolls back, so it never really existed. `READ COMMITTED` reads the last committed version from InnoDB's undo log, without waiting for A's lock.
- **`READ COMMITTED`: the second read gives 7.** Every statement takes a fresh snapshot, so B sees A's commit: a non-repeatable read.
- **`REPEATABLE READ`: the second read gives 10.** B keeps the snapshot of its first read until its transaction ends.
- **`REPEATABLE READ` after `START TRANSACTION`: 7.** The snapshot is taken at the first read, not at `START TRANSACTION`, and A committed before B read anything. (`START TRANSACTION WITH CONSISTENT SNAPSHOT` would take it at once.)
- **Counting Minji's orders: 15 under `READ COMMITTED`, 14 under `REPEATABLE READ`.** The new order is a phantom row. B's `REPEATABLE READ` snapshot doesn't show it to plain `SELECT`s.
- **The same count with `FOR SHARE`: 15.** Locking reads read the latest committed rows, not the snapshot. In one transaction, the plain and the locking count disagree.
- **Inserting an order for Minji after B's `FOR UPDATE`: A waits for a lock.** B's next-key locks cover Minji's index entries and the gaps next to them, so A can't insert into that range until B's transaction ends. That is how InnoDB prevents phantoms for locking reads.
- **Two read-modify-writes: 9, not 8.** Both sessions computed 10 − 1 from their own reads. B's `UPDATE` wrote 9 over A's 9, and one sale was lost. MySQL's `REPEATABLE READ` doesn't detect this.
- **Two atomic `quantity = quantity - 1` updates: B reads 8.** An `UPDATE` is a locking read of the latest committed row (9, after A's commit), not of B's snapshot (10). B then sees its own change.
- **`SERIALIZABLE`: A waits for a lock.** With auto-commit off, `SERIALIZABLE` turns B's plain `SELECT` into `SELECT … FOR SHARE`, and A's `UPDATE` needs an exclusive lock on the same row.
</details>

### S07-05 · Keep transactions short

**Fix** · 30 min · [`ShippingService.java`](src/main/java/com/example/bookstore/shipping/ShippingService.java)

**Before you start:** lesson sections 7 and 8.

**Bug BOOK-352 · The shop stops answering at 5 p.m.** From operations: "Every weekday at 17:00, when the warehouse ships the day's orders, the whole shop stops answering for a few minutes. Even `GET /api/books` times out. The logs are full of `HikariPool-1 - Connection is not available, request timed out after 30000ms`." The courier's API needs about 2 seconds per call.

Fix `ShippingService.ship` so that the courier call runs outside any database transaction, and the method holds a pooled connection only for the short database work before and after the call. The behavior must stay as the Javadoc describes it: a non-`PAID` order is rejected before the courier is called, and a courier failure leaves the order `PAID`.

The tests reproduce the symptom with a pool of 2 connections and 4 shipments at the same time. The courier is a fake ([`FakeCourierClient`](src/test/java/com/example/bookstore/support/FakeCourierClient.java)) that can hold every call until the test releases it, so the test needs no sleeping and no lucky timing.

**You'll practice:** recognizing a transaction that is too long, splitting work into short transactions around a remote call, `TransactionTemplate`, re-checking preconditions after a remote call.

<details>
<summary>Hint 1</summary>

When does the transaction of `ship` take its connection, and when does it give it back? Where in between is the courier call?
</details>

<details>
<summary>Hint 2</summary>

`TransactionTemplate` (lesson section 8) puts a transaction around part of a method. You need two short ones: one to check the order and build the `PickupRequest` (it reads lazy associations, so it needs the transaction), and one to mark the order shipped. `OrderStatus.canChangeTo` tells you whether an order may ship.
</details>

<details>
<summary>Hint 3</summary>

Splitting `ship` into two `@Transactional` methods of the same class doesn't work. Why? (Lesson section 2.)
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: What happens if someone cancels the order while the courier call runs? Why would `CompletableFuture.supplyAsync(() -> courier.requestPickup(…)).join()` inside the transaction not fix the bug?

### S07-06 · Programmatic transactions

**Build** · 30 min · [`BookImportService.java`](src/main/java/com/example/bookstore/book/BookImportService.java)

**Before you start:** lesson section 8.

**Ticket BOOK-360 · The nightly catalog import fails completely.** Publishers send a file of new books every night. Last night, one row with an unknown category rolled back the whole import of 2,000 books, and the shop showed no new books the next day. Implement `BookImportService.importBooks`: import the rows in chunks, one transaction per chunk, so that a bad row costs only its own chunk, and return a report of what was imported and which chunks failed.

Acceptance criteria are in the Javadoc of `importBooks`. `importRow` is given: it saves one book and its stock, and throws for a bad row. The report records, `BookImportReport` and `BookImportReport.FailedChunk`, are given too.

**You'll practice:** `TransactionTemplate.executeWithoutResult`, a transaction per chunk, catching a failure outside the transaction that it rolled back, chunking a list with `subList`.

<details>
<summary>Hint 1</summary>

Loop over the chunk start positions: `0`, `chunkSize`, `2 * chunkSize`, … For each chunk, `rows.subList(start, end)`, where `end` is never beyond `rows.size()`. Row numbers in the report start at 1.
</details>

<details>
<summary>Hint 2</summary>

Inside the template's callback, call `importRow` for every row of the chunk. Where do you catch the exception: inside the callback, or around `executeWithoutResult`? What happens to the chunk's good rows in each case?
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: What would happen if a colleague added `@Transactional` to `importBooks` "to be safe"? Why does `chunkSize = Integer.MAX_VALUE` not overflow in your loop?

## Review questions

1. A colleague puts `@Transactional` on a `private` helper method that saves two entities, and calls it from a public method of the same service. One save fails, and the other is still in the database. Why?
2. What does Spring do when a `@Transactional` method ends with a checked exception? Name two ways to make it roll back.
3. A service class has `@Transactional(rollbackFor = Exception.class)`, and one of its methods has a plain `@Transactional`. Which rollback rules does that method use?
4. A method catches a `DataIntegrityViolationException` from a repository call, logs it and carries on. At the end, the caller gets `UnexpectedRollbackException`. Explain each step.
5. When does `REQUIRES_NEW` need a second connection, and how can it make a whole application hang?
6. Under MySQL's default isolation level, a service reads the stock (10), checks that 2 copies are left, and writes `10 - 2`. Two such orders run at the same time. What can happen, and why doesn't `REPEATABLE READ` prevent it?
7. In one `REPEATABLE READ` transaction, `SELECT COUNT(*) FROM orders` returns 200, and `SELECT COUNT(*) FROM orders FOR SHARE` returns 201. How is that possible?
8. Your pool has 10 connections, and a transaction contains an HTTP call that takes 2 seconds. How many such requests per second can the application serve at most, and what happens to the other endpoints when more arrive?
9. Why can a test annotated with `@Transactional` pass while the same code fails in production? Give two examples.
10. You call `transactionTemplate.execute(…)` in a loop inside a `@Transactional` method, hoping for one transaction per iteration. Why don't you get it, and what are two ways to fix it?

<details>
<summary>Answers</summary>

1. The transactional proxy only intercepts calls that come through it, and it never intercepts `private` methods. The call from the public method to the helper is a plain Java call on `this`, so no transaction starts, and each repository call commits on its own.
2. It commits (the default rules roll back only for `RuntimeException` and `Error`). Make the exception unchecked, or declare it with `@Transactional(rollbackFor = TheException.class)`. Since Spring 6.2, `@EnableTransactionManagement(rollbackOn = RollbackOn.ALL_EXCEPTIONS)` changes the rule for the whole application.
3. The method's own annotation, with the default rules: a method-level `@Transactional` replaces the class-level one completely, without merging attributes. A checked exception from that method commits.
4. The repository method runs with `REQUIRED`, so it joins the caller's transaction. It fails with an unchecked exception, and because it doesn't own the transaction, its proxy marks the shared transaction rollback-only before rethrowing. The caller's catch block can't remove the mark. When the outermost method returns, the transaction manager sees the mark, rolls back instead of committing, and throws `UnexpectedRollbackException` so that the caller learns its work was lost.
5. Whenever it is called while a transaction is running: the caller's transaction is suspended but keeps its connection, and the new transaction takes another one. If every pooled connection belongs to a caller waiting for its `REQUIRES_NEW` call, no new transaction can get a connection, and all of them wait until the pool's timeout. The new transaction can also wait for row locks held by its own suspended caller.
6. A lost update: both transactions read 10 and both write 8, although 4 copies were sold. `REPEATABLE READ` gives each transaction a stable snapshot to read, but MySQL doesn't check whether a row changed since the snapshot when you write it. Use `UPDATE … SET quantity = quantity - 2 WHERE quantity >= 2`, `@Version`, or a `SELECT … FOR UPDATE` (S08).
7. The plain `SELECT` reads the transaction's snapshot. The locking `SELECT … FOR SHARE` reads the latest committed rows, which include an order that another transaction inserted and committed after the snapshot was taken.
8. At most 5 per second: each request holds one of the 10 connections for 2 seconds. Beyond that, requests queue for connections, and every other endpoint that needs the database waits in the same queue, up to the 30-second timeout. The application looks down while the database is idle.
9. The test's transaction stays open until the test ends and is never committed. Examples: lazy loading works in the test but throws `LazyInitializationException` in production; a failed service call only marks the test's transaction rollback-only, and its partial changes stay visible to the test; after-commit listeners never run; a constraint violation that would fail at commit never shows up without a flush.
10. The template's default propagation is `REQUIRED`, so every iteration joins the method's transaction, and nothing commits until the method returns; one failing iteration marks the whole transaction rollback-only. Remove `@Transactional` from the method, or configure the template with `PROPAGATION_REQUIRES_NEW` (and accept that it suspends the outer transaction and uses a second connection).
</details>

## Further reading

- Spring Framework reference: [Transaction management](https://docs.spring.io/spring-framework/reference/data-access/transaction.html) · [Declarative transactions and the proxy](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-decl-explained.html) · [Rolling back](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/rolling-back.html) · [`@Transactional` settings](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html) · [Propagation](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-propagation.html) · [Programmatic transactions](https://docs.spring.io/spring-framework/reference/data-access/transaction/programmatic.html)
- MySQL reference manual: [Transaction isolation levels](https://dev.mysql.com/doc/refman/9.7/en/innodb-transaction-isolation-levels.html) · [Consistent nonlocking reads](https://dev.mysql.com/doc/refman/9.7/en/innodb-consistent-read.html) · [Locking reads](https://dev.mysql.com/doc/refman/9.7/en/innodb-locking-reads.html) · [InnoDB locking: record, gap and next-key locks](https://dev.mysql.com/doc/refman/9.7/en/innodb-locking.html) · [Phantom rows](https://dev.mysql.com/doc/refman/9.7/en/innodb-next-key-locking.html)
- HikariCP: [About pool sizing](https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing), on why a small pool is usually right.
- Martin Kleppmann, *Designing Data-Intensive Applications*, chapter 7 "Transactions" (book 13 in the bookstore): the clearest explanation of isolation levels and their anomalies.
