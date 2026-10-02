# Null and Optional

Java has no `?.` or `??` operators, and the compiler doesn't track which values can be `null` (unlike TypeScript with `strictNullChecks`). `NullPointerException` is still one of the most common production errors. These rules keep `null` in check.

## The rules

1. **Never return `null` from a public method.**
   - For "no items", return an empty collection: `List.of()`, `Map.of()`.
   - For "no value", return `Optional<T>`.
2. **Use `Optional` only as a return type.** Not for fields, method parameters, or collection elements. It was designed to say "this method may have no result", nothing more.
3. **Reject `null` arguments at the boundary**, so problems show up where they start:
   ```java
   public Order(Customer customer, List<OrderLine> lines) {
       this.customer = Objects.requireNonNull(customer, "customer");
       this.lines = List.copyOf(lines); // also throws on null
   }
   ```
4. **Inside your own code, assume non-null** unless something is documented as nullable. Don't scatter defensive `if (x != null)` checks everywhere: they hide bugs instead of exposing them.

## Using Optional well

```java
Optional<Customer> customer = repository.findById(id);

// Transform and fall back: like TypeScript's ?. and ??
String name = customer.map(Customer::name).orElse("guest");

// Absence is an error here: say so with a meaningful exception
Customer found = customer.orElseThrow(() -> new CustomerNotFoundException(id));

// Do something only if present
customer.ifPresent(mailer::sendWelcome);
```

| Don't | Do instead |
|---|---|
| `optional.get()` | `orElseThrow(...)`, `orElse(...)`, `map(...)`, `ifPresent(...)` |
| `if (optional.isPresent()) { x = optional.get(); }` | `optional.map(...)` or `optional.ifPresent(...)` |
| `orElse(loadDefault())` when `loadDefault()` is expensive or has side effects | `orElseGet(this::loadDefault)`: runs only when the value is absent |
| `Optional<List<Order>>` | an empty `List<Order>` |
| return `null` from a method declared to return `Optional` | `Optional.empty()` |
| `Optional.of(value)` when `value` may be null | `Optional.ofNullable(value)` |

## Comparing safely

- `Objects.equals(a, b)` is `true` when both are `null`, and never throws.
- `"ACTIVE".equals(status)` doesn't throw when `status` is `null`. `status.equals("ACTIVE")` does.
- For enums, use `==`: `status == Status.ACTIVE` is null-safe and clear.

## Null annotations (preview of Part 2)

Spring Framework 7 marks its whole API with [JSpecify](https://jspecify.dev/) annotations. `@NullMarked` on a package means "nothing here is null unless marked", and `@Nullable` marks the exceptions. IntelliJ and tools like NullAway use these to warn about possible `NullPointerException`s while you type. It brings Java close to TypeScript's `strictNullChecks`. Many new codebases adopt it.

## TypeScript → Java

| TypeScript | Java |
|---|---|
| `user?.name` | `user.map(User::name)` (when `user` is an `Optional<User>`) |
| `name ?? "guest"` | `.orElse("guest")` |
| `if (!user) throw new Error(...)` | `.orElseThrow(() -> new ...)` |
| `string \| undefined` return type | `Optional<String>` return type |
| non-null assertion `user!` | avoid. If you're sure, `orElseThrow()` documents the assumption |
