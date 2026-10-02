# Immutability

An immutable object can't change after it's created. That one property removes a whole class of bugs:

- **No surprise changes.** Code that holds a reference can't be broken by other code changing the object.
- **Thread safety for free.** Immutable objects can be shared between threads without locks.
- **Safe map keys and set elements.** Their `hashCode` never changes.
- **Easier reasoning.** You only need to check an object's rules once, in the constructor.

Make classes immutable unless there's a good reason not to. In Spring applications, DTOs, configuration and value objects are immutable. Entities (objects with an identity and a life cycle, like an `Order`) change state, but only through methods that keep their rules.

## How to make a class immutable

1. Make all fields `private final`.
2. Don't provide setters. Methods that "change" something return a new object instead (`plus`, `withX`).
3. Validate everything in the constructor, so an invalid object can never exist.
4. **Copy mutable inputs, and don't hand out mutable internals.**
5. Make the class `final`, or use a record, so a subclass can't add mutable state.

```java
public final class Team {
    private final String name;
    private final List<String> members;

    public Team(String name, List<String> members) {
        this.name = Objects.requireNonNull(name, "name");
        this.members = List.copyOf(members); // copy: later changes to the caller's list don't affect us
    }

    public List<String> members() {
        return members; // already unmodifiable, so it's safe to return
    }

    public Team withMember(String member) {
        var updated = new ArrayList<>(members);
        updated.add(member);
        return new Team(name, updated); // a new object; this one never changes
    }
}
```

`List.copyOf` both copies and makes the result unmodifiable. `Collections.unmodifiableList(list)` does **not** copy: it's a read-only *view*, and changes to the original list still show through it.

## Records

Records are the standard way to write immutable data carriers since Java 16:

```java
public record Range(int min, int max) {
    public Range {                      // compact constructor: validate and normalize here
        if (min > max) {
            throw new IllegalArgumentException("min (%d) must not exceed max (%d)".formatted(min, max));
        }
    }
}
```

A record gives you private final fields, a constructor, accessors (`min()`, not `getMin()`), and `equals`, `hashCode` and `toString` based on all components.

- **Records are shallowly immutable.** A `record Team(List<String> members)` still holds whatever list you passed. Copy it in the compact constructor: `members = List.copyOf(members);`.
- Use records for DTOs, value objects, query results and parameter objects.
- Don't use records for JPA entities (Part 2). Entities need a no-argument constructor and change over time.

## Static factory methods and builders

A **static factory method** is a named alternative to a constructor:

```java
Temperature.ofCelsius(21.5);
Temperature.ofFahrenheit(70.7);
List.of(1, 2, 3);
```

Factories can have descriptive names, can return cached instances, and can return a subtype.

A **builder** helps when a class has many parameters, especially optional ones:

```java
var request = HttpRequest.builder("https://api.example.com/books")
        .method("POST")
        .header("Accept", "application/json")
        .build(); // validates everything at once
```

Validate in `build()`, and keep the built object immutable.

## Value objects

A **value object** is defined by its value, not by an identity: money, an ISBN, an email address, a date range. Two value objects with the same value are interchangeable.

- They are immutable.
- `equals` and `hashCode` compare the value. Records do this automatically.
- They validate and normalize on creation, so an `Isbn` object is always a valid ISBN. This removes repeated validation code everywhere else.

Wrapping primitives in small value objects (`Money` instead of `BigDecimal`, `Email` instead of `String`) makes method signatures self-explaining and prevents mix-ups, like passing a price where a quantity was expected.

## `equals` and `hashCode`

If you override `equals`, you **must** override `hashCode` consistently. Equal objects must have equal hash codes, otherwise `HashMap` and `HashSet` break silently.

`equals` must be:
- **reflexive**: `x.equals(x)`;
- **symmetric**: `x.equals(y)` exactly when `y.equals(x)`;
- **transitive**;
- **consistent**: the same answer every time while the objects don't change;
- `false` for `null`, without throwing.

Records and IDE-generated code (IntelliJ: **Code → Generate → equals() and hashCode()**) get this right. Never base `hashCode` on fields that can change while the object is inside a hash-based collection.

## Remember: immutable JDK types return new objects

`String`, `BigDecimal`, `LocalDate`, `Instant`, `Duration` and boxed numbers are immutable. Their "modifying" methods return a new object, and **ignoring the result is a bug**:

```java
price.add(tax);          // does nothing useful
price = price.add(tax);  // correct
```
