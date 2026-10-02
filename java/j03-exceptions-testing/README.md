# J03 · Exceptions and unit testing

⏱ 4 sessions · week 2 · 4 exercises

Every backend request can fail: a file is missing, a product is sold out, the mail server is down. This module shows how Java reports failures with exceptions, how to design exceptions that a REST API can turn into clear error responses, and how to prove with tests that your code behaves well, also when things go wrong. You'll use JUnit 6, AssertJ and Mockito, the standard test tools in Spring projects. Read lesson sections 1–7 before J03-01 and J03-02, and sections 8–13 before J03-03 and J03-04.

## Lesson

### 1. The exception hierarchy

An *exception* is an object that describes a failure. `throw` sends it up the call stack until a `catch` block handles it. If nothing catches it, the thread ends and the JVM prints the *stack trace*: the exception's message and the chain of method calls that led to it.

Every exception class extends `Throwable`:

```
Throwable
├── Error                          serious JVM problems: don't catch these
│   ├── OutOfMemoryError
│   └── StackOverflowError
└── Exception                      checked: the compiler makes callers handle them
    ├── IOException                (NoSuchFileException, MalformedInputException, …)
    ├── InterruptedException
    └── RuntimeException           unchecked: no compiler checks
        ├── IllegalArgumentException
        │   └── NumberFormatException
        ├── IllegalStateException
        ├── NullPointerException
        ├── UnsupportedOperationException
        ├── ArithmeticException
        ├── IndexOutOfBoundsException
        └── NoSuchElementException
```

A few JDK exceptions cover most failures in your own code:

| Exception | Meaning | Example |
|---|---|---|
| `IllegalArgumentException` | The caller passed a bad value. | `new LruCache<>(0)` |
| `NullPointerException` | A required value was `null`. `Objects.requireNonNull(x, "x")` throws it with a clear message. | `loader.load(null)` |
| `IllegalStateException` | The object can't do this in its current state. | shipping an order that was cancelled |
| `UnsupportedOperationException` | The object never supports this operation. | `List.of(1).add(2)` |

### 2. Checked and unchecked exceptions

Java splits exceptions into two groups. TypeScript has nothing like this.

*Checked* exceptions extend `Exception` but not `RuntimeException`. The compiler checks that your method either catches each one or declares it with `throws`:

```java
String readGreeting(Path file) throws IOException { // declared: every caller must catch or declare it, too
    return Files.readString(file);
}
```

*Unchecked* exceptions extend `RuntimeException` (or `Error`). You may catch or declare them, but the compiler doesn't make you.

Which kind should your code throw? Ask whether the caller can do something useful about the failure.

| Use | When | Examples |
|---|---|---|
| Unchecked | A bug in the calling code. The caller can't recover; the code must be fixed. | `IllegalArgumentException`, `NullPointerException` |
| Unchecked | A broken business rule. One central handler turns it into an error response; the code in between can't fix it. | `OutOfStockException` (J03-02) |
| Checked | A failure at the edge of the program that the caller can handle: retry, use a default, report it. Mostly I/O. | `IOException`, `InterruptedException` |

Modern Java code and Spring prefer unchecked exceptions. A checked exception spreads into the signature of every method between the place that throws it and the place that handles it. It also doesn't work with lambdas: the functional interfaces in `java.util.function` declare no exceptions, so the lambda you pass to `stream().map(...)` can't throw `IOException`. For the same reason, Spring wraps `SQLException` in its unchecked `DataAccessException`, and the JDK has `UncheckedIOException` for I/O errors inside streams.

The rule for this course: keep checked exceptions at the boundary where you call the I/O API, and translate them there (section 5).

### 3. try, catch and finally

```java
static int parsePort(String text) {
    try {
        return Integer.parseInt(text.strip());
    } catch (NumberFormatException e) {
        throw new IllegalArgumentException("port must be a number, but was '" + text + "'", e);
    }
}
```

- Java checks the `catch` blocks from top to bottom and runs the first one that matches. Put specific types before general ones; the compiler rejects a `catch` that can never run.
- One block can catch several types: `catch (NumberFormatException | NoSuchElementException e)`.
- A `finally { ... }` block after the `catch` blocks always runs: after a `return`, after a caught exception, and after an uncaught one. Use it for cleanup only. A `return` or `throw` inside `finally` silently replaces the result or the exception of the `try` block.
- In TypeScript, `catch (e)` catches everything and `e` has the type `unknown`. In Java, a `catch` handles only the types it names. Everything else passes through to the caller.

### 4. try-with-resources

Files, network connections and database connections are *resources*: they hold something from the operating system that you must give back by calling `close()`. A resource is any object that implements `AutoCloseable`, and try-with-resources closes it for you:

```java
try (BufferedReader reader = Files.newBufferedReader(path)) {
    return reader.readLine();
}   // reader.close() runs here: after the return, and also when readLine() throws
```

- Several resources: `try (var in = ...; var out = ...)`. They're closed in reverse order.
- A variable that already exists works, too, if it's `final` or never reassigned: `try (reader) { ... }`.
- If the body throws and then `close()` throws as well, one exception would have to be lost. Java keeps the body's exception, because it explains what went wrong, and attaches the one from `close()` to it as *suppressed*. `e.getSuppressed()` returns it, and the stack trace lists it under `Suppressed:`.

Compare it with closing by hand, the way Java code looked before Java 7:

```java
BufferedReader reader = Files.newBufferedReader(path);
try {
    return reader.readLine();
} finally {
    reader.close(); // if this throws, its exception REPLACES the one from readLine()
}
```

Use try-with-resources for everything that is `AutoCloseable`, including the streams that `Files.lines()` returns. TypeScript 5.2 added a similar feature: `using` with `Symbol.dispose`.

Who closes a resource? Usually the code that opened it. When a method receives a resource that is already open, its Javadoc must say whether the method closes it. `ConfigLoader.load(Reader)` in J03-01 *takes ownership*: its Javadoc says that it always closes the reader.

### 5. Chaining: keep the cause

When you catch a low-level exception and throw a higher-level one, pass the original on as the *cause*:

```java
try {
    return Files.readString(path);
} catch (IOException e) {
    throw new ConfigException("could not read the settings file " + path, e); // e becomes the cause
}
```

The stack trace then shows both what the code was trying to do and what went wrong underneath:

```
practice.j03.ex01.ConfigException: could not read the settings file /etc/shop/app.conf
    at practice.j03.ex01.ConfigLoader.loadFile(ConfigLoader.java:75)
    ...
Caused by: java.nio.file.NoSuchFileException: /etc/shop/app.conf
    ...
```

This is called *exception translation*. Each layer reports failures in its own terms (a repository doesn't let `SQLException` leak into the web layer), and nothing gets lost: code can still read the original with `getCause()`. Without the cause, the real problem disappears, and someone has to guess. JavaScript has the same idea: `new Error("message", { cause: e })`.

To let your own exception take a cause, give it a constructor that passes it on: `super(message, cause)`.

### 6. Designing your own exceptions

JDK exceptions describe technical problems. For your business rules, define your own:

```java
public enum ErrorCode {
    OUT_OF_STOCK("ORDER-001", 409, "Not enough stock for {sku}: requested {requested}, available {available}"),
    PRODUCT_NOT_FOUND("ORDER-003", 404, "No product with SKU {sku}");
    // a code, an HTTP status and a message template per error
}

public abstract class BusinessException extends RuntimeException {
    private final ErrorCode code;
    private final Map<String, Object> details;
    // ...
}

public class OutOfStockException extends BusinessException {
    public OutOfStockException(String sku, int requested, int available) { /* ... */ }
}
```

Guidelines:

- Extend `RuntimeException`. Callers that can't fix the problem don't have to mention it.
- Use one base class with an *error code*, such as `ORDER-001`. Messages change; codes stay stable, so clients and log searches can rely on them. The code also decides the HTTP status.
- Keep the data in fields, not only in the message. A client app can show "only 5 left" from the `available` detail without parsing an English sentence.
- Put the bad values into the message ("requested 6, available 5"): it saves a debugging session. Leave out secrets such as passwords, tokens and personal data, because messages end up in logs.
- Give each subclass a constructor that takes exactly the data it needs. Then nobody can create the error with details missing.
- Make exceptions immutable: `final` fields and an unmodifiable copy of any map.
- Don't create a class for every situation. Add a subclass when callers need to react differently: a different status, or a different `catch`.

In a Spring application, one handler turns every business exception into a response. You'll write it in S04. It looks like this:

```java
@RestControllerAdvice
class ApiErrorHandler {

    @ExceptionHandler(BusinessException.class)
    ProblemDetail handle(BusinessException e) {
        var status = HttpStatusCode.valueOf(e.code().httpStatus());
        var problem = ProblemDetail.forStatusAndDetail(status, e.getMessage());
        problem.setProperty("code", e.code().code());
        return problem;
    }
}
```

J03-02 builds the same idea without Spring: `ErrorResponse.from(exception)`.

### 7. Anti-patterns

| Don't | Why | Do instead |
|---|---|---|
| `catch (Exception e) { }` (*swallowing*) | The failure disappears. The program continues with wrong data, and nobody can find out why. | Handle it, or let it propagate. If ignoring it really is correct, write a comment that explains why. |
| `catch (Exception e)` or `catch (Throwable e)` in business code | It also catches bugs (`NullPointerException`) and JVM errors (`OutOfMemoryError`) that you can't handle there. | Catch the specific types you can handle. Leave the rest to one central handler. |
| Exceptions as control flow: `try { return map.get(key).name(); } catch (NullPointerException e) { ... }` | Slow, hard to read, and it hides real bugs. | Check the condition: `if`, `containsKey`, `Optional`. |
| `throw new ConfigException("failed")` inside `catch (IOException e)` | The cause is lost. | `throw new ConfigException("failed", e)` |
| `log.error("failed", e); throw e;` | The same failure appears in the log once per layer, and nobody knows which entry matters. | Log *or* rethrow. The code that finally handles the exception logs it. |
| `e.printStackTrace()` | It writes to the console and bypasses the application's logging setup. | Use the logger, or rethrow. |

Catching an exception isn't always control flow: parsing user input with `Integer.parseInt` and catching `NumberFormatException` is the normal way, because Java has no "try parse" method.

### 8. JUnit 6 basics

JUnit is the standard Java test framework. JUnit 6 continues the "Jupiter" API of JUnit 5 (package `org.junit.jupiter.api`), so JUnit 5 tutorials still apply.

```java
@DisplayName("J05-09 · LRU cache")
class LruCacheTest {

    private LruCache<String, Integer> cache;

    @BeforeEach
    void createCache() {
        cache = new LruCache<>(2);
    }

    @Test
    @DisplayName("evicts the least recently used entry when a new key arrives in a full cache")
    void evictsLeastRecentlyUsed() {
        cache.put("a", 1);
        cache.put("b", 2);

        cache.put("c", 3);

        assertThat(cache.keys()).containsExactly("b", "c");
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("rejects a capacity below 1")
        void rejectsZeroCapacity() {
            assertThatThrownBy(() -> new LruCache<String, Integer>(0))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
```

- `@Test` marks a test method. Test classes and methods don't need to be `public`.
- `@DisplayName` gives a readable name. In this course, the names of failing tests are your hints, so a name states a behavior: "rejects a capacity below 1".
- `@Nested` groups tests in an inner class, like `describe` in Jest. The exercises group their tests into "Basic", "Edge cases" and "Errors".
- JUnit creates a **new instance of the test class for every test**, so instance fields start fresh each time. `@BeforeEach` and `@AfterEach` methods run around every test. `@BeforeAll` and `@AfterAll` run once per class and must be `static`.
- `@TempDir Path dir`, as a field or a test method parameter, gives a test a new empty directory and deletes it afterwards.

### 9. Parameterized tests

A *parameterized test* runs the same test method once per set of arguments:

```java
@ParameterizedTest(name = "capacity {0}")
@ValueSource(ints = {0, -1, Integer.MIN_VALUE})
@DisplayName("rejects a capacity below 1")
void rejectsInvalidCapacity(int capacity) {
    assertThatThrownBy(() -> new LruCache<String, Integer>(capacity))
            .isInstanceOf(IllegalArgumentException.class);
}

@ParameterizedTest(name = "{0} + {1} = {2}")
@CsvSource({
        "1, 1, 2",
        "2, 3, 5",
        "-1, 1, 0"
})
@DisplayName("adds two numbers")
void adds(int a, int b, int expected) {
    assertThat(a + b).isEqualTo(expected);
}
```

| Source | Supplies |
|---|---|
| `@ValueSource(strings = {...})` | one argument per run: strings, ints, longs, doubles and so on |
| `@CsvSource({"a, 1", "b, 2"})` | several arguments per run, written as CSV; `textBlock = """..."""` for long tables |
| `@EnumSource(Violation.class)` | every constant of an enum, or a selection |
| `@MethodSource("cases")` | any objects, from a `static Stream<Arguments> cases()` method |
| `@NullSource`, `@EmptySource`, `@NullAndEmptySource` | `null`, and `""` or an empty collection |

In the `name` of a run, `{0}`, `{1}` and so on stand for the arguments. JUnit 6 puts text arguments in quotes there, and that includes `@CsvSource` values, which are text until JUnit converts them: the second example shows up as `"1" + "1" = "2"`. Add `quoteTextArguments = false` to `@ParameterizedTest` to leave the quotes out.

### 10. AssertJ and given/when/then

AssertJ assertions read like sentences, and your IDE suggests the next step after `assertThat(x).`:

```java
assertThat(total).isEqualTo(42);                   // equals()
assertThat(cached).isSameAs(original);             // == : the same object
assertThat(name).startsWith("Min").hasSize(5);
assertThat(keys).containsExactly("b", "c");        // these elements, in this order
assertThat(keys).containsExactlyInAnyOrder("c", "b");
assertThat(settings).containsEntry("port", "8080").hasSize(2);
assertThat(cache.get("a")).contains(1);            // an Optional with this value

assertThatThrownBy(() -> loader.loadFile(missing))
        .isInstanceOf(ConfigException.class)
        .hasMessageContaining("missing.conf")
        .hasCauseInstanceOf(NoSuchFileException.class);

OutOfStockException e = catchThrowableOfType(OutOfStockException.class, () -> validator.validate("BOOK-1", 6));
assertThat(e).hasMessageContaining("available 5");
assertThat(e.details()).containsEntry("available", 5);
```

`assertThat(x)` on its own checks nothing. Always finish it with at least one check such as `isEqualTo`.

Write every test in three parts, separated by blank lines:

1. **given**: create the objects and data the test needs,
2. **when**: call the one method you're testing,
3. **then**: check the outcome.

Test one behavior per test. When a test fails, its name should tell you what broke.

### 11. Test doubles: stub, mock, fake

A *test double* stands in for a real collaborator during a test, so the test runs fast and has full control. Three kinds matter:

| Kind | What it does | Example |
|---|---|---|
| Stub | Returns answers that the test prepared. The test then checks the result of the code under test. | an `Inventory` that always says "5 in stock" |
| Mock | Records how it was called, so the test can check the calls. | a `WelcomeMailer`: "was `sendWelcome` called after the user was saved?" |
| Fake | A small working implementation. | an in-memory `UserRepository` that keeps users in a `HashMap` |

When an interface has a single method, a lambda makes a fine stub or fake. J03-02 does this:

```java
Inventory inventory = sku -> switch (sku) {
    case "BOOK-1" -> 5;
    default -> throw new NoSuchElementException("no product row for " + sku);
};
```

Prefer checking results over checking calls. Check a call only when the call *is* the behavior: a mail sent, a row saved, a payment charged.

### 12. Mockito

Mockito creates mocks at runtime:

```java
UserRepository users = mock(UserRepository.class);   // unstubbed methods return false, 0, null or empty

when(users.existsByEmail("minji@example.com")).thenReturn(true);          // stub a return value
when(users.save(any(User.class))).thenThrow(new IllegalStateException("database is down"));
doThrow(new MailException("mail server is down")).when(mailer).sendWelcome(anyString()); // void methods

verify(mailer).sendWelcome("minji@example.com");    // called exactly once, with this argument
verify(users, never()).save(any());
verify(users, times(2)).existsByEmail(anyString());
verifyNoInteractions(passwordHasher, mailer);        // not called at all
```

To look inside an argument, capture it:

```java
ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
verify(users).save(saved.capture());
assertThat(saved.getValue().passwordHash()).isEqualTo("hashed");
```

To check the order of calls, across one or more mocks:

```java
InOrder inOrder = inOrder(users, mailer);
inOrder.verify(users).save(any());
inOrder.verify(mailer).sendWelcome("minji@example.com");
```

With the JUnit extension, Mockito creates fresh mocks before every test:

```java
@ExtendWith(MockitoExtension.class)
class SignupServiceTest {

    @Mock UserRepository users;
    @Mock PasswordHasher passwordHasher;
    @Mock WelcomeMailer welcomeMailer;
    @InjectMocks SignupService service;   // created with the mocks as constructor arguments
    @Captor ArgumentCaptor<User> saved;
}
```

Good to know:

- If one argument of a call uses a matcher such as `any()`, every argument must: `verify(m).send(eq("minji@example.com"), any())`.
- `MockitoExtension` uses *strict stubs*. A test fails when it stubs a call that the code never makes, or when the code calls a stubbed method with different arguments. This keeps tests short and catches mistakes early.
- You rarely need to `verify` a stubbed call. If the code uses the stubbed value, checking the result already proves that the call happened.
- Mockito 5 can also mock classes, even `final` ones. Needing that is often a sign that an interface is missing.

Jest users: `mock(...)` is like `jest.fn()`, `thenReturn` like `mockReturnValue`, and `verify` like `toHaveBeenCalledWith`. Java has no `jest.mock("./module")`: you can't swap a module at runtime. Instead, a class receives its collaborators through its constructor, and a test passes in mocks. That's *dependency injection*, which Spring automates (S01).

### 13. What not to mock

- **Values.** Records, `String`, `List`, `BigDecimal`, `LocalDate`: create real ones. They're easy to build, and a mock can't behave like the real thing.
- **The class under test.** If you want to replace part of it, that part should be a separate class.
- **Types you don't own,** such as a third-party HTTP client. Wrap them in a small interface of your own and mock that.
- **Simple, fast collaborators.** Use the real object.
- **Everything.** A test with five mocks and ten `verify` calls repeats the implementation line by line. It breaks with every refactoring and still catches few bugs. When many tests need the same collaborator, a fake is often simpler.

Mock at the *boundaries* of your code: databases, mail servers, payment providers, other services, and the clock (J07-02).

## TypeScript → Java

| TypeScript | Java | Watch out for |
|---|---|---|
| `throw new Error("…")` | `throw new IllegalArgumentException("…")` | Throw a specific type, not `Exception`. |
| `catch (e)`, with `e: unknown` | `catch (IOException e)`; several types: `catch (A \| B e)` | A `catch` handles only the types it names. |
| no checked exceptions | `throws IOException` in the method signature | The compiler makes every caller catch or declare it. |
| `new Error("…", { cause: e })` | `new ConfigException("…", e)` | Forget the cause and it's gone. |
| `class NotFoundError extends Error` | `class NotFoundException extends RuntimeException` | Extend `RuntimeException` for an unchecked exception. |
| `using file = open(…)` (`Symbol.dispose`) | `try (var reader = …) { … }` (`AutoCloseable`) | A failing `close()` becomes a suppressed exception. |
| `describe` / `it` | `@Nested` class / `@Test` method with `@DisplayName` | A new test class instance for every test. |
| `beforeEach` / `beforeAll` | `@BeforeEach` / `@BeforeAll` | `@BeforeAll` methods are `static`. |
| `it.each` | `@ParameterizedTest` with `@ValueSource`, `@CsvSource`, `@MethodSource` | |
| `expect(x).toEqual(y)` | `assertThat(x).isEqualTo(y)` | Uses `equals()`; records compare their fields. |
| `expect(x).toBe(y)` | `assertThat(x).isSameAs(y)` | `==` on objects compares references. |
| `expect(() => f()).toThrow(NotFoundError)` | `assertThatThrownBy(() -> f()).isInstanceOf(NotFoundException.class)` | |
| `jest.fn()` / `vi.fn()` | `mock(UserRepository.class)` | Unstubbed methods return `false`, `0`, `null` or empty. |
| `fn.mockReturnValue(x)` | `when(m.find()).thenReturn(x)` | For `void` methods: `doThrow(…).when(m).send(…)`. |
| `expect(fn).toHaveBeenCalledWith(a)` | `verify(m).send(a)` | |
| `fn.mock.calls[0][0]` | `ArgumentCaptor` | |
| `jest.mock("./mailer")` | constructor injection with `@Mock` and `@InjectMocks` | Design classes so collaborators come in through the constructor. |

## In real projects

- Business rules fail with unchecked exceptions that carry an error code. One `@RestControllerAdvice` turns them into `ProblemDetail` responses, so controllers contain no `try`/`catch`. See [Exceptions](../../docs/handbook/exceptions.md).
- Checked exceptions stay at the boundary: catch `IOException` where you call the file or network API, and translate it with the cause attached. See [Exceptions](../../docs/handbook/exceptions.md).
- Business code doesn't catch `Exception`. Unexpected failures travel to the global handler, which logs each one once, with its stack trace. See [Exceptions](../../docs/handbook/exceptions.md).
- Arguments are checked at the boundary with `Objects.requireNonNull(x, "x")`, and `IllegalArgumentException` messages contain the bad value. See [Java style](../../docs/handbook/java-style.md).
- For "not found", repositories return `Optional`, and the service throws a not-found exception when the caller needs the thing to exist. See [Null and Optional](../../docs/handbook/null-and-optional.md).
- Exceptions are immutable: `final` fields, and unmodifiable copies of collections. See [Immutability](../../docs/handbook/immutability.md).
- Tests use AssertJ and given/when/then, test one behavior each, and have `@DisplayName`s in plain English. Mocks replace only the boundaries; everything else is real objects or fakes. See [Testing](../../docs/handbook/testing.md).

## Exercises

### J03-01 · Checked exceptions and try-with-resources

**Build** · 30 min · [`ConfigLoader.java`](src/main/java/practice/j03/ex01/ConfigLoader.java)

Almost every service reads settings at startup. You build a small loader for `key=value` files that reports a malformed line with its line number, never leaves a file open, and turns the checked `IOException` into one unchecked `ConfigException` without losing the original error.

**You'll practice:** try-with-resources, `BufferedReader.readLine()`, wrapping a checked exception with its cause, `LinkedHashMap`, unmodifiable maps.

**Rules:** in the Javadoc of `ConfigLoader`: comments and blank lines, where a line is split, line numbers, duplicate keys, the order of the result, who closes the reader, and what each error looks like. `ConfigException` is given. The tests use `TrackingReader` (in the test sources), a `Reader` that remembers whether it was closed and can fail on purpose.

<details>
<summary>Hint 1</summary>

Wrap the `Reader` in a `BufferedReader`. Its `readLine()` returns the next line without the line break (`\n` or `\r\n`), and `null` at the end of the input. Count every line, including the ones you skip.
</details>

<details>
<summary>Hint 2</summary>

Both public methods follow the same rules. Write one private method, `parse(BufferedReader lines) throws IOException`, and call it from both, each time inside its own try-with-resources. `ConfigException` is unchecked, so `catch (IOException e)` lets it pass through untouched.
</details>

<details>
<summary>Hint 3</summary>

`HashMap` doesn't keep the insertion order, and neither does `Map.copyOf`. `LinkedHashMap` does. `Collections.unmodifiableMap(map)` gives you a read-only view of it.
</details>

**Done?** Compare with the reference on the `solutions` branch, then check: What does your `load` throw when a line is malformed *and* `close()` fails? Does any of your messages repeat the content of a line, which could be a password?

### J03-02 · Designing exceptions

**Build** · 30 min · [`BusinessException.java`](src/main/java/practice/j03/ex02/BusinessException.java) and its subclasses, [`OrderValidator.java`](src/main/java/practice/j03/ex02/OrderValidator.java), [`ErrorResponse.java`](src/main/java/practice/j03/ex02/ErrorResponse.java)

An order can fail in several ways, and a REST API must report each one clearly: a status, a stable code, a readable message, and the data behind it. You build a small exception hierarchy on top of the given `ErrorCode` enum, a validator that throws the right exception, and the mapping to an error response, a preview of Spring's `ProblemDetail`.

**You'll practice:** extending `RuntimeException`, passing a message and a cause to `super(...)`, unmodifiable copies with `Map.copyOf`, exception translation, catching only what you can handle.

**Rules:** in the Javadoc of `BusinessException`, of each subclass, of `OrderValidator` and of `ErrorResponse`. `ErrorCode` (codes, HTTP statuses, message templates) and `Inventory` are given. The three subclasses already call `super(...)` with an empty map, because a constructor has to call its superclass constructor; replace that map with the real details.

<details>
<summary>Hint 1</summary>

`ErrorCode.format(details)` builds the message for you, so the base class constructors are `super(code.format(details))` and `super(code.format(details), cause)`. Each subclass builds its details map and passes it to `super(...)`: `Map.of("sku", sku)`.
</details>

<details>
<summary>Hint 2</summary>

`Map.copyOf(map)` returns an unmodifiable copy and rejects `null` keys and values: that's three rules of `BusinessException` in one call.
</details>

<details>
<summary>Hint 3</summary>

In `OrderValidator`, put only the `inventory.stockOf(sku)` call inside `try`, and catch only `NoSuchElementException`. Which test fails if you catch `RuntimeException` instead, and why would a customer care?
</details>

**Done?** Compare with the reference on the `solutions` branch, then check: Why is `BusinessException` abstract? If you added a fourth error, such as "product discontinued", which files would change?

### J03-03 · Write tests: PasswordPolicy

**Write tests** · 30 min · [`PasswordPolicy.java`](src/main/java/practice/j03/ex03/PasswordPolicy.java) → tests in [`PasswordPolicyTest.java`](src/test/java/practice/j03/ex03/PasswordPolicyTest.java)

This time the code is finished and correct, and the tests are your job. That's most of real-world testing: pinning down what existing code must keep doing, so nobody (including an AI assistant) can break it quietly.

**How you're scored:** `./score j03-03` runs your tests twice. First against the real `PasswordPolicy`, where every test must pass. Then against six broken versions, each with one realistic bug. A broken version is *caught* when at least one of your tests fails on it. Uncaught bugs come with a hint; caught ones reveal what the bug was.

**You'll practice:** JUnit 6 `@Test`, `@Nested` and `@DisplayName`, `@ParameterizedTest`, AssertJ's `assertThat` and `assertThatThrownBy`, choosing boundary values.

<details>
<summary>Hint 1</summary>

Start from one password that is valid, and write one test per rule that breaks *only* that rule. When a test fails, you'll know exactly which rule is wrong.
</details>

<details>
<summary>Hint 2</summary>

Bugs love boundaries. For every limit in the Javadoc, test the last value inside it and the first value outside it.
</details>

**Done?** Compare with the reference tests on the `solutions` branch. Would someone reading only your test names understand all the rules?

### J03-04 · Write tests: mocking with Mockito

**Write tests** · 30 min · [`SignupService.java`](src/main/java/practice/j03/ex04/SignupService.java) → tests in [`SignupServiceTest.java`](src/test/java/practice/j03/ex04/SignupServiceTest.java)

`SignupService` coordinates three collaborators: a user repository, a password hasher and a mailer. Its bugs hide in *how* it uses them: the wrong value saved, a mail sent too early, work done after a failure. Mocks make those calls visible.

**How you're scored:** as in J03-03. `./score j03-04` runs your tests against the real `SignupService`, where every test must pass, and then against six broken versions.

**You'll practice:** `@ExtendWith(MockitoExtension.class)`, `@Mock` and `@InjectMocks`, `when(...).thenReturn(...)`, `doThrow(...)`, `verify` with `never()`, `verifyNoInteractions`, `ArgumentCaptor` and `InOrder`.

<details>
<summary>Hint 1</summary>

Write a small helper for the happy path that stubs all three collaborators: the email is free, the hasher returns a hash, and `save` returns an id. Most tests start from it and then change one thing.
</details>

<details>
<summary>Hint 2</summary>

For every rule, ask two questions: what must happen, and what must *not* happen? `verifyNoInteractions(passwordHasher, welcomeMailer)` checks the second kind.
</details>

<details>
<summary>Hint 3</summary>

The `void` methods of a mock do nothing by default. To make the mailer fail: `doThrow(new MailException("mail server is down")).when(welcomeMailer).sendWelcome(anyString());`
</details>

**Done?** Compare with the reference tests on the `solutions` branch. Which of your `verify` calls would fail if someone restructured `SignupService` without changing its behavior? Those are the ones to question.

## Review questions

1. `Files.readString` declares `throws IOException`, but `Integer.parseInt` throws `NumberFormatException` without declaring it. Why the difference, and what does each one mean for the caller?
2. A method catches `IOException` and throws `new ConfigException("could not read the settings")`. What is lost, and how do you keep it?
3. In a try-with-resources statement, the body throws exception A, and then `close()` throws exception B. Which one reaches the caller, and where is the other?
4. Why is `catch (Exception e)` in a service method usually wrong? Name one place where catching broadly is right.
5. What's wrong with logging an exception and then rethrowing it?
6. Why do business exceptions carry a code such as `ORDER-001`, and not only a message?
7. A test class has five tests. How many instances of the class does JUnit create, and why does that matter?
8. What's the difference between a stub, a mock and a fake? Give an example of each from this module.
9. A test stubs `when(passwordHasher.hash("pw")).thenReturn("h")`, but the code under test never calls `hash`. What does `MockitoExtension` do, and why is that useful?
10. Why shouldn't you mock a `User` record or a `List`?

<details>
<summary>Answers</summary>

1. `IOException` is checked: a missing or unreadable file is a normal event at the edge of a program, and the compiler makes the caller decide what to do. `NumberFormatException` is unchecked (it extends `IllegalArgumentException`): it signals bad input that the caller should have validated, and the compiler leaves the decision to you.
2. The original exception is lost: its type, its message (for example the path and "No such file") and its stack trace. Pass it as the cause: `new ConfigException("could not read the settings", e)`.
3. A reaches the caller. B is attached to A as a suppressed exception: `a.getSuppressed()` returns it, and the stack trace lists it under `Suppressed:`.
4. It also catches bugs such as `NullPointerException` and hides them, usually by turning them into a wrong result or a misleading error. Catching broadly is right at the very top of a request or a job: the global error handler that logs the failure and returns a 500 response, or a scheduler that must keep running when one job fails.
5. Every layer that does it logs the same failure again, so one error becomes several log entries, and the logs get harder to read. Either handle the exception (and log it there), or rethrow it, but not both.
6. Codes are stable and machine-readable. Clients can react to `ORDER-001` and show their own translated text, and a log search finds every case, even after someone rewords the message. The code also decides the HTTP status.
7. Five: one per test. Every test starts with fresh instance fields, so tests can't influence each other through shared state, and they can run in any order.
8. A stub returns prepared answers: the lambda `Inventory` in J03-02. A mock records calls so the test can verify them: the `WelcomeMailer` mock in J03-04. A fake is a small working implementation: an in-memory `UserRepository` backed by a `HashMap`. (`TrackingReader` in J03-01 is a fake that also records calls.)
9. The test fails with `UnnecessaryStubbingException`. An unused stub is misleading: the reader thinks it matters. It often means that the test doesn't test what its name says.
10. They're values: easy to create with real data, and their behavior (`equals`, `size`, the getters) is exactly what the code under test relies on. A mock of them only adds setup and can behave differently from the real thing.
</details>

## Further reading

- Dev.java: [Exceptions](https://dev.java/learn/language/annotations-exceptions/exceptions/), a short official tutorial
- The Java Language Specification, [chapter 11: Exceptions](https://docs.oracle.com/javase/specs/jls/se25/html/jls-11.html), for the exact rules
- Joshua Bloch, *Effective Java*, 3rd edition, items 69–77 (exceptions)
- [JUnit User Guide](https://docs.junit.org/current/user-guide/)
- [AssertJ documentation](https://assertj.github.io/doc/)
- [Mockito documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html) (it lives in the Javadoc of the `Mockito` class)
- Martin Fowler, [Mocks Aren't Stubs](https://martinfowler.com/articles/mocksArentStubs.html)
- [RFC 9457: Problem Details for HTTP APIs](https://www.rfc-editor.org/rfc/rfc9457) and Spring's [Error Responses](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html), for S04
- Java API: https://docs.oracle.com/en/java/javase/25/docs/api/
