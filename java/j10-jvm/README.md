# J10 · JVM essentials

⏱ 1 session · week 6 · 1 exercise

Your Java code doesn't run on the operating system directly. It runs on the *Java Virtual Machine* (JVM), a program that loads your compiled classes, compiles the busy parts to machine code while they run, and manages memory for you. You can write a lot of Java without thinking about it. But when something goes wrong in production, such as a `StackOverflowError`, an `OutOfMemoryError`, a memory leak, or a stack trace with three "Caused by" sections, you need a working model of what the JVM does. This module gives you that model in one session.

## Lesson

### 1. From source code to running code

`javac` compiles `.java` files into `.class` files that contain *bytecode*: instructions for the JVM, not for a particular processor. The same `.class` file runs on every operating system that has a JVM. `javap -c` shows the bytecode of a class. For this method in a class `Calculator`:

```java
static int add(int a, int b) {
    return a + b;
}
```

```
$ javap -c Calculator
  static int add(int, int);
    Code:
         0: iload_0     // push the first int parameter
         1: iload_1     // push the second
         2: iadd        // add the two top values
         3: ireturn     // return the result
```

The JVM loads a class the first time your code uses it. *Class loading* finds the `.class` file on the *class path* (the list of folders and JAR files to search), checks that the bytecode is safe to run, and runs the class's static initializers. When this fails, you'll see `ClassNotFoundException` or `NoClassDefFoundError`, which usually means a JAR is missing from the class path.

At first, the JVM *interprets* bytecode: it executes one instruction at a time, which is slow. Methods that run often ("hot" methods) get compiled to machine code by the *JIT compiler* (just-in-time): first quickly, then a second time with heavy optimizations, such as copying small methods into their callers (*inlining*). That's why a Java program gets faster during its first seconds or minutes, which is called *warm-up*, and why a benchmark must warm up the code before it measures anything. V8 does the same for JavaScript.

### 2. Stack and heap

Every thread has its own *stack*. Each method call that hasn't returned yet has a *frame* on it, and the frame holds that call's parameters and local variables. A local variable of a primitive type (`int`, `long`, `double`, …) holds the value itself. A local variable of a class type holds only a *reference*: the address of an object. When the method returns, its frame disappears.

The *heap* holds every object and every array, shared by all threads. An object's fields are part of the object, so they live in the heap too, even fields of primitive types. The same goes for array elements: an `int[]` keeps its ints inside the array object. Static fields belong to the class, and HotSpot (the JVM in every common JDK) keeps them in the heap as well.

```java
static String greet(int times) {
    String name = "Mina";
    StringBuilder text = new StringBuilder();
    for (int i = 0; i < times; i++) {
        text.append(name);
    }
    return text.toString();
}
```

```
 STACK (one per thread)                       HEAP (one, shared by all threads)

 ┌─ frame of greet ────────────┐
 │ times = 3                   │
 │ name ───────────────────────┼───────────►  String "Mina"
 │ text ───────────────────────┼───────────►  StringBuilder, now holding "MinaMina"
 │ i     = 2                   │
 ├─ frame of main ─────────────┤
 │ args ───────────────────────┼───────────►  String[] with no elements
 └─────────────────────────────┘
```

References work like object references in JavaScript: assigning one variable to another copies the reference, not the object. (J01-02 showed what that means for method arguments.)

Two more facts:

- The JIT may skip a heap allocation for an object that never leaves its method (this is called *escape analysis*). Your program can't tell the difference, so think of every object as living in the heap.
- A stack has a fixed size, typically 1 or 2 MB depending on the platform. The option `-Xss` changes it. A heap has a maximum size; by default it is a quarter of the computer's memory, and `-Xmx` changes it.

### 3. Garbage collection

Java has no `free` or `delete`. The *garbage collector* (GC) finds objects that your program can no longer reach and reuses their memory.

An object is *reachable* if there's a chain of references to it from a *GC root*. GC roots are mainly the local variables in the frames of running threads and the static fields of loaded classes. An object that no chain reaches is *eligible for garbage collection*. When the GC actually reclaims it is up to the GC.

```java
Order order = new Order(1);
order = new Order(2);   // Order 1 is now unreachable: eligible for collection
```

The GC follows references from the roots; it doesn't count how many references point to an object. So a group of objects that refer to each other, but that no root reaches, is garbage too. JavaScript engines work the same way.

Most objects die young: a temporary string, an iterator, a request's data. So the heap is split into *generations*. New objects go into the *young generation*, which the GC collects often. Collecting it is cheap, because the GC only has to copy the few objects that are still alive. Objects that survive several collections move to the *old generation*, which the GC collects less often. Some collections stop all application threads for a moment: a *pause*. The default collector, G1, balances pause times and throughput. ZGC keeps pauses below a millisecond even for huge heaps. Serial is the simplest and suits tiny heaps.

A *memory leak* in Java is an object that your program no longer needs but still references, so the GC must keep it. The usual suspects:

- a static collection or cache that only grows;
- listeners or callbacks that are registered and never removed;
- inner-class objects and lambdas that hold a reference to their outer object;
- `ThreadLocal` values in pool threads that are never cleaned up;
- maps keyed by request or session that nobody prunes.

`System.gc()` only asks for a collection, and you shouldn't call it in application code. Files, sockets and database connections aren't memory: the GC doesn't close them, so close them yourself with try-with-resources (J03).

### 4. StackOverflowError and OutOfMemoryError

```
Throwable
├── Exception                  problems a program can handle (J03)
└── Error                      serious problems: don't catch them
    └── VirtualMachineError
        ├── StackOverflowError
        └── OutOfMemoryError
```

A **`StackOverflowError`** means a thread's stack is full: the chain of calls that haven't returned yet is too long. Typical causes:

- a recursive method whose base case is never reached;
- a recursive method on input that is too deep: a long chain of nodes, a deeply nested JSON document;
- two objects whose `toString`, `equals` or `hashCode` call each other. This happens often with two-way relationships between JPA entities, especially with Lombok's generated methods.

Fix the cause: a loop instead of recursion, a correct base case, or a broken cycle. A bigger `-Xss` usually only moves the problem.

An **`OutOfMemoryError: Java heap space`** means the reachable objects don't fit into the maximum heap. Either the heap is too small for the work, or something keeps objects reachable that should be garbage: a leak. Other variants name a different resource: `Metaspace` (class definitions), `unable to create native thread`, `Requested array size exceeds VM limit`.

Don't catch either error and continue as if nothing happened. After an `OutOfMemoryError`, any line of code can fail, even the one that writes the log, and your objects may be half updated. Let the process stop, restart it, and analyze the cause.

Memory per value matters when you hold millions of them. An `int` takes 4 bytes. An `Integer` is an object: a header of 12 bytes plus the 4-byte value, which makes 16 bytes, plus a 4-byte reference to the object wherever it is stored. A `List<Integer>` therefore needs about five times as much memory as an `int[]` with the same numbers. (These sizes apply to the default settings of a 64-bit JVM with a heap below 32 GB.)

### 5. JVM options you'll meet

| Option | What it does | Example |
|---|---|---|
| `-Xmx` | maximum heap size | `-Xmx2g` |
| `-Xms` | initial heap size; servers often set it equal to `-Xmx` | `-Xms2g` |
| `-Xss` | stack size of each thread | `-Xss1m` |
| `-XX:MaxRAMPercentage` | maximum heap as a share of the memory, or of the container's memory limit | `-XX:MaxRAMPercentage=75` |
| `-XX:+HeapDumpOnOutOfMemoryError` | writes a heap dump when an `OutOfMemoryError` happens | with `-XX:HeapDumpPath=/var/dumps` |
| `-Xlog:gc` | logs every garbage collection | `-Xlog:gc:file=gc.log` |
| `-XX:+UseG1GC`, `-XX:+UseZGC`, `-XX:+UseSerialGC` | chooses the garbage collector | |

Where options go:

- On the command line: `java -Xmx512m -jar shop.jar`.
- In the environment variable `JAVA_TOOL_OPTIONS`, which every JVM on the machine reads and announces with "Picked up JAVA_TOOL_OPTIONS".
- In Gradle, `org.gradle.jvmargs` in `gradle.properties` configures Gradle's *own* JVM (this repository gives it `-Xmx1g`). Tests get their options from the test task, for example `tasks.test { maxHeapSize = "1g" }`.
- In containers, prefer `-XX:MaxRAMPercentage` to a fixed `-Xmx`, so the heap follows the container's memory limit.

### 6. Reading stack traces

A stack trace shows the chain of calls that was running when an exception was created. Here is a real one, from code that wraps a low-level exception in its own:

```
java.lang.IllegalStateException: Could not load config/shop.properties
    at com.example.shop.ConfigLoader.load(ConfigLoader.java:22)
    at com.example.shop.ShopApplication.start(ShopApplication.java:14)
    at com.example.shop.ShopApplication.main(ShopApplication.java:9)
Caused by: java.nio.file.NoSuchFileException: config/shop.properties
    at java.base/sun.nio.fs.UnixException.translateToIOException(UnixException.java:92)
    at java.base/sun.nio.fs.UnixException.rethrowAsIOException(UnixException.java:106)
    at java.base/sun.nio.fs.UnixException.rethrowAsIOException(UnixException.java:111)
    at java.base/sun.nio.fs.UnixFileSystemProvider.newFileChannel(UnixFileSystemProvider.java:213)
    at java.base/sun.nio.fs.UnixFileSystemProvider.newByteChannel(UnixFileSystemProvider.java:244)
    at java.base/java.nio.file.Files.newByteChannel(Files.java:357)
    at java.base/java.nio.file.Files.newByteChannel(Files.java:399)
    at java.base/java.nio.file.spi.FileSystemProvider.newInputStream(FileSystemProvider.java:371)
    at java.base/java.nio.file.Files.newInputStream(Files.java:154)
    at java.base/java.nio.file.Files.newBufferedReader(Files.java:2645)
    at java.base/java.nio.file.Files.readAllLines(Files.java:3085)
    at java.base/java.nio.file.Files.readAllLines(Files.java:3122)
    at com.example.shop.ConfigLoader.load(ConfigLoader.java:20)
    ... 2 more
```

How to read it:

- **The first line** names the exception class and its message.
- **Each `at` line is a frame**, written as `package.Class.method(File.java:line)`. The top frame is where the exception was created; each line below it is the caller of the line above; the bottom is where the thread started. `java.base/` marks code from the JDK. A name like `lambda$load$0` is a lambda inside the method `load`, and `Outer$Inner` is a nested class.
- **`Caused by:`** is an exception that was passed as the *cause* when the exception above it was created: `throw new IllegalStateException("Could not load " + file, e)`. The last `Caused by` is the *root cause*, the original problem.
- **`... 2 more`** means the last 2 frames are the same as in the trace above (here `start` and `main`), so they aren't printed again.
- **`Suppressed:`** sections hold exceptions thrown by `close()` in a try-with-resources block, attached to the main exception.

A good routine: go to the last `Caused by`, skip the frames that aren't yours (`java.base/`, frameworks, libraries), and open the first frame from your own package. Here that is `ConfigLoader.java:20`, the line that read the file. Then read upwards through your own frames to see how the program got there.

Since Java 15, a `NullPointerException` explains itself: `Cannot invoke "String.trim()" because "code" is null`. It names local variables when the class was compiled with debug information, which Gradle and Maven do by default.

When you wrap an exception, always pass the original as the cause. `throw new IllegalStateException("Could not load " + file)` without `e` deletes the root cause from every log.

### 7. Tools, briefly

All of these come with the JDK or are free:

- `jps` lists the running JVMs and their process ids.
- `jcmd <pid> Thread.print` prints the stack of every thread, like the older `jstack <pid>`. Use it when an application hangs: look for threads in the `BLOCKED` state and for a "Found one Java-level deadlock" section.
- `jcmd <pid> GC.heap_info` shows how full the heap is, and `jcmd <pid> GC.class_histogram` shows which classes use the most memory.
- `jcmd <pid> GC.heap_dump /tmp/shop.hprof` writes a *heap dump*: every object in the heap. VisualVM or Eclipse MAT can open it and show what keeps a large object reachable. A heap dump contains everything in memory, including passwords and personal data, so treat it as a secret.
- `jcmd <pid> VM.flags` shows the JVM options in effect.
- *JDK Flight Recorder* (JFR) records what the JVM does (allocations, garbage collections, locks, hot methods) with very little overhead, so you can use it in production: `jcmd <pid> JFR.start duration=60s filename=shop.jfr`. Open the file in JDK Mission Control.
- *VisualVM* is a desktop application that attaches to a running JVM and shows memory, threads and CPU use.

In Spring Boot, the Actuator endpoints expose much of the same information over HTTP (S14).

## TypeScript → Java

| TypeScript / Node.js | Java / JVM | Watch out for |
|---|---|---|
| V8 compiles hot JavaScript with a JIT | the JVM interprets bytecode, then JIT-compiles hot methods | Java code warms up: measure after warm-up |
| `RangeError: Maximum call stack size exceeded` | `StackOverflowError` | it's an `Error`: fix the cause, don't catch it |
| `FATAL ERROR: … JavaScript heap out of memory` | `OutOfMemoryError: Java heap space` | usually a leak, or `-Xmx` too small for the work |
| `node --max-old-space-size=4096` | `java -Xmx4g` | the default is a quarter of the memory |
| `node --stack-size=…` | `java -Xss1m` | it applies to every thread, and Java uses many threads |
| one main thread with one call stack | many threads, each with its own stack | objects in the heap are shared between threads (J08) |
| `new Error("…", { cause })`, `err.cause` | `new IllegalStateException("…", cause)`, `getCause()` | read the last `Caused by` first |
| `node --inspect` and a heap snapshot in Chrome DevTools | `jcmd <pid> GC.heap_dump`, then VisualVM or Eclipse MAT | heap dumps contain secrets |
| `process.memoryUsage()` | `jcmd <pid> GC.heap_info`, JFR, Spring Boot Actuator | |
| `WeakRef`, `FinalizationRegistry` | `WeakReference`, `Cleaner` | rarely needed in application code |

## In real projects

- Don't catch `Error` or `Throwable` to keep a process running, and always pass the cause when you wrap an exception. See [Exceptions](../../docs/handbook/exceptions.md).
- Give every cache and every long-lived map a size limit or a cleanup rule (the LRU cache of J05-09, Caffeine in S10). For millions of numbers, prefer `int[]` or `long[]` to a list of boxed values. See [Collections and generics](../../docs/handbook/collections-and-generics.md).
- Leave back-references out of `toString`, `equals` and `hashCode` in two-way relationships, and make nested classes `static` unless they need the outer object. See [Java style](../../docs/handbook/java-style.md).
- When an application hangs, take a thread dump (`jcmd <pid> Thread.print`) before you restart it, and clean up `ThreadLocal` values in thread pools. See [Concurrency](../../docs/handbook/concurrency.md).
- Behavior that depends on JVM options belongs in a test that starts its own JVM, as J10-01's tests do. Never rely on `System.gc()` in a test. See [Testing](../../docs/handbook/testing.md).
- In production, set the maximum heap explicitly (`-Xmx`, or `-XX:MaxRAMPercentage` in containers) and add `-XX:+HeapDumpOnOutOfMemoryError`, so the next crash leaves evidence behind. See the [`java` command reference](https://docs.oracle.com/en/java/javase/25/docs/specs/man/java.html).

## Exercises

### J10-01 · Predict: stack, heap and errors

**Predict** · 30 min · [`Puzzles.java`](src/main/java/practice/j10/ex01/Puzzles.java) → answers in [`Answers.java`](src/main/java/practice/j10/ex01/Answers.java)

Twenty-two puzzles about what the JVM does with your code: where values live, which objects the garbage collector may reclaim, how six small programs end when their stack or heap is small, and where to look in three stack traces. Each one is a situation you'll meet when you read a production incident.

**How:** read each group in `Puzzles` without running anything, and write your predictions in `Answers`. For group 3, read the programs in [`programs/`](src/main/java/practice/j10/ex01/programs) and note the JVM options in `Puzzles`. Then run `PuzzlesTest`. The group 3 tests start each program in a new JVM with those options (see `ChildJvm`), which takes about a second in total. No code can measure the other answers, so those tests compare your predictions with `Expected.java` in the test folder. Don't open it first.

<details>
<summary>Explanations (open after you've run the tests)</summary>

**Stack or heap**

- **The parameter `quantity`: STACK.** Parameters are local variables of the method. They live in its frame and disappear when it returns.
- **The local variable `unitPrice`: STACK.** A primitive local variable holds the number itself, in the frame.
- **The variable `order`: STACK.** It's a local variable, so it lives in the frame, but it holds only a reference to the object.
- **The `Order` object: HEAP.** Every object created with `new` lives in the heap.
- **The field `id` inside the object: HEAP.** "Primitives live on the stack" is a myth. Only primitive *local variables* do. A field is part of its object, so it lives wherever the object lives.
- **The numbers in the array: HEAP.** An array is an object, and an `int[]` stores its ints inside that object.
- **The static field `Order.created`: HEAP.** It belongs to the class, not to one call or one thread. HotSpot keeps static fields in the `Class` object, which lives in the heap.

**Garbage collection**

- **`reassigned()`: eligible.** After `order = new Order(2, …)`, no variable and no object refers to Order 1.
- **`secondReference()`: not eligible.** `sameOrder` still refers to it, and the method uses it afterwards. Setting one reference to `null` doesn't free an object.
- **`cycle()`: eligible.** The two nodes refer to each other, but no root reaches either of them. The GC follows references from the roots instead of counting them, so cycles are collected.
- **`staticList()`: not eligible.** The static field `RECENT_ORDERS` is a root as long as the class is loaded, and its list refers to the order. Most Java memory leaks look like this.
- **`innerObject()`: not eligible.** `Line` is an inner class, so every `Line` holds a hidden reference to its `Report` (`Report.this`), and `describe()` uses it. As long as `line` is in use, the report stays reachable. A `static` nested class wouldn't have this hidden reference.

**How programs end**

- **`DeepRecursion`: `STACK_OVERFLOW_ERROR`.** The base case exists, but the recursion would need a million frames at the same time, and a 512 KB stack holds a few thousand. A loop, or the formula n × (n + 1) / 2, needs no extra frames at all. The printed trace stops after 1,024 frames, because that's all the JVM prints by default.
- **`CyclicToString`: `STACK_OVERFLOW_ERROR`.** `book.toString()` prints its author, the author's `toString()` prints its books, which print their author again, and so on. There's no visible recursion in the code, which makes this one hard to spot in real projects.
- **`GarbageInALoop`: `COMPLETES`.** The program allocates 1,000 MB in total, more than 60 times the heap. But each report becomes unreachable at the end of its loop iteration, so the GC reclaims it. What counts is how much memory is *reachable at the same time*.
- **`GrowingCache`: `OUT_OF_MEMORY_ERROR`.** The same allocations, but the static list keeps every report reachable. After about 15 reports, the heap is full of live objects, and no garbage collector can help. A cache needs a size limit.
- **`PrimitiveArray`: `COMPLETES`.** 1,000,000 ints of 4 bytes each make one array of 4 MB.
- **`BoxedList`: `OUT_OF_MEMORY_ERROR`.** The same million numbers as `Integer` objects need 16 bytes each, plus 4 bytes for each reference in the list's internal array, plus spare room in that array: more than 20 MB. (`Integer.valueOf` reuses objects only for -128 to 127.)

**Stack traces**

- **Where was the `NullPointerException` thrown? `"CouponService.java:31"`.** The top frame is where the exception was created. The message says what was `null`: the variable `code`.
- **Where did the null come from? `"CheckoutService.java:57"`.** `code` is a parameter of `normalize`, so the caller passed `null`. The caller is the next frame down: `applyCoupon`, at line 57. The line that throws is often not the line with the bug.
- **Which line of your code do you open first? `"GiftService.java:22"`.** The two top frames are JDK code (`java.base/...`): `add` on an unmodifiable list, such as one from `List.of` or `List.copyOf`, always throws. The first frame from `com.example.shop` is the line that called `add`.
- **Where is the root cause? `"CsvOrderParser.java:61"`.** Start at the last `Caused by`: `Long.parseLong` can't read `"12,000"` because of the comma. Its first frame in your code is line 61. Line 63 only wrapped that exception, and line 48 wrapped it again. `... 3 more` means the last three frames are the same as in the section above.

</details>

## Review questions

1. What lives in a stack frame, and what lives in the heap?
2. When is an object eligible for garbage collection? Why can the GC collect two objects that refer to each other?
3. Name three ways a Java program can leak memory, even though it has a garbage collector.
4. A program ends with `StackOverflowError`. What are the usual causes, and why is a bigger `-Xss` rarely the fix?
5. What's the difference between "allocates a lot of memory" and "runs out of memory"?
6. How do you find the root cause in a stack trace with several `Caused by` sections? What does `... 3 more` mean?
7. Why shouldn't you catch `OutOfMemoryError` and continue?

<details>
<summary>Answers</summary>

1. A frame holds one method call's parameters and local variables: primitive values directly, objects only as references. The heap holds every object and array, including their fields and elements, and the static fields of classes.
2. When no chain of references from a GC root (a local variable of a running method, a static field) reaches it. The GC follows references from the roots instead of counting them, so a cycle that no root reaches is garbage.
3. For example: a static collection or cache that only grows; listeners that are registered but never removed; an inner-class object or a lambda that keeps its outer object alive; `ThreadLocal` values left in pool threads.
4. Recursion whose base case is never reached, recursion over input that is too deep, or two objects whose `toString`, `equals` or `hashCode` call each other. A bigger stack only allows a deeper chain of calls: infinite recursion still fails, and deep input can always be deeper. Fix the recursion instead.
5. Allocating is cheap, and the GC reclaims whatever has become unreachable, so a program can allocate many times its heap size. It runs out of memory only when the objects that are *reachable at the same time* don't fit into the heap.
6. Go to the last `Caused by`, then to its first frame in your own package. `... 3 more` means that the last three frames are identical to those of the section above and weren't printed again.
7. After an `OutOfMemoryError`, any allocation can fail, including the ones in your error handling and logging, and objects may be left half updated. A restarted process is in a known state; a process that continues after the error isn't.

</details>

## Further reading

- Official docs: [Java 25 API](https://docs.oracle.com/en/java/javase/25/docs/api/) · [dev.java/learn](https://dev.java/learn/) · [The Java Virtual Machine Specification, Java SE 25](https://docs.oracle.com/javase/specs/jvms/se25/html/index.html) (chapter 2 describes the run-time data areas)
- [The `java` command](https://docs.oracle.com/en/java/javase/25/docs/specs/man/java.html): every JVM option · [The `jcmd` command](https://docs.oracle.com/en/java/javase/25/docs/specs/man/jcmd.html)
- [Garbage Collection Tuning Guide](https://docs.oracle.com/en/java/javase/25/gctuning/) · [Troubleshooting Guide](https://docs.oracle.com/en/java/javase/25/troubleshoot/)
- dev.java: [Garbage collection](https://dev.java/learn/jvm/tool/garbage-collection/intro/) · [jcmd](https://dev.java/learn/jvm/monitoring-troubleshooting/troubleshooting/jcmd/) · [jstack](https://dev.java/learn/jvm/monitoring-troubleshooting/troubleshooting/jstack/) · [JDK Flight Recorder](https://dev.java/learn/jvm/monitoring-troubleshooting/jfr/intro/) · [javap](https://dev.java/learn/jvm/core-tools/javap/)
- [VisualVM](https://visualvm.github.io/) · [JEP 358: Helpful NullPointerExceptions](https://openjdk.org/jeps/358)
