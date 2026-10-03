# Transactions

A transaction makes several database changes succeed or fail together. The database gives you **ACID** inside a transaction:

- **Atomicity:** all changes happen, or none do.
- **Consistency:** constraints (unique keys, foreign keys) hold before and after.
- **Isolation:** concurrent transactions don't see each other's unfinished work, to the degree the isolation level promises.
- **Durability:** committed data survives a crash.

Your job in Spring code is to put the transaction boundaries in the right place and to handle what ACID doesn't cover.

## Where `@Transactional` goes

- **On service methods: one use case, one transaction.** "Place an order" creates the order, decreases stock and charges the wallet. If any step fails, none of them happened.
- **`@Transactional(readOnly = true)` for queries.** Hibernate skips dirty checking, and the database and driver may optimize. A common pattern: `readOnly = true` on the class, plain `@Transactional` on the methods that write.
- **Not on controllers**, and not on repositories you call from services (Spring Data repositories are already transactional on their own, which matters only when called outside a service transaction).

## How it works, and when it silently doesn't

Spring wraps a `@Transactional` bean in a **proxy**: callers talk to the proxy, which starts the transaction, calls your method, then commits or rolls back. So:

- **Calling a `@Transactional` method from inside the same class bypasses the proxy:** no new transaction starts (*self-invocation*). Move the method to another bean.
- **`private` methods are never proxied**, so `@Transactional` on them does nothing. Spring also can't intercept `final` methods.
- **The annotation only works on Spring beans**, never on objects you create with `new`.

## Rollback rules

By default, a transaction **rolls back for unchecked exceptions** (`RuntimeException`, `Error`) **and commits for checked exceptions**. A method that throws a checked `PaymentDeclinedException` therefore commits everything it did before the throw.

- Make business exceptions unchecked (as `BusinessException` is), or
- declare it explicitly: `@Transactional(rollbackFor = PaymentDeclinedException.class)`.
- **Catching an exception doesn't undo a rollback decision.** If an inner `@Transactional` method throws and marks the shared transaction rollback-only, catching the exception outside still ends in an `UnexpectedRollbackException` at commit.

## Propagation

| Propagation | Meaning | Use it for |
|---|---|---|
| `REQUIRED` (default) | join the caller's transaction, or start one | almost everything |
| `REQUIRES_NEW` | suspend the caller's transaction and run in a new, independent one | records that must survive the caller's rollback, such as an audit log of a failed attempt |
| `NESTED` | a savepoint inside the caller's transaction | partial rollback of one step |

`REQUIRES_NEW` holds **two** database connections at once (the suspended one and the new one). Under load it can exhaust the connection pool, and it can deadlock against its own caller if both touch the same rows.

## Isolation in MySQL

MySQL InnoDB's default is **`REPEATABLE READ`**: a transaction reads from a consistent snapshot taken at its first read.

| Anomaly | What happens | Prevented by |
|---|---|---|
| Dirty read | you see another transaction's uncommitted change | `READ COMMITTED` and above |
| Non-repeatable read | reading the same row twice gives different values | `REPEATABLE READ` and above |
| Phantom read | running the same query twice returns new rows | `SERIALIZABLE`; in MySQL, also locking reads with next-key locks under `REPEATABLE READ` |
| **Lost update** | two transactions read the same value, both write; one change disappears | **not** prevented by the default level: use an atomic `UPDATE … SET quantity = quantity - 1`, optimistic locking (`@Version`) or a locking read (`SELECT … FOR UPDATE`) |

Snapshot reads don't lock, but `SELECT … FOR UPDATE` (`@Lock(PESSIMISTIC_WRITE)` in Spring Data) reads the *latest* committed data and locks the rows until commit. S07 and S08 show each case with real MySQL sessions.

## Keep transactions short

A transaction holds a database connection, and its locks, until it ends.

- **No remote calls inside a transaction:** no HTTP calls, no email sending, no message publishing. If the remote call takes 10 seconds, the connection and its locks are held for 10 seconds. If the remote call succeeds and the transaction then rolls back, you can't undo the call.
- **Do side effects after commit:** `@TransactionalEventListener(phase = AFTER_COMMIT)`, or the transactional outbox pattern for reliable delivery (S09).
- A small connection pool (HikariCP's default is 10) fills up fast with slow transactions. Every further request then waits for a connection, and the service looks "down".

## Programmatic transactions

When a method needs a transaction around only part of its work, use `TransactionTemplate`:

```java
Receipt receipt = transactionTemplate.execute(status -> {
    // only this block runs in a transaction
    return paymentRepository.save(…);
});
mailer.sendReceipt(receipt); // outside the transaction
```

## Testing transactional code

A `@Transactional` test method rolls back at the end, which keeps the database clean. But it can hide bugs:

- **Nothing is flushed unless you ask.** Constraint violations may never reach the database. Call `entityManager.flush()` in tests that check constraints.
- **Lazy loading always works inside the test's transaction**, even where production code would fail. Test lazy-loading behavior without `@Transactional`.
- **After-commit listeners never fire**, because there's no commit. Test them with real commits, and clean up afterwards.
