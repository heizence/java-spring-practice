# J08 · Concurrency

⏱ 7 sessions · weeks 5–6 · 7 exercises

In Node.js your code runs on one thread, so two callbacks never change the same variable at the same moment. A Java backend is different. A Spring Boot app handles every HTTP request on its own thread, hundreds of these threads run at the same time on several CPU cores, and they all share the same objects. This module shows how Java threads work, how shared data goes wrong (lost updates, invisible writes, deadlocks), and which tools keep it correct: locks, atomics, concurrent collections, `CompletableFuture`, virtual threads and scoped values. You need all of it in Part 2, as soon as your Spring services serve many requests at once.

Each exercise names the lesson sections to read first. Read them at the start of the session; together they take about 10 minutes.

## Lesson

### 1. Threads and the event loop

A **thread** is a sequence of instructions that runs on its own. Every Java program starts with one thread (the one that runs `main`) and can start more. JavaScript gives you one thread plus an event loop: callbacks and the code after an `await` run one at a time, so the code between two `await`s is never interrupted.

Java threads run truly in parallel on different CPU cores, and they share the same heap, the memory where all objects live. The operating system can also pause a thread between any two machine instructions and run another one. So any line of your code can interleave with lines that other threads run at the same moment.

```java
// TypeScript: safe. Nothing else runs until this function returns or awaits.
// Java: two threads that run this line at the same time can lose an increment (section 4).
counter = counter + 1;
```

| | Node.js | Java |
|---|---|---|
| Threads that run your code | 1 (worker threads don't share objects) | many, truly in parallel |
| Memory | each worker has its own | all threads share all objects |
| Waiting for I/O | `await` frees the thread | the thread waits (blocks) |
| Typical bug | blocking the event loop | race conditions, deadlocks |

### 2. Starting and joining threads

```java
Thread worker = Thread.ofPlatform().name("worker-1").start(() -> {
    System.out.println("running on " + Thread.currentThread().getName());
});
worker.join(); // waits until the thread has finished
```

- `start()` runs the `Runnable` on the new thread. Calling `run()` instead runs it on the current thread, like any method call. That's a classic mistake.
- `join()` waits until the thread has finished. Start all threads first, then join them. If you join inside the loop that starts them, they run one after another.
- A **platform thread** is a thin wrapper around an operating-system thread. It is expensive: it reserves 1–2 MB of memory for its stack, and the operating system handles a few thousand of them well, not millions. (Virtual threads are cheap; see section 13.)
- A **daemon thread** doesn't keep the JVM alive: when only daemon threads are left, the program ends. Create one with `Thread.ofPlatform().daemon()`.
- An exception that escapes from a thread ends that thread and is printed to standard error. The thread that started it never sees it. Executors (section 3) fix this.
- Older code writes `new Thread(runnable).start()`. It does the same. The builders `Thread.ofPlatform()` and `Thread.ofVirtual()` are the modern style (Java 21).

**Interruption.** Java can't kill a thread. You can only ask it to stop: `thread.interrupt()` sets the thread's *interrupt flag*. Methods that wait (`Thread.sleep`, `join`, `BlockingQueue.take`, `Future.get` and many more) notice the flag and throw `InterruptedException`. That's why so many methods declare it. When you catch it, don't swallow it. Let it propagate (declare `throws InterruptedException`), or set the flag again so that the code further up still sees the request to stop:

```java
try {
    Thread.sleep(Duration.ofSeconds(1));
} catch (InterruptedException e) {
    Thread.currentThread().interrupt(); // keep the "please stop" signal
    return;                             // and stop
}
```

### 3. Executors, thread pools and futures

Creating a thread for every task is wasteful, and too many threads can make a server run out of memory. An **executor** runs tasks for you on a **thread pool**: a set of threads that take tasks from a queue, one after another.

```java
try (ExecutorService pool = Executors.newFixedThreadPool(4)) {
    Future<Integer> stock = pool.submit(() -> warehouse.inStock("book-42")); // returns at once
    Future<Integer> reserved = pool.submit(() -> orders.reserved("book-42"));
    int available = stock.get() - reserved.get();                            // get() waits for the result
} // close(): accept no new tasks, then wait until all submitted tasks have finished
```

| Factory | Threads | Use it for |
|---|---|---|
| `newFixedThreadPool(n)` | exactly `n` | a known amount of parallel work |
| `newCachedThreadPool()` | as many as needed, reused, no upper limit | short tasks; dangerous with unlimited input |
| `newVirtualThreadPerTaskExecutor()` | one new virtual thread per task | many blocking tasks (section 13) |
| `newSingleThreadExecutor()` | 1 | tasks that must run one at a time, in order |

- `submit(task)` returns a `Future<T>`: a handle to a result that doesn't exist yet. `future.get()` waits for it. If the task threw, `get()` throws an `ExecutionException`, and `getCause()` is the task's own exception. Unwrap it before you report it.
- `ExecutorService` is `AutoCloseable` (since Java 19), so use try-with-resources. `close()` calls `shutdown()` (accept no new tasks) and waits until the running and the queued tasks have finished. A pool that is never shut down keeps its threads alive.
- `shutdownNow()` removes the queued tasks and interrupts the running ones. Use it when the remaining work has become useless, for example after a failure.
- `invokeAll(tasks)` submits a list of tasks and waits for all of them.

### 4. Race conditions

A **race condition** is a bug where the result depends on how the steps of several threads happen to interleave. Most of the time the code works, so the tests on your laptop pass. Then it fails under load in production. Two patterns cause most race conditions.

**Read-modify-write.** `hits++` looks like one step, but it is three: read `hits`, add 1, write the result back.

| Thread A | Thread B | `hits` |
|---|---|---|
| reads 5 | | 5 |
| | reads 5 | 5 |
| writes 6 | | 6 |
| | writes 6 | 6 ← one hit is lost |

**Check-then-act.** A thread checks a condition and then acts on it, but another thread changes the situation in between:

```java
if (!sessions.containsKey(userId)) {    // check: both threads see "no session yet"
    sessions.put(userId, newSession()); // act: both create one; the second overwrites the first
}
```

Also, a plain `HashMap` that several threads change at the same time can lose entries or corrupt its internal structure. It isn't made for that.

There are three ways out. The next sections explain each one.

```java
// 1. A lock: only one thread at a time runs the guarded code (section 5).
synchronized (this) {
    hits++;
}

// 2. An atomic class: the read-modify-write happens as one indivisible step (section 7).
private final AtomicLong hits = new AtomicLong();
hits.incrementAndGet();

// 3. A concurrent collection with an atomic compound method (section 10).
private final ConcurrentMap<String, Session> sessions = new ConcurrentHashMap<>();
Session previous = sessions.putIfAbsent(userId, session); // null if this call inserted `session`
```

### 5. `synchronized` and intrinsic locks

Every Java object has a built-in lock, called its **intrinsic lock** or **monitor**. A `synchronized` block takes the lock of the given object and releases it at the end, also when an exception is thrown. While one thread holds the lock, other threads that want it wait.

```java
public class Inventory {

    private final Map<String, Integer> stock = new HashMap<>(); // guarded by "this"

    public synchronized void add(String sku, int amount) {     // locks "this"
        stock.merge(sku, amount, Integer::sum);
    }

    public synchronized int count(String sku) {                 // reads lock too (section 6)
        return stock.getOrDefault(sku, 0);
    }
}
```

- A `synchronized` method locks `this`. A `static synchronized` method locks the class.
- Every access to the guarded data must hold the *same* lock, reads included. A lock protects data only if all code uses it. Write a comment like `// guarded by this` next to the field.
- Locks are **reentrant**: a thread that holds a lock can take it again, for example when one synchronized method calls another one.
- Keep the guarded code short. Never call slow or unknown code (a remote service, a callback) while you hold a lock.

### 6. Visibility: the Java Memory Model and `volatile`

Race conditions aren't the only problem. Without synchronization, a thread may **never see** a value that another thread wrote. CPU cores keep values in their own caches, and the JIT compiler (the part of the JVM that turns frequently used code into machine code while the program runs) may keep a field in a CPU register or reorder reads and writes. Inside one thread you never notice. Across threads you do:

```java
private boolean stopRequested; // BUG: not volatile

void run() {                 // on the worker thread
    while (!stopRequested) { // the JIT may read the field once, before the loop, and loop forever
        work();
    }
}

void stop() {                // on another thread
    stopRequested = true;
}
```

The **Java Memory Model** (JMM) defines when a write by one thread is guaranteed to be visible to another. Its key idea is **happens-before**: if action A happens-before action B, then B sees everything that A did and everything that happened before A. These are the most important happens-before edges, in plain terms:

- Releasing a lock, and a later taking of the *same* lock.
- Writing a `volatile` field, and a later read of the *same* field.
- `thread.start()`, and everything the new thread does.
- Everything a thread does, and another thread returning from `join()` on it.
- Submitting a task to an executor, and the task running. The task finishing, and `future.get()` returning.
- The classes in `java.util.concurrent`: for example, putting an element into a concurrent collection and a later read that finds it, or `countDown()` and an `await()` that returns because of it.

Without such an edge, Java guarantees nothing about what another thread sees. That's why reading methods need the lock too, not only the writing ones.

A **`volatile`** field is the lightest fix for visibility: every read sees the latest write. It's the right tool for a flag that one thread writes and other threads read. It does **not** make compound actions atomic: with `volatile long hits`, `hits++` still loses updates, because the read and the write are still two steps.

Visibility bugs depend on the JIT compiler, the CPU and timing. A missing `volatile` can work for years on your laptop and fail on a server. A test can only catch it when the JIT happens to optimize the code in a certain way, so check your code against the happens-before rules instead of trusting a green test.

### 7. Atomics and compare-and-set

The classes in `java.util.concurrent.atomic` make compound actions on one variable thread-safe, without locks.

```java
var count = new AtomicLong();
count.incrementAndGet();                // like ++count, but atomic
count.addAndGet(5);
count.updateAndGet(n -> n * 2);         // any function
count.accumulateAndGet(42, Math::max);  // combine with a value: here "keep the larger one"

var config = new AtomicReference<Config>(initialConfig);
config.set(newConfig);                  // publish a new immutable object to all threads
```

They are built on **compare-and-set** (CAS), an operation that the CPU performs atomically: "set the value to B, but only if it is still A". If another thread changed the value in the meantime, CAS fails, and you read the new value and try again. That is a **CAS loop**:

```java
long current;
do {
    current = max.get();
} while (value > current && !max.compareAndSet(current, value));
```

`updateAndGet` and `accumulateAndGet` run this loop for you. Because they may retry, the function you pass must not have side effects.

**`LongAdder`** is a counter for many writers. It spreads the additions over several internal cells, so threads rarely compete for the same memory, and it adds the cells up when you call `sum()`. It's faster than `AtomicLong` for counters that many threads increase all the time, such as request metrics. `sum()` is not an atomic snapshot while other threads are still adding.

Each atomic is consistent on its own, but two atomics are not consistent *with each other*. If you update a `count` and a `sum` separately, a reader can see the new count with the old sum. When several values must change together, put them into one immutable record in an `AtomicReference`, or use a lock.

### 8. Explicit locks: `ReentrantLock` and `ReadWriteLock`

`ReentrantLock` (in `java.util.concurrent.locks`) does what `synchronized` does, plus some things `synchronized` can't do:

```java
private final ReentrantLock lock = new ReentrantLock();

void update() {
    lock.lock();
    try {
        // guarded code
    } finally {
        lock.unlock(); // always in finally: a lock that is never released blocks everyone else forever
    }
}

boolean tryUpdate() throws InterruptedException {
    if (!lock.tryLock(100, TimeUnit.MILLISECONDS)) { // waits at most 100 ms
        return false;                               // gives up instead of hanging
    }
    try {
        // guarded code
        return true;
    } finally {
        lock.unlock();
    }
}
```

- `tryLock()` without arguments doesn't wait at all. `tryLock(time, unit)` waits up to the timeout. `lockInterruptibly()` waits, but stops waiting when the thread is interrupted. A thread that waits for a `synchronized` lock can't be interrupted.
- Every way out of the code that took a lock must release it, including early returns. The `try`/`finally` pattern above does that.
- `synchronized (x)` and a `ReentrantLock` that belongs to `x` are **different locks**. They don't exclude each other. Code that guards the same data must use the same lock.
- Prefer `synchronized` for simple cases: it's shorter, and you can't forget to unlock. Use `ReentrantLock` when you need timeouts, interruptible waiting or fairness.

A **`ReentrantReadWriteLock`** has a read lock that many threads can hold at the same time, and a write lock that excludes everybody else. It helps with data that is read very often and changed rarely:

```java
private final ReadWriteLock settingsLock = new ReentrantReadWriteLock();
private final Map<String, String> settings = new HashMap<>(); // guarded by settingsLock

String get(String key) {
    settingsLock.readLock().lock();
    try {
        return settings.get(key);
    } finally {
        settingsLock.readLock().unlock();
    }
}

void put(String key, String value) {
    settingsLock.writeLock().lock();
    try {
        settings.put(key, value);
    } finally {
        settingsLock.writeLock().unlock();
    }
}
```

### 9. Deadlock and lock ordering

A **deadlock** happens when threads wait for each other in a circle, so none of them can go on. The classic example is a money transfer that locks the source account first and the target account second:

| Thread A: `transfer(a, b)` | Thread B: `transfer(b, a)` |
|---|---|
| locks `a` | locks `b` |
| waits for `b` … | waits for `a` … |

Both wait forever. A deadlocked server doesn't crash. It stops answering, while more and more requests get stuck.

The standard fix is a **global lock order**: every thread takes the locks in the same order, for example by account id. Two transfers between the same accounts then both start with the same lock, so one of them waits until the other is done.

```java
Account first = from.id() < to.id() ? from : to;
Account second = first == from ? to : from;
first.lock().lock();
try {
    second.lock().lock();
    try {
        move(from, to, amount);
    } finally {
        second.lock().unlock();
    }
} finally {
    first.lock().unlock();
}
```

Other ways out: hold only one lock at a time, or use `tryLock` with a timeout and give up (then report "busy" or try again later). To find a deadlock in a running JVM, print a thread dump with `jstack <pid>` or `jcmd <pid> Thread.print`. It lists the threads involved after the line "Found one Java-level deadlock".

### 10. Concurrent collections

The collections in `java.util.concurrent` are safe for many threads, and they offer compound methods that are atomic.

**`ConcurrentHashMap`** is the default map for shared data:

```java
ConcurrentMap<String, Long> wordCounts = new ConcurrentHashMap<>();
ConcurrentMap<String, Session> sessions = new ConcurrentHashMap<>();
ConcurrentMap<String, Book> books = new ConcurrentHashMap<>();

wordCounts.merge(word, 1L, Long::sum);                         // put 1, or add 1 to the value that is there
sessions.putIfAbsent(userId, session);                         // insert only if absent; returns the old value or null
books.computeIfAbsent(isbn, key -> loadBook(key));             // load each book only once
wordCounts.compute(word, (key, old) -> old == null ? 1L : old + 1); // any change to one entry
```

- Each of these methods is atomic for its key. A `get` followed by a `put` isn't.
- The function you pass to `compute`, `computeIfAbsent` or `merge` runs while part of the map is locked. Keep it short, and never change the same map inside it.
- `ConcurrentHashMap` doesn't allow `null` keys or values.
- `Collections.synchronizedMap(map)` makes every single call thread-safe, but compound actions and iteration still need your own `synchronized (map)` block. Prefer `ConcurrentHashMap`.

**`CopyOnWriteArrayList`** copies its whole internal array on every change. Reading and iterating need no lock and never throw `ConcurrentModificationException`. Use it for lists that are read very often and changed rarely, such as a list of event listeners.

A **`BlockingQueue`** connects producer threads with consumer threads:

| Method | When the queue is full (adding) or empty (taking) |
|---|---|
| `put(x)` / `take()` | waits until there is room / an element |
| `offer(x, time, unit)` / `poll(time, unit)` | waits up to the timeout, then returns `false` / `null` |
| `offer(x)` / `poll()` | doesn't wait: returns `false` / `null` at once |

An `ArrayBlockingQueue` has a fixed capacity. When the consumers are slow, the queue fills up and `put` makes the producer wait. This **backpressure** keeps memory use bounded. A `LinkedBlockingQueue` created without a capacity never fills up, so a fast producer can run out of memory. To tell the consumers that no more items will come, the producer puts one special **end marker**, often called a *poison pill*, per consumer into the queue.

### 11. Coordination: `CountDownLatch` and `Semaphore`

A **`CountDownLatch`** is a gate that opens once. It starts with a count. `countDown()` lowers the count, and `await()` waits until it reaches 0.

```java
var startSignal = new CountDownLatch(1);       // a starting gate
var finished = new CountDownLatch(threads);    // a finish line
for (int i = 0; i < threads; i++) {
    Thread.ofPlatform().start(() -> {
        try {
            startSignal.await();               // wait for the start signal
            doWork();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            finished.countDown();              // in finally: a task that fails still counts as finished
        }
    });
}
startSignal.countDown();                       // open the gate: all threads start together
finished.await();                              // wait for all of them
```

A **`Semaphore`** holds a number of permits. `acquire()` takes one (and waits while none is left), and `release()` gives it back. Use it to limit how many threads use something at the same time, for example at most 10 parallel calls to a slow partner API:

```java
private final Semaphore permits = new Semaphore(10);

Response call(Request request) throws InterruptedException {
    permits.acquire();
    try {
        return partnerApi.send(request);
    } finally {
        permits.release();
    }
}
```

The J08 tests also use a `CyclicBarrier`: a gate for a fixed number of threads that opens when all of them have arrived, and that can be used again afterwards.

### 12. `CompletableFuture` and `Promise`

`CompletableFuture<T>` is Java's version of `Promise<T>`: a value that arrives later, with methods that chain and combine work without blocking a thread.

```java
CompletableFuture<BigDecimal> price = prices.priceOf(id); // both lookups start now
CompletableFuture<Integer> stock = stocks.inStock(id);

CompletableFuture<Offer> offer = price
        .thenCombine(stock, (p, s) -> new Offer(id, p, s)) // runs when both are there
        .orTimeout(2, TimeUnit.SECONDS)                     // fails with TimeoutException after 2 s
        .exceptionally(error -> Offer.unavailable(id));     // a fallback value after a failure
```

| TypeScript | Java |
|---|---|
| `p.then(x => f(x))` | `p.thenApply(x -> f(x))` |
| `p.then(x => loadMore(x))`, where `loadMore` returns a promise | `p.thenCompose(x -> loadMore(x))` |
| `Promise.all([a, b])` | `a.thenCombine(b, …)` or `CompletableFuture.allOf(a, b)` |
| `p.catch(e => fallback)` | `p.exceptionally(e -> fallback)` |
| `p.then(onValue, onError)` | `p.handle((value, error) -> …)` |
| `p.finally(f)` | `p.whenComplete((value, error) -> …)` |
| `Promise.race([a, b])` | `CompletableFuture.anyOf(a, b)` |
| `await p` | `p.join()`, which **blocks** the thread |

Differences to watch out for:

- **Blocking.** `join()` and `get()` block the current thread until the value is there. Don't call them on a future that may still be running; combine futures with `thenCombine` or `allOf` instead. (Inside the callback of `thenCombine` or `allOf`, a `join()` on their inputs doesn't block: they are complete.)
- **`thenApply` versus `thenCompose`.** A function that returns another future gives `CompletableFuture<CompletableFuture<T>>` with `thenApply`. `thenCompose` flattens it, like returning a promise from `then`.
- **`allOf` waits for all.** `Promise.all` rejects as soon as one promise rejects. `allOf` and `thenCombine` wait until every input has completed, and only then report the failure. `allOf` returns a `CompletableFuture<Void>`, so read the values with `join()` in the next stage.
- **Wrapped exceptions.** A failure that travels through dependent stages arrives wrapped in a `CompletionException`, and `get()` wraps it in an `ExecutionException`. Look at `getCause()`.
- **Threads.** A callback like `thenApply` runs on the thread that completes the previous stage, or right away on the calling thread if that stage is already complete. The `…Async` variants run on the common `ForkJoinPool` or on an executor that you pass. Never run blocking work on the common pool.
- **Timeouts.** `orTimeout(time, unit)` fails the future with a `TimeoutException`. `completeOnTimeout(value, time, unit)` completes it with a default value. Both change the future you call them on, so call them on a future that you created, not on one that a library gave you and may share.

### 13. Virtual threads

A **virtual thread** (final since Java 21) is a thread that the JVM manages, not the operating system. Millions of them can exist at once. When a virtual thread blocks on I/O, on `sleep` or on a lock, the JVM takes it off its **carrier thread** (a platform thread from a small pool) and runs another virtual thread there. So blocking code becomes cheap again: you write plain, sequential code and can still handle thousands of requests at the same time.

```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (String url : urls) {
        executor.submit(() -> download(url)); // one virtual thread per download
    }
} // waits for all downloads
```

- Virtual threads help with **blocking I/O**: HTTP calls, database queries, file access. They don't make CPU-heavy work faster; there, the number of CPU cores is the limit.
- Don't pool virtual threads. Create one per task. To protect a limited resource, such as a database with 20 connections, use a `Semaphore` or the connection pool, not a thread pool.
- Since Java 24, `synchronized` no longer pins a virtual thread to its carrier, so the older advice "replace `synchronized` with `ReentrantLock` for virtual threads" is outdated. Calls into native code can still pin.
- In Spring Boot, `spring.threads.virtual.enabled=true` makes Tomcat and `@Async` use virtual threads.

### 14. `ThreadLocal` and `ScopedValue`

Sometimes many places of a request need the same data, such as the request id for logging or the current user, and you don't want to pass it through every method. A **`ThreadLocal<T>`** stores one value per thread:

```java
private static final ThreadLocal<String> REQUEST_ID = new ThreadLocal<>();

REQUEST_ID.set("req-42");
try {
    handle(request);      // any code on this thread can call REQUEST_ID.get()
} finally {
    REQUEST_ID.remove();  // essential in a thread pool
}
```

Spring keeps the current transaction, the security context and the current request in thread-locals, and logging frameworks keep the MDC there. Their big danger is **leaks in thread pools**. A pool thread keeps running after the request has ended. If you forget `remove()`, the next request on that thread sees the old request id or the old user, and the old value (with everything it references) can never be garbage-collected. Also, any code can change a thread-local at any time, which makes bugs hard to trace.

A **`ScopedValue<T>`** (final in Java 25) binds a value for the duration of one call and unbinds it automatically:

```java
private static final ScopedValue<String> REQUEST_ID = ScopedValue.newInstance();

ScopedValue.where(REQUEST_ID, "req-42").run(() -> handle(request));

// anywhere inside handle(...), on the same thread:
String id = REQUEST_ID.isBound() ? REQUEST_ID.get() : "none";
```

- The binding ends when `run` (or `call`) returns or throws. It can't leak into the next task of a pooled thread.
- Code inside can't change the value. It can bind a new value for a nested call (**rebinding**), and after the nested call the outer value is back.
- `get()` throws `NoSuchElementException` when nothing is bound, so check `isBound()` first, or use `orElse(defaultValue)`. Since Java 25, `orElse(null)` throws `NullPointerException`.
- Threads that you start yourself don't inherit the binding. Threads forked inside a `StructuredTaskScope` do, but that API is still a preview feature in Java 25.
- Scoped values are cheap, which matters when you run a million virtual threads.

### 15. Immutability: thread safety for free

An object that can't change can be shared by any number of threads without any lock. Records with immutable fields, `List.copyOf`, `Map.copyOf`, `String`, `BigDecimal` and the `java.time` types are immutable. The JMM also guarantees that other threads see the `final` fields of an object as the constructor set them, as long as the constructor doesn't let `this` escape.

So the simplest concurrent design is often: keep mutable state inside one thread, and pass immutable objects between threads. When shared state must change, replace an immutable object as a whole, with an `AtomicReference<Settings>` and `updateAndGet`, or with a `volatile` field that holds an immutable record.

### 16. Spring: one thread per request

Spring MVC on Tomcat handles each HTTP request on a thread from a pool (200 threads by default). Your controllers, services and repositories are **singleton beans**: one instance, shared by all those threads. So every mutable field of a singleton bean is shared state:

```java
@Service
public class PricingService {

    private BigDecimal lastPrice;                             // BUG: every request writes the same field
    private final Map<String, Quote> cache = new HashMap<>(); // BUG: changed by many threads at once

    public Quote quote(String sku) { … }
}
```

The rules:

- Keep singleton beans **stateless**: only `final` fields that hold other beans or immutable configuration.
- Keep request data in local variables and parameters. They live on the stack of the thread that runs the request, so other threads can't see them.
- When you do need shared mutable state, such as a counter or a small cache, use the tools from this module: atomics, `ConcurrentHashMap`, or a cache library (module S10).
- Data of the current request belongs in the thread-locals that the framework manages (`SecurityContextHolder`, `RequestContextHolder`, the logging MDC), not in fields.

Module S08 continues with concurrency in the database: optimistic and pessimistic locking, and idempotency.

## TypeScript → Java

| TypeScript | Java | Watch out for |
|---|---|---|
| one thread with an event loop | many threads, truly in parallel | any line can interleave with other threads |
| `new Worker(file)` (no shared objects) | `Thread.ofPlatform().start(task)`, executors | all threads share all objects |
| `Promise<T>` | `CompletableFuture<T>` | callbacks run on other threads |
| `await p` | `p.join()` or `future.get()` | blocks the current thread |
| `p.then(f)` | `thenApply(f)` or `thenCompose(f)` | use `thenCompose` when `f` returns a future |
| `Promise.all([a, b])` | `CompletableFuture.allOf(a, b)`, `a.thenCombine(b, f)` | waits for all inputs, even after a failure |
| `p.catch(f)` | `exceptionally(f)` | `f` often gets a `CompletionException` wrapper |
| `AbortSignal.timeout(ms)` | `orTimeout`, `completeOnTimeout` | they change the future they're called on |
| `Atomics.add` on a `SharedArrayBuffer` | `AtomicLong.incrementAndGet()`, `LongAdder` | `volatile` alone isn't atomic |
| `AsyncLocalStorage` | `ScopedValue` (or `ThreadLocal`) | a `ThreadLocal` needs `remove()` in pools |
| a `Map` shared by callbacks | `ConcurrentHashMap` | use `merge` or `putIfAbsent`, not `get` + `put` |
| (not needed) | `synchronized`, `ReentrantLock` | reads need the lock too |

## In real projects

- Prefer the highest-level tool that fits: immutable objects first, then concurrent collections and atomics, then locks. Use executors instead of raw threads. [Concurrency](../../docs/handbook/concurrency.md) · [Immutability](../../docs/handbook/immutability.md)
- Close every executor with try-with-resources, give threads meaningful names, and never combine an unbounded pool or queue with unlimited input. [Concurrency](../../docs/handbook/concurrency.md)
- Never swallow `InterruptedException`: rethrow it, or restore the flag with `Thread.currentThread().interrupt()`. Unwrap `ExecutionException` and `CompletionException` before you report a failure. [Exceptions](../../docs/handbook/exceptions.md)
- Write `// guarded by <lock>` next to every field that needs a lock, and keep locked code short. Never call a remote service or a callback while you hold a lock. [Concurrency](../../docs/handbook/concurrency.md)
- Use `ConcurrentHashMap.merge`, `computeIfAbsent` and `putIfAbsent` instead of check-then-act. [Collections and generics](../../docs/handbook/collections-and-generics.md)
- Keep the lambdas you pass to `CompletableFuture`, `compute` or `updateAndGet` short and free of side effects. [Functional style](../../docs/handbook/functional-style.md)
- Return `Optional` for "maybe there is a value", such as the current request id. [Null and Optional](../../docs/handbook/null-and-optional.md)
- Keep amounts of money in `long` cents or `BigDecimal`, never in `double`. [Dates and money](../../docs/handbook/dates-and-money.md)
- Keep Spring singleton beans stateless: only `final` fields. [Concurrency](../../docs/handbook/concurrency.md)
- Test concurrent code under stress: start many threads at the same moment, repeat many times, use timeouts and daemon threads, and check invariants such as "the total amount of money stays the same". A passing test doesn't prove that there is no race, so also check the code against the happens-before rules. [Testing](../../docs/handbook/testing.md)

## Exercises

### J08-01 · Threads and executors

**Build** · 30 min · [`ThreadBasics.java`](src/main/java/practice/j08/ex01/ThreadBasics.java) · [`ParallelFetcher.java`](src/main/java/practice/j08/ex01/ParallelFetcher.java)

Read lesson sections 1–3 first. You start and join plain threads, then build one of the most common concurrent tasks in backend work: call a slow service for many ids at once on a fixed-size pool, and collect the results in order. Real code does this to load the prices of a shopping cart, or to call several partner APIs in parallel.

**You'll practice:** `Thread.ofPlatform()`, `start()` and `join()`, `ExecutorService` with try-with-resources, `Future.get()`, unwrapping `ExecutionException`, `shutdownNow()`.

**Rules:** in the Javadoc of `ThreadBasics` and `ParallelFetcher`. `FetchException` is given.

<details>
<summary>Hint 1</summary>

`runAll`: `List.copyOf(tasks)` rejects a `null` list and `null` elements before you start anything. Then write two loops: one that starts all threads, and one that joins them. `Thread.ofPlatform().name("worker-", 0)` gives you a builder that numbers the thread names for you.
</details>

<details>
<summary>Hint 2</summary>

`fetchAll`: submit all calls first, and keep the `Future`s in a list in the order of the ids. Then call `get()` on them in that order. When `get()` throws an `ExecutionException`, its `getCause()` is the fetcher's exception. Call `pool.shutdownNow()` before you throw, so that `close()` doesn't wait for the remaining calls.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: What happens to the pool threads when `fetchAll` throws, and how do you know? Why doesn't the order of the results depend on which call finishes first?

### J08-02 · Race conditions

**Fix** · 30 min · [`HitCounter.java`](src/main/java/practice/j08/ex02/HitCounter.java)

Read lesson sections 4–5 first. The analytics team reports two problems. The page-view numbers on the dashboard are lower than the numbers in the web server logs. And some visitors are listed with a landing page that wasn't the first page they opened. `HitCounter` is one object shared by all request threads, and each method works fine in a single-threaded test. Find the race conditions and fix them.

**You'll practice:** recognizing read-modify-write and check-then-act races, `AtomicLong`, `ConcurrentHashMap.putIfAbsent` (or `synchronized`), reading the results of race tests.

**Rules:** in the Javadoc of `HitCounter`. The tests in the "Concurrency" group show the symptoms; read the numbers in their failure messages.

<details>
<summary>Hint 1</summary>

There are two bugs. `hits++` is three steps. And `registerIfAbsent` checks with `containsKey` and acts with `put`: what can another thread do between the two?
</details>

<details>
<summary>Hint 2</summary>

Look for a map method that checks and inserts in one atomic step, and tells you whether it inserted. The map itself must also be one that many threads may change at the same time.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Would `private volatile long hits` have been enough? If you fixed the class with `synchronized`, do the reading methods take the lock too, and why must they?

### J08-03 · Atomics and visibility

**Build** · 30 min · [`Stats.java`](src/main/java/practice/j08/ex03/Stats.java) · [`PiEstimator.java`](src/main/java/practice/j08/ex03/PiEstimator.java)

Read lesson sections 6–7 first. `Stats` collects response-time statistics from many request threads without locks, the way a metrics library does. `PiEstimator` runs a CPU-heavy loop on a background thread until another thread tells it to stop: the classic stop flag, and the classic place for a visibility bug.

**You'll practice:** `AtomicLong`, `LongAdder`, `accumulateAndGet` and compare-and-set loops, `volatile`, happens-before through `Thread.join()`.

**Rules:** in the Javadoc of `Stats` and `PiEstimator`. The `Snapshot` record is given.

<details>
<summary>Hint 1</summary>

`Stats` needs three fields and two short methods. The maximum is the interesting part: `if (value > max.get()) max.set(value)` is check-then-act, while `max.accumulateAndGet(value, Math::max)` is atomic.
</details>

<details>
<summary>Hint 2</summary>

`PiEstimator.run()` keeps the sum and the term counter in local variables, loops until a stop was requested, and stores the result in fields at the end. Which field does another thread write *while* `run()` is running? That one must be `volatile`.
</details>

**Done?** Compare with the reference on the `solutions` branch. Then remove `volatile` from your stop flag and run the tests again. Why do they fail now, and why could the same code pass on another JVM? Why don't the result fields need `volatile`?

### J08-04 · Locks and deadlocks

**Fix** · 30 min · [`Bank.java`](src/main/java/practice/j08/ex04/Bank.java)

Read lesson sections 8–9 first. The payments team has three bug reports. Under load, the transfer service sometimes stops answering until somebody restarts it. Since the new `tryTransfer` endpoint went live, the total amount of money in the system sometimes changes. And now and then an account stays blocked forever after a `tryTransfer` gave up. `Account` is correct; the bugs are in `Bank`.

**You'll practice:** recognizing a deadlock, lock ordering, `ReentrantLock` with `lock`, `tryLock` and `try`/`finally`, using one lock for one piece of data.

**Rules:** in the Javadoc of `Bank` and `Account`. A test that deadlocks fails after 5 seconds with "Is there a deadlock or an endless loop?". Its threads are daemon threads, so they can't stop the test run from finishing.

<details>
<summary>Hint 1</summary>

Imagine two threads: one calls `transfer(a, b)`, the other `transfer(b, a)`. Which lock does each of them take first? `tryTransfer` already shows a better way.
</details>

<details>
<summary>Hint 2</summary>

`synchronized (from)` takes the object's monitor, but the balance is guarded by `from.lock()`. Does `transfer` keep a `tryTransfer` on the same account out? And in `tryTransfer`, follow the path where the second `tryLock` fails: who releases the first lock?
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: `tryTransfer` gives up after a timeout, so could it lock in argument order without harm? Try it and run the test with opposite `tryTransfer`s.

### J08-05 · Concurrent collections and coordination

**Build** · 30 min · [`WordCounter.java`](src/main/java/practice/j08/ex05/WordCounter.java) · [`StartingGate.java`](src/main/java/practice/j08/ex05/StartingGate.java) · [`Pipeline.java`](src/main/java/practice/j08/ex05/Pipeline.java)

Read lesson sections 10–11 first. Three small tools that you'll meet again and again: counting into a shared `ConcurrentHashMap`, a starting gate built from two `CountDownLatch`es (the core of every load test), and a producer-consumer pipeline with a bounded queue (the pattern behind import jobs and message consumers).

**You'll practice:** `ConcurrentHashMap.merge`, `CountDownLatch`, `ArrayBlockingQueue` with `put` and `take`, backpressure, end markers (poison pills).

**Rules:** in the Javadoc of the three classes. `WordCounter.words` is given.

<details>
<summary>Hint 1</summary>

`Pipeline.process`: start the consumers on a fixed thread pool (try-with-resources), then produce on the calling thread. Each consumer loops: it takes an element from the queue, stops if the element is an end marker, and otherwise passes the item to the worker.
</details>

<details>
<summary>Hint 2</summary>

The queue needs a type that can hold an item or an end marker. A small private `sealed interface Message<T>` with two records, `Item<T>(T value)` and `End<T>()`, works well with a `switch` on the element you took.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: What would happen with one end marker for four consumers? What does `offer` do differently from `put` when the queue is full?

### J08-06 · CompletableFuture

**Build** · 30 min · [`ProductPageService.java`](src/main/java/practice/j08/ex06/ProductPageService.java)

Read lesson section 12 first. A product page needs a price, the stock level and the reviews, each from a different service. Calling them one after another adds up their response times; calling them at the same time costs only the slowest one. You combine three asynchronous answers, with a fallback for the optional part and a timeout for the required parts. A backend for a web or mobile front end does this for almost every page it serves.

**You'll practice:** `thenCombine` or `allOf`, `exceptionally`, `orTimeout`, `completeOnTimeout`, unwrapping `CompletionException`.

**Rules:** in the Javadoc of `ProductPageService`. The service interfaces, `ProductPage` and `ProductPageException` are given. The tests use fake services that return futures which the test completes by hand, so they are fast and don't depend on timing.

<details>
<summary>Hint 1</summary>

Call the three services first and keep their futures in local variables. Then build the page's future from them. Don't call `join()` or `get()` on them in `load()`.
</details>

<details>
<summary>Hint 2</summary>

Order matters. `reviews.exceptionally(e -> List.of())` turns a failure into an empty list, and a `completeOnTimeout(List.of(), …)` after it handles slow reviews. For the required parts, `CompletableFuture.allOf(price, stock).orTimeout(…)`. At the end, an `exceptionally` can throw a `ProductPageException` with the unwrapped cause.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Why doesn't `price.join()` block inside the `thenCombine` callback? What would change if you called `orTimeout` on the future that the price service returned, instead of on your own future?

### J08-07 · Virtual threads and scoped values

**Build** · 30 min · [`BlockingTasks.java`](src/main/java/practice/j08/ex07/BlockingTasks.java) · [`RequestContext.java`](src/main/java/practice/j08/ex07/RequestContext.java)

Read lesson sections 13–16 first. You run 10,000 blocking tasks at once on virtual threads, and build a request context that makes the current request id available everywhere, without the leaks of a thread-local. These are the building blocks of request handling in a modern Java server.

**You'll practice:** `Executors.newVirtualThreadPerTaskExecutor()`, `Future.state()`, `ScopedValue.where(…).run(…)` and `call(…)`, `isBound()`, rebinding.

**Rules:** in the Javadoc of `BlockingTasks` and `RequestContext`. JUnit runs every test method on a new thread, so each test starts outside of any request.

<details>
<summary>Hint 1</summary>

`BlockingTasks.runAll`: keep the futures that `submit` returns. After the try-with-resources block every future is done, and `future.state() == Future.State.FAILED` tells you which tasks threw.
</details>

<details>
<summary>Hint 2</summary>

`RequestContext` needs one `private static final ScopedValue<String>`. `runWithRequestId` is one line with `ScopedValue.where(…).run(…)`. `call` expects a `ScopedValue.CallableOp`, and the method reference `action::get` fits it.
</details>

**Done?** Compare with the reference on the `solutions` branch. Then change the executor in `BlockingTasks` to `Executors.newFixedThreadPool(100)` and run the 10,000-task test: how long does it take now, and why? How would a `ThreadLocal` version of `RequestContext` have to restore the outer id after a nested call?

## Review questions

1. Why can `count++` lose updates in Java, but not in a Node.js callback?
2. A colleague made a counter field `volatile` to fix lost updates. Is the bug fixed? What does `volatile` guarantee, and what doesn't it?
3. Name three actions that create a happens-before edge.
4. What's the difference between `shutdown()`, `shutdownNow()` and `close()` on an `ExecutorService`?
5. Why does `future.get()` throw an `ExecutionException` instead of the task's own exception, and what should you do with it?
6. Two threads call `transfer(a, b)` and `transfer(b, a)` at the same time. Describe the deadlock, and two ways to prevent it.
7. When is `tryLock()` without a timeout the wrong choice?
8. Two threads run `map.put(key, map.getOrDefault(key, 0) + 1)` on a `ConcurrentHashMap` at the same time. What can go wrong, and how do you fix it?
9. How does `CompletableFuture.allOf` behave differently from `Promise.all` when one input fails?
10. When do virtual threads help, and when don't they?
11. How can a `ThreadLocal` leak data from one HTTP request into another? How does a `ScopedValue` prevent that?
12. A Spring `@Service` has a field `private int requestCount;` that every request increments. What goes wrong, and how do you fix it?

<details>
<summary>Answers</summary>

1. A Node.js callback runs to the end on the only thread, so nothing else runs during `count++`. In Java, two threads can run `count++` at the same moment on two cores, and `++` is three steps (read, add, write): both threads can read the same old value and write the same new one.
2. No. `volatile` guarantees visibility (every read sees the latest write) and ordering, but `count++` is still a separate read and write, so two threads can still interleave them. Use an `AtomicLong`, a `LongAdder` or a lock.
3. Any three of: releasing a lock and a later taking of the same lock; writing a `volatile` field and a later read of it; `thread.start()` and the actions of the started thread; the actions of a thread and `join()` returning; submitting a task and the task running; a task finishing and `Future.get()` returning; `countDown()` and the `await()` that returns because of it.
4. `shutdown()` stops accepting new tasks but lets queued and running tasks finish, and returns at once. `shutdownNow()` also removes the queued tasks and interrupts the running ones, and returns at once. `close()` calls `shutdown()` and then waits until all tasks have finished (if it is interrupted while it waits, it calls `shutdownNow()`).
5. `get()` must be able to report any exception of the task, including checked ones, and keep it apart from problems of the waiting itself (`InterruptedException`, `TimeoutException`). Take the real exception with `getCause()`, and rethrow it or wrap it in an exception that makes sense to your caller.
6. Thread A locks `a` and waits for `b`; thread B locks `b` and waits for `a`. Neither can go on. Prevent it with a global lock order (for example, lower account id first), or with `tryLock` and a timeout so that a thread gives up and releases what it holds. Holding only one lock at a time also prevents it.
7. When the lock is often busy for a moment. `tryLock()` gives up at once, even if the lock would be free a microsecond later, so under load almost every attempt fails. `tryLock(time, unit)` waits a bounded time.
8. Both threads can read the same old value and both write old + 1, so one increment is lost. `ConcurrentHashMap` makes each single call atomic, not a sequence of calls. Use `map.merge(key, 1, Integer::sum)` or `compute`.
9. `Promise.all` rejects as soon as one input rejects. `allOf` completes only after every input has completed, and then completes exceptionally if any of them failed. So a failure next to a slow input is reported late, or only by a timeout.
10. They help when many tasks spend most of their time blocked on I/O (HTTP calls, database queries, sleeping): a blocked virtual thread doesn't occupy an operating-system thread, so you can have one per task. They don't help CPU-heavy work, where the number of cores is the limit, and they don't remove other limits, such as the size of the database connection pool.
11. A pool thread keeps running after a request has ended. A value in a `ThreadLocal` stays with the thread until somebody removes it, so the next request on that thread sees it (wrong user, wrong id), and it can't be garbage-collected. A `ScopedValue` is bound only while one call runs; when the call returns or throws, the binding is gone, without any code you could forget.
12. The service is a singleton shared by all request threads, so `requestCount++` from many threads at the same time loses updates, and other threads may not even see the latest value. Use an `AtomicLong` or `LongAdder` field (or a metrics library such as Micrometer), or keep the counter out of the bean.
</details>

## Further reading

- Brian Goetz and others, *Java Concurrency in Practice*: older than virtual threads, but still the best explanation of the ideas behind this module.
- The concurrency tutorials on dev.java: https://dev.java/learn/
- The package summary of `java.util.concurrent`, including its section "Memory Consistency Properties": https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/package-summary.html
- The Java Language Specification, chapter 17 "Threads and Locks": https://docs.oracle.com/javase/specs/jls/se25/html/jls-17.html
- JEP 444 (virtual threads), JEP 491 (synchronize virtual threads without pinning), JEP 506 (scoped values) and JEP 505 (structured concurrency, a preview): https://openjdk.org/jeps/
- Aleksey Shipilëv, "Close Encounters of The Java Memory Model Kind": https://shipilev.net/blog/2016/close-encounters-of-jmm-kind/
