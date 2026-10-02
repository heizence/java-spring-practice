# Exceptions

## Checked or unchecked?

Java has two kinds of exceptions:

- **Checked** (subclasses of `Exception` but not `RuntimeException`): the compiler forces callers to catch them or declare them with `throws`. Example: `IOException`.
- **Unchecked** (subclasses of `RuntimeException`): no compiler enforcement. Example: `IllegalArgumentException`.

Modern practice, and Spring's own design:

- **Use unchecked exceptions for almost everything**: programming errors (bad arguments, wrong state) and business rule violations ("out of stock").
- **Use checked exceptions rarely**, for a failure the *immediate* caller can and should recover from. In application code that's mostly I/O at the edges.
- **When a checked exception crosses into your domain code, wrap it** in an unchecked one and keep the cause:

```java
try {
    return Files.readString(path);
} catch (IOException e) {
    throw new ConfigException("Can't read config file " + path, e); // keep e as the cause!
}
```

## Which exception to throw

Reuse the standard exceptions when they fit. Every Java developer knows what they mean.

| Situation | Exception |
|---|---|
| An argument is invalid (`quantity` is negative) | `IllegalArgumentException` |
| An argument is `null` but mustn't be | `NullPointerException`, via `Objects.requireNonNull(x, "x")` |
| The object is in the wrong state for the call (withdraw from a closed account) | `IllegalStateException` |
| An index is out of range | `IndexOutOfBoundsException` |
| There's no element (`pop()` on an empty stack) | `NoSuchElementException` |
| The operation isn't supported (modifying an unmodifiable list) | `UnsupportedOperationException` |
| A business rule is broken | your own exception (below) |

## Your own exceptions

For business errors, define a small hierarchy with **error codes**. The codes let one central place (in Spring, a `@RestControllerAdvice`) turn every business error into a consistent API response.

```java
public enum ErrorCode {
    OUT_OF_STOCK("ORDER-001", "Not enough stock"),
    PRODUCT_NOT_FOUND("PRODUCT-001", "Product not found");
    // fields, constructor, accessors …
}

public abstract class BusinessException extends RuntimeException {
    private final ErrorCode code;

    protected BusinessException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }
}

public class OutOfStockException extends BusinessException {
    public OutOfStockException(String sku, int requested, int available) {
        super(ErrorCode.OUT_OF_STOCK,
              "Requested %d of %s, but only %d left".formatted(requested, sku, available));
    }
}
```

## Messages

- **Include the bad value and the context**: `"capacity must be at least 1, but was 0"`, not `"invalid capacity"`. The message is often all you have when debugging production.
- **Never put secrets or personal data in messages**: passwords, tokens, full card numbers. Messages end up in logs.

## Anti-patterns

| Don't | Why | Do instead |
|---|---|---|
| `catch (Exception e) { }` | Swallows the failure. The program continues in a broken state. | Catch only what you can handle; let the rest propagate. |
| `catch (Exception e)` deep in business code | Also catches bugs (`NullPointerException`) you didn't mean to handle. | Catch specific types. Broad catches belong at the top boundary (the web layer). |
| `throw new MyException(e.getMessage())` | Loses the stack trace of the original problem. | `throw new MyException("context", e)` |
| `log.error(...)` and then `throw` | The same error gets logged twice or more. | Log once, where it's handled (usually the boundary). |
| Exceptions for normal control flow | Slow and confusing. | Return `Optional` or a result value for expected outcomes. |
| `return` or `throw` inside `finally` | Silently replaces the original exception. | Use `finally` only for cleanup, or better, try-with-resources. |

## Always use try-with-resources

Anything that must be closed (files, streams, connections, executors) goes in a try-with-resources block. It closes the resource even when an exception is thrown, and keeps any exception from `close()` as a "suppressed" exception instead of losing the original:

```java
try (var reader = Files.newBufferedReader(path)) {
    return parse(reader);
}
```

## Testing exceptions

```java
assertThatThrownBy(() -> account.withdraw(500))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("balance");
```

Assert the **type** and something about the **message** or **error code**. A test that only checks "something was thrown" also passes when the wrong thing fails.
