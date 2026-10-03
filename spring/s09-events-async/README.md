# S09 · Events, async and scheduling

⏱ 4 sessions · weeks 11–12 · 4 exercises

Much of what a backend does shouldn't happen inside the request that triggers it, or at that moment: a confirmation email after a payment, a sales report that takes a minute to build, a nightly cleanup, a message that tells another service that an order has shipped. Spring gives you a tool for each: application events to decouple "what happened" from "what follows", `@TransactionalEventListener` to act only after a commit, `@Async` methods on thread pools you control, and `@Scheduled` jobs. This module also builds the *transactional outbox*, the standard pattern for sending messages to other systems without losing any, and without sending any for changes that were rolled back. You need S07 (transactions) for all of it.

Each exercise names the lesson sections to read first.

## Lesson

### 1. Application events

A service often triggers follow-up work that isn't its business: when an order is paid, send an email, add loyalty points, update a dashboard. If `PaymentService` called each of these directly, it would depend on all of them, and every new follow-up would change it. With *events*, the service only announces what happened, and *listeners* decide what to do about it:

```java
public record OrderPaid(long orderId, long memberId, String email, long amount, Instant paidAt) {
}

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public PaymentResponse pay(long orderId, String idempotencyKey) {
        // ... the payment itself ...
        eventPublisher.publishEvent(new OrderPaid(orderId, memberId, email, amount, clock.instant()));
        return response;
    }
}

@Component
class LoyaltyListener {

    @EventListener
    void onOrderPaid(OrderPaid event) {   // the parameter's type selects the events
        ...
    }
}
```

- Any object can be an event; a record is the usual choice. `ApplicationEventPublisher` is a bean that you inject like any other. (The `ApplicationContext` is one.)
- A plain `@EventListener` runs **synchronously**: on the publisher's thread, inside the publisher's transaction, before `publishEvent` returns. An exception in the listener reaches the publisher, and rolls its transaction back.
- **Events carry values, never entities.** A listener may run when the publisher's persistence context is closed, and then lazy associations can't load. Put everything listeners need into the event, as plain values.
- Spring Boot publishes its own events too, such as `ApplicationReadyEvent` when the application has started.

### 2. Transaction-bound events: `@TransactionalEventListener`

A synchronous listener that sends an email has two problems. It sends the email **before** the commit, so a payment that fails afterwards (a constraint violation at commit, a later exception) has already told the customer "paid". And a slow or failing mail server makes the payment slow or fails it.

`@TransactionalEventListener` holds the event until the publishing transaction ends, and then calls the listener in a chosen *phase*:

| Phase | The listener runs |
|---|---|
| `AFTER_COMMIT` (default) | after a successful commit |
| `AFTER_ROLLBACK` | after a rollback |
| `AFTER_COMPLETION` | after the transaction ended either way |
| `BEFORE_COMMIT` | right before the commit, still inside the transaction |

```java
@TransactionalEventListener   // phase = AFTER_COMMIT
void sendConfirmation(OrderPaid event) {
    mailer.send(...);
}
```

Things to know:

- **No transaction, no event.** If `publishEvent` is called outside a transaction, a transactional listener is silently skipped, unless it says `fallbackExecution = true`.
- **Exceptions don't reach the publisher.** In `AFTER_COMMIT`, the data is already committed. Spring logs an exception from the listener and goes on, and the publisher's method returns normally.
- **Writes in an `AFTER_COMMIT` listener need their own transaction.** The finished transaction's resources are still attached to the thread, and new writes would join a transaction that never commits again. Annotate a writing listener with `@Transactional(propagation = Propagation.REQUIRES_NEW)`.
- **Best effort.** If the application stops between the commit and the listener, the listener never runs. That's acceptable for a courtesy email, and not acceptable for a message that another system must receive (section 6).
- Add `@Async` (section 3) to run the listener on another thread, so that the request doesn't wait for the email.

### 3. `@Async` methods and thread pools

`@Async` on a public method of a bean makes the proxy (S03) hand the call to an *executor* and return at once. The method then runs on a thread of that executor. Switch it on once, with `@EnableAsync` on a configuration class:

```java
@Configuration(proxyBeanMethods = false)
@EnableAsync
class AsyncConfiguration {

    @Bean
    ThreadPoolTaskExecutor reportExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("report-");
        return executor;
    }
}

@Service
class ExportService {

    @Async("reportExecutor")                          // the executor bean's name
    public CompletableFuture<Export> export(long id) {
        Export export = ...;                          // runs on report-1 or report-2
        return CompletableFuture.completedFuture(export);
    }
}
```

**Results and failures.** An `@Async` method returns `void` or a future:

- With `CompletableFuture<T>`, the caller gets a future at once. It completes with the value of the future your method returns, or fails with whatever your method throws, including argument checks at its first line: **every failure travels through the future.** The caller decides how to wait: `future.get(5, SECONDS)`, `future.join()`, or `thenApply(…)`, like `then` on a `Promise`.
- With `void`, nobody can see a failure. Spring passes it to an `AsyncUncaughtExceptionHandler`, which only logs it unless you configure your own (`AsyncConfigurer`).

**The same proxy rules as `@Transactional`** (S07 section 2): a call from the same class, or to a `private` method, runs synchronously on the caller's thread. On a method that has both annotations, `@Async` comes first, so the transaction starts on the executor's thread. The caller's transaction never extends into an async method.

**Executors.** Spring Boot creates one executor for you, `applicationTaskExecutor`: 8 core threads and an unbounded queue. `@Async` without a name uses it. Name a pool of your own for work that must not compete with everything else, or that needs limits. Two Boot details:

- Defining any `Executor` bean switches `applicationTaskExecutor` off, and plain `@Async` then picks a different executor. `spring.task.execution.mode: force` keeps Boot's executor next to yours.
- `ThreadPoolTaskExecutor` follows Java's `ThreadPoolExecutor`: it starts threads up to the core size, then **queues** tasks, and starts threads beyond the core size only when the queue is full. With an unbounded queue, the maximum size never matters. When the queue and the maximum are both full, a new task is rejected with `TaskRejectedException`. That's a feature: it tells the caller "too busy" at once.
- `setWaitForTasksToCompleteOnShutdown(true)` with `setAwaitTerminationSeconds(30)` lets running tasks finish when the application stops.

**Virtual threads.** With `spring.threads.virtual.enabled: true` (Java 21+), Spring Boot runs requests, `applicationTaskExecutor` and the scheduler on virtual threads (J08): one new, cheap thread per task, no pool. Pools you define yourself stay as they are. A limit is still needed somewhere, because the database connection pool doesn't grow with the threads.

### 4. Context on other threads: the MDC and `TaskDecorator`

The *MDC* (mapped diagnostic context) is a per-thread map whose values the logger adds to every log line, such as a request id. S14 fills it for every request; this module's `application.yml` shows it in every line, after the thread name: `[report-1] [req-42] Sales report for 2026-09: …`.

Because the MDC belongs to a thread, an `@Async` method or any executor task starts with an empty MDC, or worse, with the MDC of the task that the pool thread ran before. A `TaskDecorator` fixes both. The executor calls it on the **caller's** thread when a task is submitted, and runs what it returns on the pool thread:

```java
public Runnable decorate(Runnable task) {
    Map<String, String> callersContext = MDC.getCopyOfContextMap(); // caller's thread
    return () -> {                                                   // pool thread
        // set callersContext (or clear, if it is null), run the task, and clear in a finally block
    };
}
```

Other per-thread context needs the same care: Spring Security's `SecurityContext` (S12) and tracing data. Spring Boot can propagate tracing context with `spring.task.execution.propagate-context: true`.

### 5. Scheduled jobs

`@EnableScheduling` on a configuration class, and `@Scheduled` on a bean method without parameters, run work on a schedule:

```java
@Scheduled(fixedDelayString = "${bookstore.orders.pending-cleanup.interval}",     // "10m"
        initialDelayString = "${bookstore.orders.pending-cleanup.interval}")
public void run() {
    int cancelled = cleanupService.cancelExpiredOrders();
}
```

| Attribute | Meaning |
|---|---|
| `fixedDelay` | wait this long after a run **ends**, then start the next: runs never overlap |
| `fixedRate` | start a run every interval, measured from the **starts** |
| `cron` | Spring's cron has six fields, seconds first: `"0 0 3 * * *"` is every day at 03:00:00; set `zone = "Asia/Seoul"`, or it uses the server's time zone; `"-"` switches the job off |
| `initialDelay` | wait this long after startup before the first run |

- The `…String` variants take placeholders and durations such as `10m` or `PT10M`, so the schedule lives in the configuration.
- Spring Boot's scheduler has **one thread** by default (`spring.task.scheduling.pool.size`), so a slow job delays the others.
- An exception in a job is logged, and the next run happens as planned.
- **Every instance of the application runs every job.** With three instances, the cleanup runs three times. Make jobs safe to run in parallel (row locks, `SKIP LOCKED`, idempotent changes), switch them on for one instance only, or use a distributed lock such as ShedLock.

**Testable jobs.** Keep the `@Scheduled` method thin: it decides *when*, and a service method decides *what*. Tests call the service directly, with the test clock, and never wait for the scheduler. Make scheduling switchable, and switch it off in tests, so that no job changes the data a test is checking:

```java
@Configuration(proxyBeanMethods = false)
@ConditionalOnBooleanProperty(name = "bookstore.scheduling.enabled", matchIfMissing = true)
@EnableScheduling
class ScheduledJobsConfiguration {
}
```

The schedule itself can be tested in a small context of its own (`ApplicationContextRunner`, S01): Spring's `ScheduledTaskHolder` lists the registered tasks with their intervals.

### 6. The transactional outbox

When the order ships, the delivery-tracking service must hear about it through the message broker (Kafka, RabbitMQ). The obvious code publishes the message in `ship()`. That's a *dual write*: two systems, the database and the broker, and no transaction covers both.

- Publish inside the transaction, then the transaction rolls back: the message announces a shipment that never happened.
- Commit, then publish, and the broker is down or the application stops: the message is lost.

The **transactional outbox** turns the dual write into a single write:

1. `ship()` inserts the event into an `outbox_events` table, **in the same transaction** as the order change. Both commit, or neither does.
2. A **relay** polls the table: it reads the unpublished events, publishes each one, and marks it published.

```sql
SELECT * FROM outbox_events WHERE published_at IS NULL ORDER BY id LIMIT 100
FOR UPDATE SKIP LOCKED
```

`FOR UPDATE` locks the rows until the relay's transaction ends, so nobody else publishes them meanwhile. `SKIP LOCKED` (MySQL 8 and later) makes a second relay, on another instance of the application, skip the locked rows instead of waiting for them: the instances share the work, and each event goes out once per run.

What the outbox guarantees, and what it doesn't:

- **At least once.** If the relay publishes an event and the application stops before the commit that marks it published, the next run publishes it again. Consumers must therefore be **idempotent**: they remember the event ids they have handled (for example in a table with a unique key, written in the same transaction as their own change) and skip repeats.
- **Retries for free.** An event whose publishing fails stays unpublished, and the next run tries it again. Count the attempts, and alert or park an event that keeps failing.
- **Order.** Events are published by id. If one fails and a later one of the same order succeeds, the consumer sees them out of order; relays that need strict order per aggregate stop at the first failure of that aggregate. Brokers keep the order of messages with the same key, which is why the message key is the order's id.
- **Housekeeping.** Delete published events after a few days, with another scheduled job.

Polling is the simple way to run a relay. Larger systems read the database's change log instead (change data capture, such as Debezium). Spring Modulith offers a ready-made "event publication registry" built on the same idea.

| | `@TransactionalEventListener(AFTER_COMMIT)` | Transactional outbox |
|---|---|---|
| Runs | in the same application, right after the commit | in a relay, a few seconds later |
| If the application stops at the wrong moment | the work is lost | the event is published later, maybe twice |
| Good for | emails, cache refreshes, anything you can lose | messages to other systems that must arrive |

## TypeScript/NestJS → Spring

| TypeScript / NestJS | Spring | Watch out for |
|---|---|---|
| `@nestjs/event-emitter`: `emit('order.paid', payload)`, `@OnEvent('order.paid')` | `publishEvent(new OrderPaid(…))`, `@EventListener` | Spring picks listeners by the event's type, not by a name. Listeners run synchronously unless you make them async. |
| An "after commit" hook (`typeorm-transactional`'s `runOnTransactionCommit`) | `@TransactionalEventListener` | A transactional listener is skipped when no transaction is running. |
| `async` function returning a `Promise` | `@Async` method returning `CompletableFuture` | Java runs it on another thread, with its own transaction and an empty thread context. |
| `Promise.all([...])` | `CompletableFuture.allOf(...)` | |
| `AsyncLocalStorage`, which follows `await` | MDC and other thread-locals with a `TaskDecorator` | Context doesn't move to a pool thread by itself. |
| `@nestjs/schedule`: `@Cron('0 */10 * * * *')`, `@Interval(600000)` | `@Scheduled(cron = "0 */10 * * * *")`, `@Scheduled(fixedDelayString = "10m")` | NestJS also accepts five cron fields; Spring needs six, seconds first. Every instance runs every job. |
| A BullMQ queue with workers | the outbox table with a relay, and a broker | The outbox makes the message part of the database transaction. |

## In real projects

- **Services publish events, listeners react.** Events are records with plain values. Keep listeners small; a listener that grows into a use case of its own becomes a service method. See [Spring layering](../../docs/handbook/spring-layering.md).
- **Side effects after the commit.** Emails, cache refreshes and notifications use `@TransactionalEventListener`; messages that other systems rely on use an outbox. Never call a remote system inside a transaction (S07). See [Transactions](../../docs/handbook/transactions.md).
- **Name and bound every thread pool.** A name for the logs and thread dumps, a bounded queue, a rejection you have thought about, and a graceful shutdown. Spring Boot's actuator (S14) reports executor metrics. See [Concurrency](../../docs/handbook/concurrency.md).
- **`@Async` methods return a `CompletableFuture`,** or handle their own errors; configure an `AsyncUncaughtExceptionHandler` for `void` methods. Callers wait with a timeout.
- **Carry the request id to other threads** with a `TaskDecorator`, so that a log search for one request finds all its lines. See [Logging and observability](../../docs/handbook/logging-and-observability.md).
- **Jobs are thin, idempotent and switchable.** A property turns scheduling on or off ([Configuration](../../docs/handbook/configuration.md)); jobs are safe to run on several instances; tests call the service with a fixed clock instead of waiting. See [Testing Spring applications](../../docs/handbook/spring-testing.md).
- **Outbox consumers are idempotent**, because delivery is at least once.

## Exercises

### S09-01 · After-commit events

**Build** · 30 min · [`OrderConfirmationListener.java`](src/main/java/com/example/bookstore/notification/OrderConfirmationListener.java) · [`PaymentService.java`](src/main/java/com/example/bookstore/payment/PaymentService.java)

**Before you start:** lesson sections 1 and 2.

**Ticket BOOK-402 · Confirm paid orders by email.** Members want an email when their order is paid. Product adds: "A similar feature at my last company sent 'payment received' emails for payments that failed a moment later. That must not happen here."

1. In `PaymentService.pay`, publish an `OrderPaid` event where the `TODO` comment is. The record `OrderPaid` is given; its Javadoc says why it carries the email address.
2. Implement `OrderConfirmationListener.onOrderPaid`, which sends the email through the `Mailer`.

Acceptance criteria, from the Javadoc of `onOrderPaid`: the email goes to the event's address, with the given subject and body; it is sent only after the payment's transaction commits, never for a payment that is rolled back, and never while the transaction is still running; and a failure to send never undoes the payment. The tests use [`FakeMailer`](src/test/java/com/example/bookstore/support/FakeMailer.java), which records the emails.

**You'll practice:** `ApplicationEventPublisher`, `@EventListener` versus `@TransactionalEventListener`, transaction phases, testing after-commit behavior with real commits.

<details>
<summary>Hint 1</summary>

`ApplicationEventPublisher` is a bean: add it as a `final` field of `PaymentService`, and Lombok's constructor injects it. Publish after the wallet step, with the time from the `Clock`.
</details>

<details>
<summary>Hint 2</summary>

A listener method needs an annotation, or Spring never calls it. Which one waits for the commit, and which phase do you need? Run the tests with a plain `@EventListener` first, and read which ones fail.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: What happens to the email if the application stops between the commit and the listener? Where would a listener that also *writes* to the database need its own transaction, and why?

### S09-02 · Async methods and thread pools

**Build** · 30 min · [`AsyncConfiguration.java`](src/main/java/com/example/bookstore/common/AsyncConfiguration.java) · [`MdcTaskDecorator.java`](src/main/java/com/example/bookstore/common/MdcTaskDecorator.java) · [`SalesReportService.java`](src/main/java/com/example/bookstore/report/SalesReportService.java)

**Before you start:** lesson sections 3 and 4.

**Ticket BOOK-415 · Monthly sales reports in the background.** Finance wants a sales report per month. A report scans a month of orders, and finance starts several at once on the first day of each month. Run report generation in the background, on a pool of its own with 2 threads, so that it can't slow down anything else. The caller gets a future, and every failure must reach it through that future. Operations adds: "When a report misbehaves, we search the logs by request id. Report log lines must carry it."

1. `AsyncConfiguration`: switch on `@Async`, and define the `reportExecutor` bean, as its Javadoc lists.
2. `MdcTaskDecorator`: copy the caller's MDC to the report thread, and clear it afterwards.
3. `SalesReportService.generate`: run on the report executor, and report as its Javadoc describes. The query is given: `OrderRepository.sumOrders`.

**You'll practice:** `@EnableAsync`, a named `ThreadPoolTaskExecutor`, `@Async` with `CompletableFuture`, failures through futures, `TaskDecorator` and the MDC, waiting for futures with a timeout in tests.

<details>
<summary>Hint 1</summary>

The stub of `AsyncConfiguration` is an empty class, so that the application starts before you begin. Add `@EnableAsync` and one `@Bean` method named `reportExecutor` that returns a configured `ThreadPoolTaskExecutor` (section 3 shows one). Spring calls its `initialize()` for you.
</details>

<details>
<summary>Hint 2</summary>

In `generate`, write ordinary code: check the month, query, log, and `return CompletableFuture.completedFuture(report)`. For a failure, throw an exception. The async proxy turns the exception into a failed future, because the whole method runs on the report thread.
</details>

<details>
<summary>Hint 3</summary>

The tests read the log output to see the report's thread and request id. If "runs on a thread of the report executor" fails, check that the log line starts with `Sales report for ` and that `@Async` names the executor.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Why does `generate(null)` return a future instead of throwing? What happens when finance starts 13 reports at the same moment?

### S09-03 · Scheduled jobs

**Build** · 30 min · [`PendingOrderCleanupService.java`](src/main/java/com/example/bookstore/order/PendingOrderCleanupService.java) · [`PendingOrderCleanupJob.java`](src/main/java/com/example/bookstore/order/PendingOrderCleanupJob.java) · [`ScheduledJobsConfiguration.java`](src/main/java/com/example/bookstore/common/ScheduledJobsConfiguration.java)

**Before you start:** lesson section 5.

**Ticket BOOK-421 · Unpaid orders block stock.** A `PENDING` order holds its copies until it is paid. Some orders are never paid, so their copies are never sold. Every 10 minutes, cancel the `PENDING` orders that are older than 72 hours and put their copies back into stock. Both durations come from `application.yml` (`bookstore.orders.pending-cleanup.*`, bound to `PendingOrderCleanupProperties`). Operations wants one switch for all scheduled jobs, `bookstore.scheduling.enabled`, so that they can run the jobs on one instance only; the tests switch them off.

1. `PendingOrderCleanupService.cancelExpiredOrders`: the cleanup itself. `OrderRepository.findByStatusAndOrderedAtBeforeOrderById` is given.
2. `PendingOrderCleanupJob.run`: the schedule.
3. `ScheduledJobsConfiguration`: scheduling, switchable by the property.

**You'll practice:** `@EnableScheduling`, `@Scheduled` with fixed delays from the configuration, a property-controlled configuration class, the `Clock` in time-based rules, testing a job's logic and its schedule separately.

<details>
<summary>Hint 1</summary>

"Older than 72 hours" means ordered before `now − maxAge`, with "now" from the `Clock`. `order.cancel()` changes the status; putting copies back is the caller's job (`Order.cancel()`'s Javadoc), with `inventory.increase(quantity)` for each line.
</details>

<details>
<summary>Hint 2</summary>

For the switch, Spring Boot has `@ConditionalOnBooleanProperty`. What should happen when the property is missing? For the schedule, `fixedDelayString` and `initialDelayString` accept `${…}` placeholders.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: What happens when three instances of the application run this job at the same moment, and is the result still correct? Why is the job class so thin?

### S09-04 · The transactional outbox

**Build** · 30 min · [`ShippingService.java`](src/main/java/com/example/bookstore/shipping/ShippingService.java) · [`OutboxRelay.java`](src/main/java/com/example/bookstore/outbox/OutboxRelay.java)

**Before you start:** lesson section 6.

**Ticket BOOK-430 · Tell delivery tracking about shipped orders, reliably.** The delivery-tracking service needs a message for every order that ships, through the message broker. Two rules from the last incident review: no message for a shipment that was rolled back, and no lost message when the broker is down for a while.

The outbox is prepared: read [`V3__create_outbox_events.sql`](src/main/resources/db/migration/V3__create_outbox_events.sql), [`OutboxEvent`](src/main/java/com/example/bookstore/outbox/OutboxEvent.java) and [`OutboxEventRepository`](src/main/java/com/example/bookstore/outbox/OutboxEventRepository.java) first. Then:

1. `ShippingService.ship`: ship the order, and write an `OrderShipped` event to the outbox in the same transaction.
2. `OutboxRelay.publishPending`: publish the waiting events, mark them published, retry failures on the next run, and let several relays work at the same time.

`OutboxRelayJob`, which runs the relay every 5 seconds, is given. The tests use [`FakeMessagePublisher`](src/test/java/com/example/bookstore/support/FakeMessagePublisher.java) as the broker. It can refuse chosen events, and hold one call while a second relay runs.

**You'll practice:** the transactional outbox, at-least-once delivery, `FOR UPDATE SKIP LOCKED` for several instances, keeping a transaction healthy while one item of a batch fails, JSON with Jackson's `JsonMapper`.

<details>
<summary>Hint 1</summary>

`ship` needs one transaction around `order.ship()` and `outboxEventRepository.save(OutboxEvent.create(…))`. The payload is `jsonMapper.writeValueAsString(new OrderShipped(…))`.
</details>

<details>
<summary>Hint 2</summary>

The relay's whole run is one transaction, so that the locks from `lockUnpublished` last while it publishes and marks the events. Catch a failure per event, around the publish call, so that one bad event doesn't stop the others. Which of the event's methods do you call in each case?
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Where can a duplicate message come from, and how does the delivery-tracking service cope with it? If event 2 of an order fails and event 3 of the same order succeeds, in which order does the consumer see them?

## Review questions

1. A listener with plain `@EventListener` throws an exception. What happens to the transaction of the method that published the event?
2. You publish an event outside any transaction, and its `@TransactionalEventListener` never runs. Why, and what are two ways to change that?
3. An `AFTER_COMMIT` listener saves an entity, and the entity never reaches the database. Why?
4. Why does an `@Async` method called from another method of the same class run synchronously?
5. An `@Async` method returns `void` and throws. Where does the exception go? What changes when it returns `CompletableFuture`?
6. A `ThreadPoolTaskExecutor` has core size 2, max size 10 and an unbounded queue. How many threads does it ever use, and why?
7. Your log lines from `@Async` methods sometimes show the request id of a different, earlier request. What's wrong?
8. What's the difference between `fixedDelay` and `fixedRate`? With Spring Boot's default scheduler, what happens when a `fixedRate` job takes longer than its rate?
9. Three instances of the application run the same `@Scheduled` job. Name two ways to make that safe.
10. Explain the dual-write problem with one example of each failure, and how the outbox solves both. What does it give up in exchange?

<details>
<summary>Answers</summary>

1. A plain listener runs synchronously inside the publisher's call, so the exception travels up through `publishEvent` into the publishing method. If that method doesn't catch it, it leaves the `@Transactional` method as an unchecked exception, and the whole transaction rolls back.
2. A transactional listener waits for the end of the current transaction, and with no transaction there is nothing to wait for, so Spring skips the event. Publish inside a transaction, or set `fallbackExecution = true` on the listener so that it runs at once when no transaction is active.
3. In `AFTER_COMMIT`, the transaction has already committed, but its resources are still attached to the thread. The save joins that finished transaction, and nothing commits it again. Annotate the listener with `@Transactional(propagation = Propagation.REQUIRES_NEW)` so that it gets a transaction of its own.
4. `@Async` works through the bean's proxy, and a call from one method of the class to another is a plain call on `this`, which never passes the proxy.
5. With `void`, the caller has nothing to look at, so Spring hands the exception to an `AsyncUncaughtExceptionHandler`, which logs it by default. With `CompletableFuture`, the exception completes the caller's future exceptionally: `get()` throws an `ExecutionException` with it as the cause.
6. Two. A `ThreadPoolExecutor` adds threads beyond the core size only when the queue is full, and an unbounded queue is never full. Every task beyond the first two waits in the queue.
7. The pool threads keep the MDC of the task they ran before: either nothing copies the caller's MDC, or the decorator copies it but doesn't clear it after the task. A `TaskDecorator` must set the caller's MDC (or an empty one) before the task and clear it in a `finally` block.
8. `fixedDelay` waits a fixed time after a run ends; `fixedRate` starts runs at a fixed rate, measured from the starts. The default scheduler never runs the same job twice at the same time: a `fixedRate` run that takes too long makes the next run start late, right after it ends, so the runs follow each other without any pause. Runs only overlap if the job hands its work to another thread, for example with `@Async`.
9. Make the job's work safe in parallel (each instance locks and takes different rows with `FOR UPDATE SKIP LOCKED`, and the changes are idempotent); turn scheduling on for one instance only (a property such as `bookstore.scheduling.enabled`); or take a distributed lock for each run (ShedLock with a database table).
10. The database and the broker can't share a transaction. Publish before the commit and roll back: the message describes a change that never happened. Commit and then fail to publish (the broker is down, the application stops): the message is lost. The outbox writes the event into the database in the same transaction as the change, so the two always agree, and a relay publishes it later, retrying until it works. In exchange, messages arrive a little later, and at least once rather than exactly once, so consumers must ignore duplicates.
</details>

## Further reading

- Spring Framework reference: [Application events](https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html#context-functionality-events) · [Transaction-bound events](https://docs.spring.io/spring-framework/reference/data-access/transaction/event.html) · [Task execution and scheduling](https://docs.spring.io/spring-framework/reference/integration/scheduling.html)
- Spring Boot reference: [Task execution and scheduling](https://docs.spring.io/spring-boot/reference/features/task-execution-and-scheduling.html) · [Virtual threads](https://docs.spring.io/spring-boot/reference/features/spring-application.html#features.spring-application.virtual-threads)
- MySQL reference manual: [Locking reads, `NOWAIT` and `SKIP LOCKED`](https://dev.mysql.com/doc/refman/9.7/en/innodb-locking-reads.html)
- SLF4J manual: [Mapped Diagnostic Context](https://www.slf4j.org/manual.html#mdc)
- Chris Richardson, [Pattern: Transactional outbox](https://microservices.io/patterns/data/transactional-outbox.html), and [Pattern: Idempotent consumer](https://microservices.io/patterns/communication-style/idempotent-consumer.html).
- Spring Modulith: [Working with application events](https://docs.spring.io/spring-modulith/reference/events.html), including its event publication registry.
