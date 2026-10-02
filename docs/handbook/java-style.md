# Java style

Code is read far more often than it's written. These conventions make Java code predictable for everyone who reads it, including you in six months and the AI tools that read your codebase.

## Naming

| What | Convention | Examples |
|---|---|---|
| Classes, interfaces, records, enums | `UpperCamelCase` nouns | `OrderService`, `PaymentMethod`, `Isbn` |
| Methods | `lowerCamelCase` verbs | `placeOrder()`, `calculateTotal()` |
| Boolean methods and variables | a question: `is`, `has`, `can`, `should` | `isEmpty()`, `hasStock()`, `canTransitionTo()` |
| Variables and parameters | `lowerCamelCase` | `orderCount`, `customerId` |
| Constants (`static final`, deeply immutable) | `UPPER_SNAKE_CASE` | `MAX_LENGTH`, `DEFAULT_TIMEOUT` |
| Packages | all lowercase, no underscores, reverse domain | `com.acme.bookstore.order` |
| Type parameters | one capital letter | `T`, `E` (element), `K`/`V` (key/value), `R` (result) |

- **Spell words out.** `customer`, not `cust`. Common abbreviations like `id`, `url`, `http` are fine. Write them like words: `userId`, `HttpClient`, not `userID`, `HTTPClient`.
- **Name by role, not by type.** `activeUsers`, not `userList`.
- **Avoid vague names.** `Manager`, `Helper`, `Util`, `Data`, `Info` and `process()` say nothing. If you can't find a precise name, the class or method probably does too much.

> Packages in this repository (`practice.j05.ex09`) are short on purpose. Real projects use reverse-domain names like `com.company.product.feature`.

## Files and classes

- One top-level class per file, named like the file.
- Order members as: constants, static fields, instance fields, constructors, public methods, private helpers, nested types. Keep a private helper close to the method that uses it.
- **Make everything as private as possible.** Start with `private`. Use package-private (no modifier) for things only the same package needs. Make something `public` only when other packages must use it.
- Prefer small classes with one clear responsibility.

## Methods

- **Short and at one level of detail.** A method either coordinates steps or does one step, not both.
- **Guard clauses first.** Check invalid input and special cases at the top and return or throw early. This avoids deep nesting.

  ```java
  public Money discountFor(Order order) {
      Objects.requireNonNull(order, "order");
      if (order.items().isEmpty()) {
          return Money.ZERO;
      }
      // the main logic, not indented inside if/else
  }
  ```
- **No boolean flag parameters.** `send(message, true)` is unreadable. Write two methods, `send(message)` and `sendUrgently(message)`, or pass an enum.
- **Three or four parameters at most.** For more, group them into a record (a "parameter object").
- **Don't return `null`.** See [Null and Optional](null-and-optional.md).

## Variables

- **Declare variables where you first need them**, in the smallest scope.
- **`var`** is fine when the type is obvious from the right-hand side: `var orders = new ArrayList<Order>()`. Write the type when it isn't: `List<Order> pending = repository.findPending()`.
- **`final` fields always** (see [Immutability](immutability.md)). `final` on local variables and parameters is a team choice. Many teams skip it because short methods make reassignment obvious.
- **No magic numbers.** Give meaning a name: `if (password.length() < MIN_LENGTH)`, not `< 8`.

## Comments and Javadoc

- **Javadoc on public API**: what the method does, its rules, what it returns, what it throws. Callers read it instead of the code.
- **Comments explain *why*, never *what*.** `// LinkedHashMap with accessOrder=true keeps LRU order for us` helps. `// increment i` doesn't.
- **No commented-out code.** Git remembers it.
- A `TODO` says what's missing and why: `// TODO(J05-09): evict when full`.

## Formatting

- 4-space indentation, lines up to about 120 characters.
- Always use braces, even for one-line `if` bodies. It prevents a classic bug when someone adds a second line later.
- No wildcard imports (`import java.util.*`). Explicit imports show what a file depends on. Static imports are fine for test assertions (`assertThat`).
- Let the IDE format: in IntelliJ, **Code → Reformat Code** (⌥⌘L on macOS).

## Reviewing AI-generated code

AI assistants write plausible Java quickly. When you review it, check especially for:

- names that are generic (`data`, `result`, `handleRequest`) or that don't match the domain;
- comments that repeat the code;
- `catch (Exception e)` blocks that hide failures;
- methods that silently return `null`;
- mutable state shared without care (see [Concurrency](concurrency.md));
- tests that only cover the happy path (see [Testing](testing.md)).
