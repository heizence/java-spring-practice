# J06 · Functional Java

⏱ 5 sessions + 1 optional · week 4 · 5 exercises

Modern Java passes behavior around as values: a comparator to a sort, a mapping function to a stream, an action to a retry helper. This module covers lambdas, method references, the standard functional interfaces, streams, collectors and `Optional`. You'll use them in almost every Spring service method. The module also shows where they hurt: streams that run twice, `Optional`s that are `null`, parallel streams that lose data. Much of it will feel familiar from TypeScript's array methods, but Java streams are lazy and single-use, and that changes how you write them.

## Lesson

### 1. Lambdas implement functional interfaces

Java has no function types like TypeScript's `(x: number) => number`. A lambda is a short way to write an object that implements an interface with exactly one abstract method. Such an interface is called a *functional interface*.

```java
Comparator<String> byLength = (a, b) -> Integer.compare(a.length(), b.length());
Runnable hello = () -> System.out.println("hello");
Function<String, Integer> length = s -> s.length();

int n = length.apply("lambda");   // 6. You call the interface's method: length("lambda") doesn't compile.
```

The compiler works out the parameter types from the *target type*: the interface that the lambda must become. That's why `var f = x -> x * 2;` doesn't compile. It has no target type.

| Form | Example |
|---|---|
| one parameter | `s -> s.length()` |
| several parameters | `(a, b) -> a + b` |
| no parameters | `() -> "hi"` |
| explicit parameter types | `(String s) -> s.length()` |
| a block with `return` | `s -> { log(s); return s.length(); }` |

You can declare your own functional interface. The `@FunctionalInterface` annotation asks the compiler to check that it has exactly one abstract method. Default methods don't count.

```java
@FunctionalInterface
interface TriFunction<A, B, C, R> {
    R apply(A a, B b, C c);
}
```

### 2. The standard functional interfaces

The package `java.util.function` has the shapes you need most of the time. Learn the method names, because you call them.

| Interface | Method | Shape | Example |
|---|---|---|---|
| `Function<T, R>` | `apply` | T → R | `String::length` |
| `BiFunction<T, U, R>` | `apply` | (T, U) → R | `(price, quantity) -> price * quantity` |
| `UnaryOperator<T>` | `apply` | T → T | `String::strip` |
| `BinaryOperator<T>` | `apply` | (T, T) → T | `Integer::sum` |
| `Supplier<T>` | `get` | () → T | `ArrayList::new` |
| `Consumer<T>` | `accept` | T → nothing | `System.out::println` |
| `BiConsumer<T, U>` | `accept` | (T, U) → nothing | `map::put` |
| `Predicate<T>` | `test` | T → boolean | `String::isBlank` |
| `BiPredicate<T, U>` | `test` | (T, U) → boolean | `String::startsWith` |
| `Runnable` (in `java.lang`) | `run` | () → nothing | `() -> cache.clear()` |

Generics only work with objects, so a `Function<Integer, Integer>` *boxes* every `int` into an `Integer` object. The *primitive specializations* avoid that: `IntFunction<R>`, `ToIntFunction<T>`, `IntUnaryOperator`, `IntPredicate`, `IntSupplier`, `IntBinaryOperator`, and the same for `Long` and `Double`. Their methods have names like `applyAsInt` and `getAsInt`.

None of these interfaces declares a checked exception, so a lambda used as a `Function` can't throw `IOException`. Catch it inside the lambda and throw an unchecked exception (such as `UncheckedIOException`), or declare your own functional interface whose method `throws IOException`.

### 3. Capturing variables: "effectively final"

A lambda can use the local variables of the method around it, but only if they are *effectively final*: assigned once and never changed.

```java
String prefix = "order-";
Function<Integer, String> label = id -> prefix + id;   // fine: prefix never changes

int count = 0;
Runnable increment = () -> count++;                   // compile error: count changes
```

In JavaScript, a closure captures the variable itself, so `let count = 0; const inc = () => count++;` works. Java copies the variable's *value* into the lambda. If the variable could change later, the copy would be out of date without anyone noticing, so the compiler forbids it.

When a lambda needs state that changes, keep that state in an object. The variable that holds the object stays effectively final, and the object's contents change. `AtomicInteger` is the usual holder for a counter:

```java
AtomicInteger count = new AtomicInteger();
Runnable increment = count::incrementAndGet;   // `count` itself is never reassigned
```

Fields are different: a lambda reaches them through `this`, which never changes, so it can read and change fields. And JavaScript's old loop trap, where every closure created in a `for (var i ...)` loop sees the last `i`, can't happen in Java. The compiler rejects a lambda that uses the changing `i`, so you copy it into an effectively final variable first. The variable of an enhanced `for` loop (`for (Order order : orders)`) is a new variable in each round, so lambdas may use it.

### 4. Method references

When a lambda only calls one existing method, a *method reference* says the same thing more briefly. There are four kinds:

| Kind | Example | The same as |
|---|---|---|
| Static method | `Integer::parseInt` | `s -> Integer.parseInt(s)` |
| Instance method of one particular object (*bound*) | `allowed::contains` | `w -> allowed.contains(w)` |
| Instance method of whichever object comes first (*unbound*) | `String::length` | `s -> s.length()` |
| Constructor | `ArrayList::new` | `() -> new ArrayList<>()` |

The unbound kind surprises people: its first parameter becomes the object the method runs on, and the other parameters become the method's arguments. So `String::startsWith` fits a `BiPredicate<String, String>`, meaning `(text, prefix) -> text.startsWith(prefix)`.

A bound method reference evaluates its object once, when the reference is created. If `user` is `null`, `user::name` throws `NullPointerException` immediately. The lambda `() -> user.name()` would throw only when someone calls it.

Use a method reference when it reads better, and a lambda when you need to change the arguments first.

### 5. Composing functions

The functional interfaces have default methods that build new functions from existing ones:

```java
Function<String, String> trim = String::strip;
Function<String, Integer> length = String::length;
Function<String, Integer> trimmedLength = trim.andThen(length);    // trim first, then length
Function<String, Integer> same = length.compose(trim);             // compose runs its argument first

Predicate<String> isBlank = String::isBlank;
Predicate<String> isShort = s -> s.length() < 4;
Predicate<String> keep = isBlank.or(isShort).negate();             // also and(...)
Predicate<String> notBlank = Predicate.not(String::isBlank);       // handy inside filter(...)
```

`Comparator` composes the same way, and you'll use it all the time:

```java
Comparator<Order> highestTotalFirst = Comparator.comparingLong(Order::totalCents)
        .reversed()                  // reverses everything built so far
        .thenComparing(Order::id);   // added after reversed(), so ids still go from A to Z
```

Two traps. First, `reversed()` reverses the whole chain before it, so put it right after the part you want reversed. Second, `Comparator.comparing(o -> o.total()).reversed()` doesn't compile: in a chain, the lambda's parameter type gets lost and becomes `Object`. Use a method reference (`Order::total`) or write the type: `(Order o) -> o.total()`.

### 6. Streams are lazy, single-use pipelines

A stream *pipeline* has three parts:

- a *source*: `list.stream()`, `Stream.of(...)`, `IntStream.range(0, 10)`;
- any number of *intermediate operations*, such as `filter` or `map`, each returning a new stream;
- one *terminal operation*, such as `toList`, `count` or `forEach`, that produces the result.

Intermediate operations are *lazy*: they only describe the work. Nothing runs until the terminal operation asks for elements. Then each element travels through the whole pipeline before the next one starts:

```java
Stream.of("ant", "bee", "cat")
        .filter(s -> { System.out.println("filter " + s); return true; })
        .map(s -> { System.out.println("map " + s); return s.toUpperCase(); })
        .findFirst();
// prints "filter ant" and "map ant", then stops: findFirst needs only one element
```

TypeScript's `array.filter(...).map(...)` works the other way: `filter` runs over the whole array and builds a new array, then `map` runs over that. Java's laziness makes *short-circuiting* operations (`findFirst`, `anyMatch`, `limit`) cheap, and makes endless sources like `Stream.iterate(1, n -> n * 2)` possible.

A stream can run only once. A second terminal operation throws `IllegalStateException: stream has already been operated upon or closed`. So keep the collection, not the stream, and call `stream()` again for each question. A stream also never changes its source: `sorted()` sorts the elements inside the pipeline, not your list.

### 7. The everyday operations

```java
List<String> customers = orders.stream()
        .filter(o -> o.status() == Status.PAID)   // keep the elements that match
        .map(Order::customer)                     // turn each element into something else
        .distinct()                               // drop duplicates: keeps the first one, and the order
        .sorted()                                 // natural order, or sorted(comparator)
        .limit(10)                                // at most 10 elements
        .toList();                                // an unmodifiable List

boolean anyBig = orders.stream().anyMatch(o -> o.totalCents() > 100_000);   // also allMatch, noneMatch
Optional<Order> firstNew = orders.stream().filter(o -> o.status() == Status.NEW).findFirst();
long paidCount = orders.stream().filter(o -> o.status() == Status.PAID).count();

List<String> products = orders.stream()
        .flatMap(o -> o.lines().stream())         // one order → many lines, all in one stream
        .map(OrderLine::product)
        .toList();
```

`skip(n)` drops the first `n` elements, so `skip` and `limit` together give you one page of results. `reduce` combines all elements into one value: `Stream.of(1, 2, 3).reduce(0, Integer::sum)` is 6. The function must be *associative*, meaning `(a + b) + c == a + (b + c)`, because a parallel stream may combine partial results in any grouping. For sums and counts, the specialized operations in the next section read better.

Collect into a list with `toList()` (Java 16+), which returns an unmodifiable list. The older `collect(Collectors.toList())` returns a list that callers can change, which is rarely what you want.

### 8. Primitive streams

`IntStream`, `LongStream` and `DoubleStream` hold primitive numbers without boxing, and add math operations:

```java
long revenue = orders.stream().mapToLong(Order::totalCents).sum();
OptionalDouble average = orders.stream().mapToLong(Order::totalCents).average();
double averageOrZero = average.orElse(0.0);
IntSummaryStatistics stats = words.stream().mapToInt(String::length).summaryStatistics();

IntStream.range(0, 5).forEach(i -> process(i));   // 0, 1, 2, 3, 4 (rangeClosed includes the end)
List<Integer> numbers = IntStream.range(0, 5).boxed().toList();
```

`sum()` of an empty stream is 0, but `average()` returns an `OptionalDouble`, because an empty stream has no average.

### 9. Collectors

`collect(...)` takes a `Collector`: a recipe for building a result. The class `java.util.stream.Collectors` has the common ones. The examples use static imports such as `import static java.util.stream.Collectors.groupingBy;`.

```java
Set<String> customers = orders.stream().map(Order::customer).collect(toSet());

// toMap throws IllegalStateException on a duplicate key, unless you say how to merge two values:
Map<String, Long> totals = orders.stream()
        .collect(toMap(Order::customer, Order::totalCents, Long::sum));
// A fourth argument chooses the map, here one that keeps the order of first appearance:
SequencedMap<String, Long> inOrder = orders.stream()
        .collect(toMap(Order::customer, Order::totalCents, Long::sum, LinkedHashMap::new));

String text = Stream.of("ari", "jun", "mina").collect(joining(",", "[", "]"));   // "[ari,jun,mina]"
```

`groupingBy` splits the elements into groups by a key. On its own, each group becomes a `List`. A second collector, the *downstream collector*, decides what each group becomes instead:

```java
Map<Status, List<Order>> byStatus = orders.stream().collect(groupingBy(Order::status));
Map<Status, Long> countByStatus = orders.stream().collect(groupingBy(Order::status, counting()));
Map<String, Long> spentByCustomer = orders.stream()
        .collect(groupingBy(Order::customer, summingLong(Order::totalCents)));
Map<String, List<String>> idsByCustomer = orders.stream()
        .collect(groupingBy(Order::customer, mapping(Order::id, toList())));
Map<String, Set<String>> productsByCustomer = orders.stream()
        .collect(groupingBy(Order::customer,
                flatMapping(o -> o.lines().stream().map(OrderLine::product), toSet())));

// A map factory in the middle chooses the map: sorted keys, or an EnumMap for enum keys.
SortedMap<String, Long> sortedCounts = orders.stream()
        .collect(groupingBy(Order::customer, TreeMap::new, counting()));
Map<Status, Long> enumCounts = orders.stream()
        .collect(groupingBy(Order::status, () -> new EnumMap<>(Status.class), counting()));
```

`partitioningBy(predicate)` is a `groupingBy` with the keys `true` and `false`, and both keys are always there, even when one side is empty.

`maxBy` and `minBy` give an `Optional`, because an empty stream has no maximum. Inside `groupingBy` a group is never empty, so you can unwrap it safely with `collectingAndThen(collector, finisher)`, which runs a function on the collector's result:

```java
// The latest order of each customer
Map<String, Order> latest = orders.stream()
        .collect(groupingBy(Order::customer,
                collectingAndThen(maxBy(comparing(Order::placedAt)), Optional::orElseThrow)));
```

Read nested collectors from the outside in: "group by customer; in each group, take the maximum by date; then unwrap the `Optional`".

### 10. Optional

`Optional<T>` is a box that holds one value or nothing. Use it as a return type: a method that may find nothing returns `Optional<T>` instead of `null`, and the type reminds the caller to handle the empty case.

```java
Optional<Customer> found = repository.findById(id);

String city = found
        .flatMap(Customer::address)   // for a method that itself returns an Optional
        .map(Address::city)           // transform the value, if there is one
        .filter(c -> !c.isBlank())    // empty unless the value passes the test
        .orElse("unknown");           // the value, or this default

Customer customer = found.orElseThrow(() -> new CustomerNotFoundException(id));
found.ifPresent(c -> mailer.welcome(c));
```

`orElse(x)` evaluates `x` every time, even when the value is present, because Java evaluates every argument before it calls a method. When the default is expensive or has a side effect, use `orElseGet(() -> compute())`: it calls the supplier only when the `Optional` is empty.

The rules most teams follow:

- Use `Optional` as a return type only. Not for fields, parameters or collection elements. An `Optional` field can itself be `null`, which gives you three states instead of two.
- Never return `null` from a method whose return type is `Optional`.
- Don't call `get()`: it throws `NoSuchElementException` when the `Optional` is empty. Use `orElse`, `orElseGet`, `orElseThrow(...)`, `map` or `ifPresent`. If you're sure there is a value, `orElseThrow()` says so openly.
- Create one with `Optional.ofNullable(x)` when `x` may be `null`. `Optional.of(null)` throws `NullPointerException`.
- For primitives there are `OptionalInt`, `OptionalLong` and `OptionalDouble`.

### 11. Side effects, `peek` and parallel streams

The lambdas in a pipeline should only compute from their input. They shouldn't change shared state. Collect results with a collector instead of adding them to a list from outside:

```java
List<String> labels = new ArrayList<>();
orders.stream().map(Order::label).forEach(labels::add);              // works, but fragile

List<String> labels = orders.stream().map(Order::label).toList();     // better
```

`forEach` as the last step is fine for a real side effect, such as sending each email.

`peek(action)` runs an action on each element that passes by. It exists for debugging. Don't use it for real work: a stream runs only the operations it needs. Since Java 9, `list.stream().peek(...).count()` doesn't run `peek` at all, because `count()` can get the size from the list.

`parallelStream()` splits the work over the threads of one shared pool, the *common ForkJoinPool*. That helps only for CPU-heavy work on large amounts of data. It hurts for blocking work such as database calls, because every parallel stream in the application shares those few threads. In a parallel stream, `forEach` runs in no particular order, and adding to an `ArrayList` from several threads at once loses elements or throws exceptions. `toList()` and the collectors are safe in parallel streams: each thread collects its own part, and the parts are combined at the end. `toList()` keeps the order.

A stream gets its order from its source. A `List` has an order, so its stream has one. A `HashSet` or `HashMap` has none, so streaming it gives the elements in an order you must not rely on. Use a `LinkedHashSet` or `LinkedHashMap` (insertion order), a `TreeSet` or `TreeMap` (sorted), or `sorted()`.

### 12. When a loop is better

Streams are a tool, not a rule. A plain loop is often clearer when you need to:

- return early or `break` out in the middle;
- throw checked exceptions;
- update several results in one pass;
- work with indexes, for example to compare each element with the next one;
- write an inner loop where every nanosecond counts.

```java
// Clear as a loop: stop at the first invalid line, and report which one it was.
for (int i = 0; i < lines.size(); i++) {
    if (!isValid(lines.get(i))) {
        return Optional.of(i);
    }
}
return Optional.empty();
```

A useful test: if a stream needs comments before anyone understands it, write it as a loop too, and keep the version that reads better.

### 13. Stream gatherers (Java 24+)

`stream.gather(gatherer)` plugs your own intermediate operation into a stream, the way `collect(collector)` plugs in a terminal one. The class `java.util.stream.Gatherers` has five built-in gatherers:

```java
Stream.of(1, 2, 3, 4, 5).gather(Gatherers.windowFixed(2)).toList();            // [[1, 2], [3, 4], [5]]
Stream.of(1, 2, 3, 4).gather(Gatherers.windowSliding(2)).toList();             // [[1, 2], [2, 3], [3, 4]]
Stream.of(1, 2, 3).gather(Gatherers.scan(() -> 0, Integer::sum)).toList();     // [1, 3, 6]
Stream.of(1, 2, 3).gather(Gatherers.fold(() -> "", (s, n) -> s + n)).toList(); // ["123"]
// mapConcurrent(n, function): maps on up to n virtual threads (J08) at a time and keeps the order
```

`windowSliding` emits one short window when the stream is shorter than the window. `scan` emits the running result after each element. `fold` emits one result at the end, even for an empty stream.

To write your own, use `Gatherer.ofSequential(initializer, integrator)`. The *initializer* creates fresh state for each run of a stream. The *integrator* gets the state, the next element and the *downstream* (the rest of the pipeline). It can `push` elements downstream, and it returns `false` to stop the stream early:

```java
// Takes elements while they keep increasing: 1, 3, 7, 2, 9 → 1, 3, 7
static Gatherer<Integer, ?, Integer> takeWhileIncreasing() {
    class Last {                                    // a small mutable holder for the state
        Integer value;
    }
    return Gatherer.ofSequential(
            Last::new,                              // new state for every stream
            (last, n, downstream) -> {
                if (last.value != null && n <= last.value) {
                    return false;                   // stop: we want no more elements
                }
                last.value = n;
                return downstream.push(n);          // false when later steps want no more, as after limit(3)
            });
}
```

## TypeScript → Java

| TypeScript | Java | Watch out for |
|---|---|---|
| `(x: number) => x * 2` | `x -> x * 2`, typed by an interface such as `IntUnaryOperator` | No function types: the interface decides the method name |
| `f(3)` | `f.apply(3)`, `p.test(x)`, `s.get()`, `c.accept(x)` | A lambda can't be called like `f(3)` |
| closures capture variables | lambdas capture effectively final locals only | Keep changing state in an `AtomicInteger` or a field |
| `arr.map(f).filter(g)` runs at once and builds arrays | `list.stream().map(f).filter(g).toList()` | Nothing runs before the terminal operation, and a stream runs once |
| `arr.sort(compare)` sorts the array itself | `stream.sorted(comparator)` | `sorted()` leaves the source alone; `List.sort` changes it |
| `arr.reduce((a, b) => a + b, 0)` | `reduce(0, Integer::sum)` or `mapToInt(...).sum()` | `reduce` needs an associative function |
| `arr.flatMap(o => o.lines)` | `flatMap(o -> o.lines().stream())` | `flatMap` needs a stream, not a list |
| `[...new Set(arr)]` | `distinct()` | Keeps the first occurrence and the order |
| `Object.groupBy(arr, f)` | `collect(groupingBy(f))` | A downstream collector sums or counts each group |
| `arr.some(p)` / `arr.every(p)` | `anyMatch(p)` / `allMatch(p)` | `allMatch` of an empty stream is `true`, like `every` |
| `arr.find(p)` gives `T \| undefined` | `filter(p).findFirst()` gives `Optional<T>` | |
| `a?.b?.c ?? "none"` | `optA.flatMap(A::b).map(B::c).orElse("none")` | `orElse` always evaluates its argument; use `orElseGet` |
| `arr.join(", ")` | `collect(joining(", "))` | The elements must be strings: `map(String::valueOf)` first |

## In real projects

- Prefer method references and small named methods to long lambdas. A lambda longer than about three lines usually wants to become a private method. See [Functional style](../../docs/handbook/functional-style.md).
- Return `Optional` for "may be missing", never `null`, and don't use `Optional` for fields or parameters. See [Null and Optional](../../docs/handbook/null-and-optional.md).
- Collect with `toList()` and return unmodifiable collections. See [Immutability](../../docs/handbook/immutability.md) and [Collections and generics](../../docs/handbook/collections-and-generics.md).
- Don't use parallel streams in code that handles requests. Spring already serves requests on many threads, and blocking work in the common pool slows the whole application down. See [Concurrency](../../docs/handbook/concurrency.md).
- Inside a lambda, wrap a checked exception in a meaningful unchecked one instead of hiding it. See [Exceptions](../../docs/handbook/exceptions.md).
- Keep money in `long` cents or `BigDecimal` in stream code too: `mapToLong(...).sum()`, never `mapToDouble`. See [Dates and money](../../docs/handbook/dates-and-money.md).
- Plain loops are fine. Reviewers favor whichever version reads best. See [Java style](../../docs/handbook/java-style.md).

## Exercises

### J06-01 · Lambdas and functional interfaces

**Build** · 30 min · [`Functions.java`](src/main/java/practice/j06/ex01/Functions.java) and [`TriFunction.java`](src/main/java/practice/j06/ex01/TriFunction.java)

You build a small toolbox of higher-order functions: methods that take functions or return them. Retry helpers, caches and decorators in real libraries are built from exactly these pieces, and writing them makes lambdas, closures and the functional interfaces feel natural.

**You'll practice:** lambdas for `Function`, `Supplier` and `IntSupplier`; keeping state in a closure; the effectively final rule; a default method on your own `@FunctionalInterface`; all four kinds of method reference.

**Rules:** in the Javadoc of `Functions` and `TriFunction`. Write the last four methods of `Functions` as single method references.

<details>
<summary>Hint 1</summary>

`compose` and `andThen` each return a lambda: it takes the input, applies one function, and passes the result to the other. Check the arguments *before* the `return`, so a `null` fails right away instead of inside the lambda later.
</details>

<details>
<summary>Hint 2</summary>

For `memoize`, create a `HashMap` inside `memoize` and let the returned lambda use it. `Map.computeIfAbsent(key, function)` does "look up, or compute and store" in one call. Read its Javadoc: what does it do when the function returns `null`?
</details>

<details>
<summary>Hint 3</summary>

In `counter`, `int next = start; return () -> next++;` doesn't compile. Which object from section 3 holds a number that changes? One of its methods fits `IntSupplier` as a bound method reference.
</details>

<details>
<summary>Hint 4</summary>

`retry` is a `for` loop with a `try`/`catch` inside. Which exception type should the `catch` name, so that an `Error` passes through untouched?
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Does every `null` argument fail right away, or only when the returned function runs? Can you name the kind of each of your four method references?

### J06-02 · Streams basics

**Build** · 30 min · [`OrderQueries.java`](src/main/java/practice/j06/ex02/OrderQueries.java)

Six everyday questions about a list of orders: filter by status, the biggest orders, the customers, a yes-or-no check, the revenue and the average order value. Each answer is one stream pipeline, the kind you'll write in almost every service class.

**You'll practice:** `filter`, `map`, `distinct`, `sorted` with `Comparator.comparingLong(...).reversed().thenComparing(...)`, `limit`, `anyMatch`, `mapToLong(...).sum()`, `average()` and `OptionalDouble`, `Predicate.not`, `toList()`.

**Rules:** in the Javadoc of `OrderQueries`, including which two methods leave out cancelled orders.

<details>
<summary>Hint 1</summary>

Build the comparator for `topByTotal` step by step: `Comparator.comparingLong(Order::totalCents)`, then `.reversed()`, then `.thenComparing(Order::id)`. The position of `reversed()` matters. Keeping it in a `private static final` field gives it a name.
</details>

<details>
<summary>Hint 2</summary>

`average()` on a `LongStream` returns an `OptionalDouble`. Which of its methods gives you a value for the empty case?
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Could any of your methods change the list it got, or return a list that the caller can change? Where would `.reversed()` at the end of the comparator chain have sorted the ids?

### J06-03 · Collectors

**Build** · 60 min · [`SalesReport.java`](src/main/java/practice/j06/ex03/SalesReport.java)

A sales report built from collectors: revenue per category, order counts per status, big and small orders, products per customer, spending per customer, a CSV export and the top customer of each category. Backend code is full of reports like this one, and collectors turn each of them into a single readable pipeline.

**You'll practice:** `groupingBy` with a map factory and downstream collectors (`counting`, `summingLong`, `mapping`, `flatMapping`, `toCollection`, `collectingAndThen`), `partitioningBy`, `toMap` with a merge function, `joining`, `EnumMap`, `TreeMap` and `LinkedHashMap`, and a local record.

**Rules:** in the Javadoc of `SalesReport`. Cancelled orders count only in `orderCountByStatus()`.

**How:** the Javadoc numbers the methods from step 1 to step 7, from easy to hard, and the Basic tests carry the same step numbers. Write the constructor first, then one step at a time, and run the tests after each step.

<details>
<summary>Hint 1</summary>

Write a private helper `Stream<Order> activeOrders()` that leaves out cancelled orders, and use it in every method except `orderCountByStatus()`. The lines are inside the orders: `flatMap(order -> order.lines().stream())` gives you one stream of all lines.
</details>

<details>
<summary>Hint 2</summary>

`groupingBy` has a three-argument version: the key, a map factory and a downstream collector. `TreeMap::new` sorts the keys. An `EnumMap` needs to know its enum, so its factory is `() -> new EnumMap<>(Status.class)`.
</details>

<details>
<summary>Hint 3</summary>

For step 6, the header is a line of its own. If you pass it as the prefix of `joining`, what does a report without categories return? `Stream.concat(Stream.of(header), rows)` is another way.
</details>

<details>
<summary>Hint 4</summary>

Step 7 needs two levels of grouping: by category, and inside each category the amount per customer. `collectingAndThen(groupingBy(..., summingLong(...)), spent -> ...)` gives you each category's map of customer to amount and lets you pick the winner. After `flatMap` to lines, you no longer know which customer bought a line. A small local record such as `record Purchase(String category, String customer, long cents) {}` keeps them together.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Which of your maps are sorted, and which keep the order of insertion? What would `toMap` do without the merge function?

### J06-04 · Optional and stream pitfalls

**Fix** · 30 min · [`Customer.java`](src/main/java/practice/j06/ex04/Customer.java), [`CustomerService.java`](src/main/java/practice/j06/ex04/CustomerService.java) and [`OrderStats.java`](src/main/java/practice/j06/ex04/OrderStats.java)

The shop's customer and order code compiles and looks reasonable, but it's full of bugs that people really ship. These bug reports came in this week:

1. "Customers who signed up by phone crash the order email job with a `NullPointerException`."
2. "Looking up an unknown customer gives `NoSuchElementException: No value present`. Which customer?"
3. "The pricing service gets a request for every checkout, even for customers with a contract discount."
4. "The summary of paid orders throws `IllegalStateException`."
5. "The customer list should show customers in the order of their first order. The order looks random."

Each failing test shows one symptom. Fix the code, not the tests.

**You'll practice:** `Optional.ofNullable`, `orElse` versus `orElseGet`, `orElseThrow` with your own exception, why `Optional` fields cause trouble, the single-use rule of streams, and order in hash-based collections. Two more pitfalls, `peek` and side effects in parallel streams, are in lesson section 11 and the review questions.

**Rules:** the Javadoc of each class describes the intended behavior.

<details>
<summary>Hint 1</summary>

Start with `Customer`. What value does the `email` field have after the two-argument constructor runs? And what does `Optional.of(null)` do?
</details>

<details>
<summary>Hint 2</summary>

Java evaluates every argument before it calls a method. What does that mean for `orElse(pricing.defaultDiscountPercent(...))`?
</details>

<details>
<summary>Hint 3</summary>

For the two bugs in `OrderStats`, read lesson sections 6 and 11 again. Each fix makes the code shorter.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: For each bug, can you explain in one sentence why the code looked right to the person who wrote it?

### J06-05 · Stream gatherers ★

**Build** · 30 min · [`Gathering.java`](src/main/java/practice/j06/ex05/Gathering.java)

This exercise is optional. Gatherers (Java 24+) let you add your own intermediate operations to streams. You use the built-in gatherers for batches, moving averages, running balances and a fold, then write `distinctBy`, a gatherer that the JDK doesn't have.

**You'll practice:** `Gatherers.windowFixed`, `windowSliding`, `scan` and `fold`; `Gatherer.ofSequential` with an initializer and an integrator; state that belongs to one run of a stream; stopping early through `downstream.push`.

**Rules:** in the Javadoc of `Gathering`.

<details>
<summary>Hint 1</summary>

`windowSliding` emits one short window when there are fewer values than the window size. The Javadoc of `movingAverages` says what to return in that case.
</details>

<details>
<summary>Hint 2</summary>

`Math.multiplyExact` and `Math.addExact` throw `ArithmeticException` instead of wrapping around.
</details>

<details>
<summary>Hint 3</summary>

The state of `distinctBy` is the `Set` of keys seen so far. Create it in the initializer (`HashSet::new`), not in `distinctBy` itself. A test shows why.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Could you have written `digitsToNumber` with `reduce`? What would go wrong in a parallel stream?

## Review questions

1. Why can't a lambda change a local `int` variable? What do you do instead?
2. Name the four kinds of method reference, with an example of each. Which kind is `String::startsWith` when it's used as a `BiPredicate<String, String>`?
3. When does a stream pipeline do its work? What happens when you call a second terminal operation on the same stream?
4. What's the difference between `orElse(load())` and `orElseGet(this::load)`?
5. Why shouldn't `Optional` be used for fields or parameters?
6. What does `Collectors.toMap` do with a duplicate key, and how do you handle it?
7. How is `partitioningBy(predicate)` different from `groupingBy(predicate)`?
8. Why can `peek` be skipped, and what should you use for side effects instead?
9. Why is `parallelStream().forEach(list::add)` wrong? Give two fixes.
10. When would you write a loop instead of a stream?
11. ★ What can a gatherer do that `map` and `filter` can't?

<details>
<summary>Answers</summary>

1. Java copies the values of captured local variables into the lambda, so it only allows variables that never change (effectively final). Keep changing state in an object, such as an `AtomicInteger`, or in a field.
2. Static (`Integer::parseInt`), bound to one object (`allowed::contains`), unbound (`String::length`) and constructor (`ArrayList::new`). `String::startsWith` as a `BiPredicate<String, String>` is unbound: the first argument is the text, the second the prefix.
3. Only when the terminal operation runs, element by element. A second terminal operation throws `IllegalStateException`, because a stream can run only once.
4. `orElse(load())` calls `load()` every time, before `orElse` runs, even when a value is present. `orElseGet(this::load)` calls it only when the `Optional` is empty.
5. An `Optional` field or parameter can itself be `null`, which gives three states instead of two, and callers have to wrap their values. `Optional` was designed for return types. Use a nullable field and return `Optional.ofNullable(field)` from the getter.
6. It throws `IllegalStateException`. Pass a merge function as the third argument, such as `Long::sum`, or `(first, second) -> first` to keep the first value.
7. `partitioningBy` always has both keys, `true` and `false`, even when a side is empty. `groupingBy(predicate)` only has the keys that occurred.
8. A stream only runs the operations it needs to produce the result. `count()` may get the size from the source and skip the pipeline, `peek` included. Use `forEach` (or a loop) for side effects.
9. `ArrayList` isn't thread-safe, so adding from several threads loses elements or throws, and `forEach` on a parallel stream runs in no particular order. Fixes: collect with `toList()` (safe in parallel and keeps the order), or use a sequential stream.
10. When you need to return or `break` early, throw checked exceptions, update several results at once or work with indexes. And whenever the loop reads better.
11. A gatherer can keep state between elements, emit any number of elements for each input (or none), emit something at the end, and stop the stream early. `map` and `filter` look at one element at a time and forget it.
</details>

## Further reading

- Official API docs: [`java.util.function`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/function/package-summary.html), [`java.util.stream`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/stream/package-summary.html) (read the sections on laziness, side effects and ordering), [`Collectors`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/stream/Collectors.html), [`Optional`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/Optional.html), [`Gatherers`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/stream/Gatherers.html)
- dev.java: [Lambda expressions](https://dev.java/learn/lambdas/) and [The Stream API](https://dev.java/learn/api/streams/)
- [JEP 485: Stream Gatherers](https://openjdk.org/jeps/485)
- The JLS: [15.13 Method reference expressions](https://docs.oracle.com/javase/specs/jls/se25/html/jls-15.html#jls-15.13) and [15.27 Lambda expressions](https://docs.oracle.com/javase/specs/jls/se25/html/jls-15.html#jls-15.27)
- Joshua Bloch, *Effective Java*, 3rd edition: items 42–48 (lambdas and streams) and item 55 (returning optionals)
