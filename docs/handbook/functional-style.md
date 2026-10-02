# Functional style: streams and lambdas

Streams and lambdas make data transformations short and readable. Used everywhere, they make code harder to follow. These rules help you choose.

## Streams or a loop?

**Use a stream** to transform, filter, group or aggregate data:

```java
Map<Category, Long> revenueByCategory = orders.stream()
        .filter(order -> order.status() == Status.PAID)
        .collect(Collectors.groupingBy(Order::category, Collectors.summingLong(Order::total)));
```

**Use a loop** when:
- the logic needs `break`, `continue`, or several early returns;
- you need the index, or to look at neighboring elements;
- the body calls methods that throw checked exceptions;
- the stream version would need nested lambdas several levels deep.

A clear loop beats a clever stream. In code review, "can you make this readable?" wins over "can you make this a one-liner?".

## Rules

- **One step per line.** Line up `.filter`, `.map`, `.collect` vertically.
- **Keep lambdas short.** If a lambda grows beyond one or two lines, move it into a named method and use a method reference: `.filter(this::isOverdue)`.
- **Prefer method references when they're clearer**: `.map(Order::customer)` instead of `.map(o -> o.customer())`.
- **No side effects inside `map`, `filter` or `sorted`.** Don't add to outside lists or change objects there. Collect the result instead:
  ```java
  // Don't
  List<String> names = new ArrayList<>();
  users.stream().filter(User::isActive).forEach(u -> names.add(u.name()));

  // Do
  List<String> names = users.stream().filter(User::isActive).map(User::name).toList();
  ```
- **`peek` is for debugging only.** Its execution isn't guaranteed: a stream may skip it entirely.
- **A stream can be used only once.** Calling a second terminal operation throws `IllegalStateException`. Store the collection, not the stream.
- **Streams are lazy.** Nothing runs until a terminal operation (`toList`, `collect`, `count`, `forEach` …).
- **Prefer `stream.toList()`** (unmodifiable, Java 16+) over `collect(Collectors.toList())`.
- **Use primitive streams for numbers**: `mapToLong(Order::total).sum()` avoids boxing every value.
- **Don't use parallel streams in server code** unless you've measured a benefit. They share one global thread pool, and a web server already handles many requests in parallel.

## Lambdas and captured variables

A lambda can use local variables from the surrounding method only if they're **effectively final** (never reassigned). This is stricter than JavaScript closures. When you feel the need to change a captured counter, the code usually wants a stream operation (`count()`, `sum()`) instead.

## Optional chains

See [Null and Optional](null-and-optional.md). Short `map`/`orElse` chains read well; long `flatMap` chains usually don't.

## Know the standard functional interfaces

| Interface | Shape | Example use |
|---|---|---|
| `Function<T, R>` | `T → R` | `map` |
| `Predicate<T>` | `T → boolean` | `filter` |
| `Supplier<T>` | `() → T` | `orElseGet`, lazy defaults |
| `Consumer<T>` | `T → void` | `forEach`, `ifPresent` |
| `UnaryOperator<T>` | `T → T` | `replaceAll` |
| `BiFunction<T, U, R>` | `(T, U) → R` | `merge` |

Use these before declaring your own interface. Declare your own (with `@FunctionalInterface`) when a domain name explains more: `DiscountRule` reads better than `Function<Order, Money>`.
