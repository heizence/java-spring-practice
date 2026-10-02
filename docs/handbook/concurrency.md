# Concurrency

A Spring Boot server handles many requests at the same time, each on its own thread. Every object shared between requests (every singleton bean, every cache, every static field) is shared between threads. Concurrency bugs pass tests and appear under load, so the habits matter more than the tricks.

## Thread-safety strategies, in order of preference

1. **Don't share.** Keep state in local variables and method parameters. Each thread has its own.
2. **Share only immutable objects.** They're safe without any locking (see [Immutability](immutability.md)).
3. **Use thread-safe building blocks**: `ConcurrentHashMap`, `AtomicLong`, `LongAdder`, `BlockingQueue`, executors.
4. **Lock**: `synchronized` or `ReentrantLock`, when an operation must update several things together.

Most application code stops at step 1 or 2. **Spring singleton beans should be stateless**: no fields that change per request.

## Rules

- **Don't create raw threads in application code.** Use an `ExecutorService` (in Spring, `@Async` or a `TaskExecutor`) so threads are reused, limited and shut down properly.
- **Always shut down executors you create.** Since Java 19, `ExecutorService` is `AutoCloseable`:
  ```java
  try (var executor = Executors.newFixedThreadPool(4)) {
      // submit tasks…
  } // waits for the tasks to finish, then shuts down
  ```
- **Check-then-act needs one atomic step.** `if (!map.containsKey(k)) map.put(k, v)` is a race even with a `ConcurrentHashMap`. Use `putIfAbsent`, `computeIfAbsent` or `merge`.
- **Read-modify-write needs one atomic step.** `count++` is three steps (read, add, write). Use `AtomicLong.incrementAndGet()` or `LongAdder.increment()`.
- **`volatile` gives visibility, not atomicity.** It's right for a stop flag written by one thread and read by others. It doesn't fix `count++`.
- **Keep critical sections small**, and never call slow or unknown code (I/O, callbacks, other services) while holding a lock.
- **Always acquire multiple locks in the same global order** (for example, by account id). Inconsistent order is the classic cause of deadlock.
- **Lock on a private object**, not on `this` or a public object that other code might also lock.
- **Handle `InterruptedException` properly**: either let it propagate, or restore the flag with `Thread.currentThread().interrupt()`. Never swallow it.

## Virtual threads (Java 21+)

Virtual threads are cheap threads managed by the JVM. Code that blocks on I/O (database calls, HTTP calls) can use one thread per task again, even for tens of thousands of tasks.

- Use them for **blocking I/O**, not CPU-heavy work.
- **Don't pool them.** Create one per task: `Executors.newVirtualThreadPerTaskExecutor()`.
- Spring Boot can run request handling on virtual threads with one property: `spring.threads.virtual.enabled=true` (Part 2).
- Avoid big per-thread caches in `ThreadLocal`. With many virtual threads they multiply. For passing context such as a request id, Java 25 has `ScopedValue`.

## CompletableFuture

`CompletableFuture` is Java's `Promise`: `thenApply` ≈ `then` (map), `thenCompose` ≈ `then` returning a promise, `allOf` ≈ `Promise.all`, `exceptionally` ≈ `catch`. Two differences from JavaScript:

- **Threads are real.** Callbacks may run on another thread, so shared state needs the same care as above.
- **Give async calls their own executor** for blocking work. The default common pool is small and shared by the whole JVM.

## Testing concurrent code

- Start all threads at the same moment (a `CountDownLatch` "starting gate") to make races likely.
- Repeat the test many times. One green run proves little.
- Always use timeouts, so a deadlock fails the test instead of hanging the build.
- A passing concurrency test doesn't prove the code is correct. Reasoning about which state is shared still matters most.
