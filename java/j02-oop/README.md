# J02 · Object-oriented Java

⏱ 8 sessions · weeks 1–2 · 7 exercises

In Java, every method belongs to a class, and almost every value you handle is an object. This module shows how experienced Java developers design those classes: they keep state private behind methods that enforce rules, make values immutable, prefer interfaces and composition to deep class hierarchies, and model data with records, enums and sealed types. Spring is built on the same ideas. Your services are objects behind interfaces, your configuration uses builders, and your requests and responses are records. You'll also meet the places where Java differs most from TypeScript: a type's name decides what fits where (not its shape), `equals` and `hashCode` decide what counts as "the same", and access rules still hold at run time.

## Lesson

### 1. Classes, objects and constructors

A *class* describes a kind of object: the data each object holds (its *fields*) and what it can do (its *methods*). `new` creates an object, also called an *instance*, and runs a *constructor* to set it up.

```java
public class Counter {
    private final String name; // a field: every Counter object has its own name
    private int count;         // fields start as 0, false or null until you assign them

    public Counter(String name) { // a constructor: same name as the class, no return type
        this.name = name;         // `this` is the object being built: this.name is the field, name the parameter
    }

    public Counter() {
        this("unnamed");          // constructor chaining: calls the other constructor
    }

    public void increment() {
        count++;                  // inside the class, `count` means this.count
    }

    public int count() {
        return count;
    }
}

Counter clicks = new Counter("clicks");
clicks.increment();
```

- A file holds one public top-level class and has its name: `Counter.java`.
- A class without any constructor gets an empty one with no parameters. As soon as you write a constructor yourself, Java no longer adds the empty one.
- A `final` field must be assigned exactly once: where it is declared, or in every constructor. The compiler checks this for you.
- TypeScript's parameter properties (`constructor(private name: string)`) don't exist. You declare the field and assign it yourself. Records (section 9) remove this boilerplate for data classes.
- Check the arguments in the constructor, so that an object never exists in a broken state:

```java
public Counter(String name) {
    this.name = Objects.requireNonNull(name, "name"); // NullPointerException with the message "name"
    if (name.isBlank()) {
        throw new IllegalArgumentException("name must not be blank");
    }
}
```

Since Java 25, a constructor may run statements *before* it calls `this(...)` or `super(...)`, as long as they don't use the object under construction. That is useful for checking arguments first; you'll use it in J02-03.

### 2. Encapsulation and access modifiers

*Encapsulation* means: an object keeps its data private, and other code can change it only through methods that enforce the rules. A rule that must always hold for an object is called an *invariant*, for example "the balance is never negative". When the field is private and every method keeps the invariant, no code anywhere can break it.

```java
public final class Wallet {
    private long balance; // no code outside this class can write the field...

    public void pay(long amount) { // ...only methods that check the rules can
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0, but was " + amount);
        }
        if (amount > balance) {
            throw new IllegalStateException("not enough money: " + balance + " < " + amount);
        }
        balance -= amount;
    }
}
```

Prefer methods that say what happens (`pay`, `deposit`, `cancel`) to setters (`setBalance`). A setter accepts any value, and the rules end up scattered across its callers.

| Modifier | Who can use the member | TypeScript |
|---|---|---|
| `private` | code in the same class (including its nested classes) | `private`, `#field` |
| *(none)*, called *package-private* | code in the same package | no equivalent |
| `protected` | code in the same package, plus subclasses | `protected` (without the package part) |
| `public` | all code | `public` (the default) |

Three differences from TypeScript matter in practice:

- **The default is package-private, not public.** A field without a modifier is visible to the whole package. Write a modifier on every field and method, on purpose.
- **`private` belongs to the class, not to the object.** A `Wallet` method may read the private `balance` of *another* `Wallet`. TypeScript works the same way.
- **Java checks access at run time too.** TypeScript's `private` disappears after compilation, and any JavaScript can read the field. Java keeps the modifier in the compiled class, and the JVM enforces it.

Java has two naming styles for methods that read a field: `owner()`, which records use, and `getOwner()`, the older *JavaBeans* style that some frameworks look for. These exercises use `owner()`. Part 2 uses getters where a framework needs them.

Choose the exception type by what went wrong:

| What went wrong | Throw | Example |
|---|---|---|
| A required argument is `null` | `NullPointerException`, via `Objects.requireNonNull(x, "x")` | `new Wallet(null)` |
| The argument is wrong whatever state the object is in | `IllegalArgumentException` | a negative amount, a blank name |
| The argument is fine, but the object's current state doesn't allow the call | `IllegalStateException` | paying more than the balance |
| A calculation doesn't fit in its type | `ArithmeticException`, via `Math.addExact`, `Math.multiplyExact` and so on | a balance above `Long.MAX_VALUE` |

Put the bad value in the message: `"amount must be greater than 0, but was -500"` saves time when you read logs.

### 3. Static and instance members

An instance member belongs to each object. A `static` member belongs to the class itself: there is exactly one, shared by every object.

```java
public final class Ticket {
    private static int issued = 0; // one counter for the whole class
    private final int number;      // one number per ticket

    public Ticket() {
        issued++;
        this.number = issued;
    }

    public static int issuedSoFar() { // called on the class: Ticket.issuedSoFar()
        return issued;                // a static method has no `this`, so it can't read `number`
    }
}
```

- Call static members on the class: `Math.max(a, b)`, `List.of(1, 2)`, `Ticket.issuedSoFar()`.
- Constants are `static final` and named in UPPER_SNAKE_CASE: `static final int MAX_ITEMS = 50;`.
- A *utility class* holds only static methods. Make it `final` with a private constructor, like `Math` and `Objects`.
- A static field that changes is global state. Every object and every thread shares it, so tests can influence each other. `issued++` isn't even safe with threads: two threads can read the same value and hand out the same number. `AtomicLong` fixes that (J08 explains why).
- The closest TypeScript equivalent of a static field is a variable at the top level of a module.

### 4. Interfaces and abstract classes

An *interface* lists methods that a class promises to provide. A class can implement any number of interfaces. An interface has no instance fields, but it can contain:

- *abstract* methods: no body; every implementation must write them;
- `default` methods: a body that implementations inherit and may override;
- `static` methods: helpers called on the interface, such as `Comparator.naturalOrder()`;
- `private` methods that help the other methods.

```java
public interface Greeter {
    String name(); // abstract: implementations must provide it

    default String greet() { // default: inherited, may be overridden
        return "Hello, " + name() + "!";
    }

    static Greeter of(String name) { // static: called as Greeter.of("Mina")
        return new FixedGreeter(name);
    }
}

record FixedGreeter(String name) implements Greeter { } // the record's name() implements the interface
```

An *abstract class* is a class you can't instantiate. It can have fields, constructors and finished methods, plus `abstract` methods that subclasses must implement. A class can extend only one class.

The *template method* pattern uses an abstract class to fix the steps of an algorithm and leave one or two of them to subclasses:

```java
public abstract class Report {
    public final String render() { // final: no subclass can change the steps
        return title().toUpperCase(Locale.ROOT) + "\n" + body() + "\n(end of report)";
    }

    protected abstract String title(); // protected: meant for subclasses, not for callers
    protected abstract String body();
}

public final class SalesReport extends Report {
    @Override
    protected String title() {
        return "Sales";
    }

    @Override
    protected String body() {
        return "Total: 1,250,000 won";
    }
}
```

| Choose an interface when… | Choose an abstract class when… |
|---|---|
| unrelated classes share a capability (`Comparable`, `AutoCloseable`, a `PaymentGateway` service) | closely related classes share fields and code |
| a class needs several of them | you want to fix the order of steps (template method) |
| you want to swap implementations, for example a fake in tests | you need a constructor that every subclass runs |

Start with an interface. Add an abstract class when subclasses really need to share fields and code.

### 5. Polymorphism and dynamic dispatch

```java
Greeter greeter = new FormalGreeter(); // declared type: Greeter. Class of the object: FormalGreeter.
greeter.greet();
```

- The declared type of the variable (its *static type*) decides which methods you may call. The compiler checks this.
- The class of the actual object (its *run-time type*) decides which implementation runs. This is *dynamic dispatch*, and it's what *polymorphism* means: one call, several possible implementations.
- It also works from inside a superclass or an interface. When the default method `greet()` calls `name()`, the JVM runs the `name()` of the object's real class.
- Write `@Override` on every overriding method. It asks the compiler to check that the method really overrides something. Without it, a typo or a different parameter type silently creates a second method instead.
- *Overloading* means several methods with the same name and different parameter types. The compiler picks one at compile time, from the static types of the arguments. Section 7 shows a classic bug that this causes in `equals`.

JavaScript also looks methods up at run time, through the prototype chain, so dispatch will feel familiar. Overloading won't: TypeScript only has overload *declarations* for a single implementation.

### 6. The trouble with inheritance, and composition

`extends` gives a subclass everything the superclass has, including its internal habits. A subclass that overrides methods depends on how the superclass calls its *own* methods. That is called *self-use*, it's an implementation detail, and it can change in any version. This is the *fragile base class* problem.

```java
public class Stack {                        // a class someone else wrote
    public void push(String item) { /* ... */ }

    public void pushAll(List<String> items) {
        for (String item : items) {
            push(item);                     // self-use: pushAll calls push
        }
    }
}

public class CountingStack extends Stack {
    private int pushed;

    @Override
    public void push(String item) {
        pushed++;
        super.push(item);
    }

    @Override
    public void pushAll(List<String> items) {
        pushed += items.size();
        super.pushAll(items);               // calls push() for each item: counted again!
    }
}
```

`pushAll` with 3 items counts 6. Nothing in `Stack`'s public methods tells you that `pushAll` calls `push`. If the next version stops doing so, the count changes again.

*Composition* avoids the problem. Instead of extending a class, keep an object of it in a private field and *forward* (or *delegate*) each call to it:

```java
public final class CountingStack implements StackOperations { // the same interface as the wrapped object
    private final StackOperations stack;                      // the wrapped object
    private int pushed;

    public CountingStack(StackOperations stack) {
        this.stack = Objects.requireNonNull(stack, "stack");
    }

    @Override
    public void push(String item) {
        pushed++;
        stack.push(item);       // the wrapped object's internal calls never reach this class
    }

    @Override
    public void pushAll(List<String> items) {
        pushed += items.size();
        stack.pushAll(items);   // forwarded once, counted once
    }
}
```

The wrapper depends only on the public methods of the interface, so the wrapped object's internals can't break it, and it works with every implementation of the interface. A wrapper that implements the same interface and adds behavior is called a *decorator*.

More inheritance pitfalls:

- A constructor that calls an overridable method runs subclass code before the subclass has set its own fields.
- A subclass must keep every promise of its superclass. This is the *Liskov substitution principle*. A `Square` that extends a `Rectangle` with a `setWidth` method breaks the promise "setting the width leaves the height alone".

In short: prefer composition. Use `extends` only when the subclass really is a kind of the superclass and the superclass was designed for it. Make every other class `final`. Spring follows the same idea on a larger scale: a service gets its collaborators through its constructor instead of extending them.

### 7. equals, hashCode and toString

For objects, `==` compares references: "are these the same object?" To compare values, call `equals`. Every class inherits `equals` from `Object`, and until you override it, it does the same as `==`.

Override `equals` for *value objects*: objects defined only by their values, such as an ISBN, an amount of money or a date. Don't override it for *entities*, objects with an identity of their own: two bank accounts with the same owner and balance are still two accounts.

```java
public final class Money {
    private final long amount;
    private final String currency;

    public Money(long amount, String currency) {
        this.amount = amount;
        this.currency = Objects.requireNonNull(currency, "currency");
    }

    @Override
    public boolean equals(Object other) {   // the parameter type must be Object
        return other instanceof Money that  // false for null and other types; also casts
                && amount == that.amount
                && currency.equals(that.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency); // exactly the fields that equals compares
    }

    @Override
    public String toString() {
        return amount + " " + currency;     // for logs and test failure messages
    }
}
```

The contract of `equals`, from the Javadoc of `Object`:

| Rule | Meaning |
|---|---|
| reflexive | `x.equals(x)` is `true` |
| symmetric | `x.equals(y)` is `true` exactly when `y.equals(x)` is `true` |
| transitive | if `x.equals(y)` and `y.equals(z)`, then `x.equals(z)` |
| consistent | the answer stays the same as long as the objects don't change |
| `null` | `x.equals(null)` is `false` and doesn't throw |

The contract of `hashCode`: **equal objects must have equal hash codes.** Unequal objects may share a hash code, but fewer collisions make hash tables faster. `HashMap` and `HashSet` use `hashCode` to choose a bucket, then `equals` to compare inside the bucket. If you break the rule, a `HashSet` can hold two "equal" objects, or fail to find one that is there.

Common mistakes:

- Overriding `equals` but not `hashCode`.
- Writing `equals(Money other)`. That's an *overload*, and collections never call it: they call `equals(Object)`. With `@Override`, the compiler reports the mistake.
- Calling `other.getClass()` before checking for `null`.
- Basing `equals` on fields that change while the object is in a `HashSet`: afterwards, the set looks in the wrong bucket.
- Comparing `double` values with `==` after arithmetic. `0.1 + 0.2 == 0.3` is `false` in Java too.

Write `toString` for every value class. Show the type or unit and the important values. Never include passwords, tokens or personal data that mustn't end up in logs. Records (section 9) generate all three methods for you.

### 8. Immutability, static factories and builders

An *immutable* object can't change after it is created. That makes it simple to reason about, safe to share between threads, safe as a `HashMap` key, and safe to pass to code you don't trust.

How to make a class immutable:

1. No setters, and no other methods that change state.
2. Make every field `private final`.
3. Make the class `final`, or its constructors private, so that no subclass can add state that changes.
4. Copy mutable inputs, and never hand out your mutable internals. These are *defensive copies*.
5. Methods that "change" the value return a new object instead, like `LocalDate.plusDays(1)` and `BigDecimal.add(other)`.

`final` alone doesn't make anything immutable:

```java
private final List<String> tags = new ArrayList<>(); // the reference can't change...
tags.add("sale");                                    // ...but the list can
```

TypeScript's `readonly` has the same limit: `readonly items: string[]` still allows `items.push(...)`.

A *copy* and a *view* are different things:

```java
List<String> source = new ArrayList<>(List.of("a"));
List<String> copy = List.copyOf(source);                  // an unmodifiable copy
List<String> view = Collections.unmodifiableList(source); // an unmodifiable VIEW of source

source.add("b");
System.out.println(copy); // [a]     a snapshot
System.out.println(view); // [a, b]  changes to source show through
```

`List.of`, `List.copyOf`, `Set.copyOf` and `Map.copyOf` create unmodifiable copies (they also reject `null`). `Collections.unmodifiableList` and its relatives only stop *you* from changing the collection through that one reference.

A *static factory method* is a static method that returns an object, often instead of a public constructor:

```java
Duration timeout = Duration.ofSeconds(30);
List<String> names = List.of("Mina", "Joon");
Optional<String> nothing = Optional.empty();
```

Static factories have names. A class can't have two constructors `Temperature(double celsius)` and `Temperature(double fahrenheit)`, because both take one `double`. It can have `ofCelsius(double)` and `ofFahrenheit(double)`. A factory can also check or normalize its input, return an object it already has, or return a subtype. Common names are `of`, `from`, `valueOf`, `parse`, `create` and `newSomething`. When the factories should be the only way to get an object, make the constructor `private`.

A *builder* helps when an object has many values and some are optional. The builder is a separate, mutable object that collects the values. Its `build()` method checks them all at once and creates the immutable object.

```java
public final class Email {
    private final String to;
    private final String subject;
    private final List<String> cc;

    private Email(Builder builder) {        // only Builder.build() calls this
        this.to = builder.to;
        this.subject = builder.subject;
        this.cc = List.copyOf(builder.cc);  // a copy: the builder may keep changing
    }

    public static Builder builder(String to) { // the required value goes here
        return new Builder(to);
    }

    public static final class Builder {     // a static nested class (section 11)
        private final String to;
        private String subject = "";
        private final List<String> cc = new ArrayList<>();

        private Builder(String to) {
            this.to = Objects.requireNonNull(to, "to");
        }

        public Builder subject(String subject) {
            this.subject = Objects.requireNonNull(subject, "subject");
            return this;                    // returning the builder allows chained calls
        }

        public Builder cc(String address) {
            cc.add(Objects.requireNonNull(address, "address"));
            return this;
        }

        public Email build() {              // one place that checks everything
            if (!to.contains("@")) {
                throw new IllegalArgumentException("not an email address: " + to);
            }
            return new Email(this);
        }
    }
}

Email email = Email.builder("mina@example.com").subject("Your order").cc("joon@example.com").build();
```

Many immutable classes also offer `toBuilder()`, which returns a builder filled with the object's current values. It's the immutable way to say "the same, but with a different subject". In Spring code you'll often see Lombok's `@Builder(toBuilder = true)` generate all of this. In Part 1 you write it by hand, so you know what Lombok writes for you.

### 9. Records

```java
public record Point(int x, int y) { }
```

That line gives you private final fields `x` and `y`, a constructor `Point(int x, int y)`, the accessors `x()` and `y()` (not `getX()`), `equals` and `hashCode` that compare all components, and a `toString()` that prints `Point[x=1, y=2]`.

A *compact constructor* validates and normalizes the values:

```java
public record EmailAddress(String value) {
    public EmailAddress {                              // no parameter list: it has the record's parameters
        Objects.requireNonNull(value, "value");
        value = value.strip().toLowerCase(Locale.ROOT); // you may assign new values to the parameters...
        if (!value.contains("@")) {
            throw new IllegalArgumentException("not an email address: " + value);
        }
    }                                                  // ...and the final values are stored after this line
}
```

- A record is `final` and can't extend a class, but it can implement interfaces.
- It may have static fields, static methods and instance methods, but no extra instance fields.
- It is only *shallowly* immutable. A `List` component can still change, unless you copy it with `List.copyOf` in the compact constructor.
- A record declared inside another type is automatically `static`. A record declared inside an interface is also `public`, so its compact constructor must be `public` too.

Use records for plain data: values such as `Money`, query results, and the *DTOs* (data transfer objects) that carry JSON requests and responses. Jackson and Spring handle records well.

### 10. Enums with fields and behavior

A Java `enum` is a class with a fixed set of instances. Each constant is an object, created once when the enum is loaded. Enums can have fields, constructors (always private) and methods.

```java
public enum Coin {
    TEN(10), FIFTY(50), HUNDRED(100), FIVE_HUNDRED(500);

    private final int won;

    Coin(int won) { // runs once for each constant
        this.won = won;
    }

    public int won() {
        return won;
    }

    public static Coin fromWon(int won) {
        for (Coin coin : values()) { // values(): every constant, in declaration order
            if (coin.won == won) {
                return coin;
            }
        }
        throw new IllegalArgumentException("no coin is worth " + won + " won");
    }
}
```

Behavior that differs per constant goes in a `switch` expression, or in *constant-specific bodies*: a class body for each constant. Both versions below do the same:

```java
public enum Shipping {
    STANDARD,
    EXPRESS;

    public long fee(long grams) {
        return switch (this) {  // no default: adding a constant breaks the build until you handle it here
            case STANDARD -> 3_000;
            case EXPRESS -> 5_000 + grams / 1_000 * 500;
        };
    }
}

public enum Shipping { // the same enum, written with constant-specific bodies
    STANDARD {
        @Override
        public long fee(long grams) {
            return 3_000;
        }
    },
    EXPRESS {
        @Override
        public long fee(long grams) {
            return 5_000 + grams / 1_000 * 500;
        }
    };

    public abstract long fee(long grams); // every constant must implement it
}
```

- `name()` returns `"EXPRESS"`, `valueOf("EXPRESS")` does the opposite (and throws `IllegalArgumentException` for an unknown name), `values()` returns every constant, and `ordinal()` returns the position (0, 1, …).
- Don't build logic on `ordinal()`. Reordering the constants would silently change the behavior. Keep what you need in a field.
- A constant can't mention a *later* constant in its constructor arguments. `PLACED(PAID)` doesn't compile ("illegal forward reference"), because `PAID` doesn't exist yet when `PLACED` is created. Compute such relationships in a method.
- `EnumMap` and `EnumSet` are a map and a set made for enum keys. They keep declaration order and store the data in a small array:

```java
Map<Coin, Integer> counts = new EnumMap<>(Coin.class);
counts.put(Coin.TEN, counts.getOrDefault(Coin.TEN, 0) + 1);
```

TypeScript enums are named numbers or strings. Java enums are objects with types, fields and methods, and the compiler knows the complete list of constants.

### 11. Nested, inner and anonymous classes

| Kind | How you declare it | Holds a reference to an outer object? | Typical use |
|---|---|---|---|
| static nested class | `static class Builder` inside a class | no | builders, helper types that belong to one class |
| inner class | `class ItemView` inside a class, without `static` | yes, `Order.this` | an object that works with its outer object's fields |
| local class | a class declared inside a method | yes, in an instance method | rare |
| anonymous class | `new SomeType() { ... }` | yes, in an instance method | one-off implementations, test doubles |

```java
public class Order {
    private final List<String> items = new ArrayList<>();

    static class Builder { /* ... */ } // needs no Order: new Order.Builder()

    class ItemView {                   // inner: every ItemView belongs to one Order
        int count() {
            return items.size();       // reads Order.this.items
        }
    }
}

Order order = new Order();
Order.ItemView view = order.new ItemView(); // an inner object needs an outer object
```

An anonymous class defines and instantiates a class in one expression:

```java
Comparator<String> byLength = new Comparator<>() {
    @Override
    public int compare(String a, String b) {
        return Integer.compare(a.length(), b.length());
    }
};
```

For an interface with a single abstract method, a lambda is shorter (J06). Anonymous classes are still useful for abstract classes, and in tests. The J02-03 tests use one to create a `DiscountPolicy`.

A good default: make a nested class `static` unless it needs the outer object. An inner object keeps its outer object alive in memory, which can cause memory leaks (J10).

### 12. Sealed types and pattern matching

A *sealed* interface or class lists every type allowed to implement or extend it:

```java
public sealed interface Event permits OrderPlaced, OrderShipped, OrderCancelled { }

public record OrderPlaced(long orderId, long total) implements Event { }
public record OrderShipped(long orderId, String trackingNumber) implements Event { }
public record OrderCancelled(long orderId, String reason) implements Event { }
```

When the permitted types are declared in the same file, you can leave out `permits`. Every permitted type must be `final` (records are), `sealed` or `non-sealed`.

*Pattern matching* tests a value's type and extracts data in one step. With `instanceof`:

```java
if (event instanceof OrderShipped shipped) { // test and cast in one step
    notifyCustomer(shipped.trackingNumber());
}
```

With `switch`:

```java
String describe(Event event) {
    return switch (event) {
        case OrderPlaced placed when placed.total() >= 100_000 -> "big order " + placed.orderId();
        case OrderPlaced placed -> "order " + placed.orderId();
        case OrderShipped(long id, String tracking) -> "order " + id + " shipped: " + tracking;
        case OrderCancelled(long id, _) -> "order " + id + " cancelled";
    }; // no default: the compiler knows these are all the kinds of Event
}
```

- A *type pattern* such as `case OrderPlaced placed` matches the type and binds a variable.
- A *guard*, `when ...`, adds a condition. Cases are tried from top to bottom, so a guarded case must come before the unguarded case for the same type. The other order doesn't compile, because the unguarded case would always match first.
- A *record pattern* such as `case OrderShipped(long id, String tracking)` takes a record apart into its components. Patterns can nest: `case Line(Point(var x1, var y1), Point end)`.
- The *unnamed pattern* `_` matches anything and binds nothing: `case OrderCancelled(long id, _)`, or `case OrderPlaced _` for the type alone. Several patterns without variables can share one case: `case OrderShipped _, OrderCancelled _ -> ...`.
- Java has no constant patterns such as `case OrderPlaced(long id, 0)` yet. Use a guard: `case OrderPlaced(long id, long total) when total == 0`.
- A pattern `switch` throws `NullPointerException` for `null`, unless it has a `case null`.

Exhaustiveness is the main benefit. When you add a fourth kind of `Event`, every `switch` without a `default` branch stops compiling, and the compiler points to each place you must update. A `default` branch would hide them, so leave it out when you switch over a sealed type.

### 13. Three TypeScript habits to rethink

**Structural versus nominal typing.** TypeScript is *structural*: any value with the right shape fits.

```ts
interface Named { name: string }
const user = { name: "Mina", age: 30 };
const named: Named = user; // fine: the shape fits
```

Java is *nominal*: a type fits only if its declaration says so.

```java
interface Named { String name(); }
record User(String name, int age) { }

Named named = new User("Mina", 30); // compile error: User doesn't declare "implements Named"
```

Two records with identical components are unrelated types. That's stricter, but the compiler knows every relationship, which makes renaming and "find all implementations" reliable in a large codebase.

**`readonly` versus `final`.** Both only stop reassignment, and neither makes the referenced object immutable. Java's `final` also works on local variables and parameters, and it has two more meanings: a `final` class can't be extended, and a `final` method can't be overridden.

**Discriminated unions versus sealed interfaces with records.**

```ts
type Payment =
  | { kind: "card"; last4: string }
  | { kind: "points"; balance: number };

function fee(payment: Payment, amount: number): number {
  switch (payment.kind) {
    case "card": return Math.floor(amount * 0.02);
    case "points": return 0;
  }
}
```

```java
sealed interface Payment permits Card, Points { }
record Card(String last4) implements Payment { }
record Points(long balance) implements Payment { }

long fee(Payment payment, long amount) {
    return switch (payment) {
        case Card card -> amount * 2 / 100;
        case Points points -> 0;
    };
}
```

The `kind` tag becomes the record type, narrowing becomes a pattern, and TypeScript's exhaustiveness check (with `never`) becomes the Java compiler's exhaustiveness check.

## TypeScript → Java

| TypeScript | Java | Watch out for |
|---|---|---|
| `constructor(private readonly id: number) {}` | a `private final` field and a constructor that assigns it | no parameter properties; a `record` comes close |
| members are `public` unless marked | members are package-private unless marked | write `private` on fields every time |
| `private` (compile time only), `#field` | `private`, also checked at run time | private to the class, not to the object |
| structural typing: same shape, same type | nominal typing: `implements` is required | two records with the same fields are unrelated |
| `readonly` | `final` | both are shallow: the object can still change |
| `interface` (shape only) | `interface` with `default`, `static` and `private` methods | no instance fields in interfaces |
| `abstract class` | `abstract class` | a class extends only one class |
| `===` on objects | `==` (same object?) | compare values with `equals` |
| `Map`/`Set` find object keys by identity | `HashMap`/`HashSet` use `equals` and `hashCode` | override both together, or neither |
| discriminated union + `switch (x.kind)` | `sealed` interface + records + pattern `switch` | no `default`, so the compiler checks completeness |
| `enum` or a union of string literals | `enum` with fields, constructors and methods | don't put logic on `ordinal()` |
| `{ ...request, url }` | `request.toBuilder().url(url).build()` | the original object stays as it was |
| `Object.freeze`, `ReadonlyArray` | `List.of`, `List.copyOf`, `Map.copyOf` | `Collections.unmodifiableList` is only a view |
| overload declarations, one implementation | real overloads, chosen at compile time | `equals(Money)` is an overload, not an override |
| object literal that implements an interface | anonymous class or lambda | inner and anonymous classes hold their outer object |

## In real projects

- Make fields `private final`, give domain objects behavior instead of setters, and make classes `final` unless you design them for subclasses. See [Java style](../../docs/handbook/java-style.md).
- Make value objects immutable: records, `List.copyOf` at the boundaries, `toBuilder()` or `with...` methods for changed copies. See [Immutability](../../docs/handbook/immutability.md).
- Check arguments where they enter: `Objects.requireNonNull` for required references, `IllegalArgumentException` for bad values. Don't return `null` from public methods. See [Null and Optional](../../docs/handbook/null-and-optional.md).
- Pick exceptions by cause (`IllegalArgumentException` for a bad argument, `IllegalStateException` for the wrong state) and put the bad value in the message. See [Exceptions](../../docs/handbook/exceptions.md).
- Anything you put in a `HashSet` or use as a `HashMap` key needs consistent `equals` and `hashCode`; records generate them. JPA entities are different and get their own rules in S06. See [Collections and generics](../../docs/handbook/collections-and-generics.md).
- Keep amounts of won in a `long` and use exact arithmetic such as `Math.addExact`. J07 introduces `BigDecimal` for amounts with decimals. See [Dates and money](../../docs/handbook/dates-and-money.md).
- Test behavior through the public API, not through private fields, with one behavior per test. See [Testing](../../docs/handbook/testing.md).

## Exercises

### J02-01 · Classes and encapsulation

**Build** · 30 min · [`BankAccount.java`](src/main/java/practice/j02/ex01/BankAccount.java)

You build a bank account whose balance changes only through methods that check the rules: no negative amounts, no overdraft, no half-finished transfers. It's the smallest version of the domain objects you'll write in every backend: an object that protects its own data.

**You'll practice:** private fields, constructors and constructor chaining, a `static` counter shared by every account, choosing between `IllegalArgumentException` and `IllegalStateException`, `Math.addExact`, `toString`.

**Rules:** in the Javadoc of `BankAccount`.

<details>
<summary>Hint 1</summary>

Which fields never change after the constructor? Make them `final`. The counter for the next id belongs to the class, not to an account, so it's `static`. Take the id only after every check has passed.
</details>

<details>
<summary>Hint 2</summary>

"A failed transfer changes neither account": run every check, and compute every new balance with `Math.addExact`, before you assign anything. Inside `BankAccount` you may read and write `target.balance`, because `private` means private to the class.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: What happens if two threads open accounts at the same moment and your counter is a plain `static long`? Why doesn't `BankAccount` override `equals`?

### J02-02 · equals, hashCode and toString

**Build** · 30 min · [`Isbn.java`](src/main/java/practice/j02/ex02/Isbn.java)

An ISBN is a value object: two `Isbn` objects with the same 13 digits are the same ISBN, however someone typed them. You write `equals`, `hashCode` and `toString` so that a `HashSet` or a `HashMap` treats equal ISBNs as one. You'll rely on the same thing whenever you remove duplicates or use a value as a map key.

**You'll practice:** normalizing input in a constructor, the `equals` contract, `instanceof` with a pattern, a `hashCode` that agrees with `equals`, checking characters one by one.

**Rules:** in the Javadoc of `Isbn`.

<details>
<summary>Hint 1</summary>

Normalize in the constructor and store only the 13 digits. Then `equals`, `hashCode` and `toString` all work with one canonical value and never need to know how the input was written.
</details>

<details>
<summary>Hint 2</summary>

For the checksum, loop over the 13 characters. `c - '0'` turns the character `'7'` into the number 7, but check first that `c` is between `'0'` and `'9'`. Add the digit at even positions (0, 2, …, 12) and three times the digit at odd positions; the ISBN is valid when `sum % 10 == 0`.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: How would `Isbn` look as a record, and which part would still need code? What goes wrong in a `HashSet` if you override `equals` but not `hashCode`?

### J02-03 · Interfaces and polymorphism

**Build** · 30 min · [`Shape.java`](src/main/java/practice/j02/ex03/Shape.java), [`DiscountPolicy.java`](src/main/java/practice/j02/ex03/DiscountPolicy.java)

Two small type hierarchies show Java's two main tools. `Shape` is an interface with a default method that works for every shape, implemented by `Circle`, `Rectangle` and `Square`. `DiscountPolicy` is an abstract class whose `final` template method enforces the rules, while `FixedDiscount` and `PercentageDiscount` only decide the amount.

**You'll practice:** `implements` and `extends`, default and static interface methods, `super(...)`, `@Override`, dynamic dispatch, the template method pattern, `protected`, `Math.clamp`, formatting with `Locale.ROOT`.

**Rules:** in the Javadoc of `Shape`, `DiscountPolicy` and each class (`Circle`, `Rectangle`, `Square`, `FixedDiscount`, `PercentageDiscount`).

<details>
<summary>Hint 1</summary>

`Double.isFinite(x) && x > 0` rejects 0, negative numbers, NaN and infinity together. Why isn't `x <= 0` enough? (What is `Double.NaN <= 0`?)
</details>

<details>
<summary>Hint 2</summary>

`Square` extends `Rectangle`, so it inherits `area()` and `perimeter()`. Its constructor calls `super(side, side)`, and it overrides only `name()`. You may check `side` before calling `super(...)`, which gives a clearer error message.
</details>

<details>
<summary>Hint 3</summary>

In `apply`, `Math.clamp(value, min, max)` keeps a number in a range. `Math.multiplyExact` throws instead of overflowing.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Which of your shape classes could have been records? What would a `setWidth` method on `Rectangle` do to `Square`?

### J02-04 · Composition over inheritance

**Fix** · 30 min · [`InstrumentedSet.java`](src/main/java/practice/j02/ex04/InstrumentedSet.java)

`InstrumentedSet` counts how many elements callers try to add, for a metrics dashboard. The analytics team reports three problems:

- After an import that adds 500 products with `addAll`, the dashboard shows 1,000 attempts.
- A new `InstrumentedSet` that wraps a set of 3 products reports 6 attempts before anyone adds anything.
- The search team wraps a `TreeSet` to keep product codes sorted, but the codes come out unsorted, and the `TreeSet` itself never sees the new codes.

All three problems have the same cause: `InstrumentedSet` extends `HashSet`. Rewrite it with composition: a class that implements `Set<E>`, keeps the set it was given in a private field, and forwards every call to it. Keep the public API (the constructor and `addCount()`) so that the tests compile.

**You'll practice:** finding a self-use bug, composition and forwarding, implementing a whole interface, forwarding `equals`, `hashCode` and `toString`.

**Rules:** the intended behavior is in the Javadoc of `InstrumentedSet`.

<details>
<summary>Hint 1</summary>

Why does `addAll` count twice? Open `HashSet.addAll` in your IDE (Ctrl+click or Cmd+click on the method). It's inherited from `AbstractCollection`. Which method does it call for each element?
</details>

<details>
<summary>Hint 2</summary>

Your IDE can write the forwarding methods. In IntelliJ IDEA: Code → Generate → Delegate Methods. Then add the counting to `add` and `addAll`. Don't copy the set you're given: it must be the same object, so that changes go through.
</details>

<details>
<summary>Hint 3</summary>

`equals`, `hashCode` and `toString` come from `Object`, not from `Set`, so the compiler doesn't remind you to forward them.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Which methods would you reuse for an `InstrumentedList`? *Effective Java* (item 18) moves the forwarding into a reusable `ForwardingSet` class. When would that be worth it?

### J02-05 · Immutability, static factories and builders

**Build** · 30 min · [`Temperature.java`](src/main/java/practice/j02/ex05/Temperature.java), [`HttpRequest.java`](src/main/java/practice/j02/ex05/HttpRequest.java)

You build two immutable classes. `Temperature` is created through static factories that name the unit. `HttpRequest` is built step by step with a builder. Spring code is full of both patterns: `Duration.ofSeconds(30)`, `ResponseEntity.ok(body)`, `RestClient.builder()`.

**You'll practice:** private constructors, static factory methods, final fields, rounding before comparing, returning new objects instead of changing `this`, a static nested `Builder`, copies versus views, `toBuilder()`.

**Rules:** in the Javadoc of `Temperature` and `HttpRequest`.

<details>
<summary>Hint 1</summary>

Store the temperature as a `long` number of hundredths of a degree Celsius: `Math.round(celsius * 100)`. Comparing `long` values is exact, so `equals` and `hashCode` can't disagree. Check for NaN before you round. What is `Math.round(Double.NaN)`?
</details>

<details>
<summary>Hint 2</summary>

`Map.copyOf(map)` makes a copy; `Collections.unmodifiableMap(map)` makes a view. Which one keeps a built request unchanged when someone keeps using the builder? And `toBuilder()` needs a new builder with its own, modifiable map.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Why can't `Temperature` have the two constructors `Temperature(double celsius)` and `Temperature(double fahrenheit)`? What could a caller do if `headers()` returned the builder's map?

### J02-06 · Enums with behavior

**Build** · 30 min · [`OrderStatus.java`](src/main/java/practice/j02/ex06/OrderStatus.java), [`Operation.java`](src/main/java/practice/j02/ex06/Operation.java), [`OrderStatistics.java`](src/main/java/practice/j02/ex06/OrderStatistics.java)

Java enums are full classes. You give `OrderStatus` the rules of an order's life cycle, give `Operation` a symbol and the arithmetic behind it, and count orders per status with an `EnumMap`. A small state machine like `OrderStatus` guards the order flow of the bookstore in Part 2.

**You'll practice:** enum fields and constructors, an exhaustive `switch` over an enum, `values()`, looking up a constant by a field, exact arithmetic, `EnumMap`.

**Rules:** in the Javadoc of each of the three types.

<details>
<summary>Hint 1</summary>

`PLACED(PAID, CANCELLED)` doesn't compile, because `PAID` doesn't exist yet when `PLACED` is created. Return the allowed statuses from a method instead, with a `switch (this)` expression that has one case per constant.
</details>

<details>
<summary>Hint 2</summary>

`Math.addExact`, `subtractExact`, `multiplyExact` and `divideExact` throw `ArithmeticException` where the operators would overflow. For the counts, put a 0 for every status into the `EnumMap` first. `Map.copyOf` would lose the order, but `Collections.unmodifiableMap` keeps it.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Which lines must change if you add a `RETURNED` status, and how do you find them? Why is `next.ordinal() > ordinal()` a bad way to decide which moves are allowed?

### J02-07 · Records, sealed types and pattern matching

**Build** · 60 min · [`PaymentMethod.java`](src/main/java/practice/j02/ex07/PaymentMethod.java), [`FeeCalculator.java`](src/main/java/practice/j02/ex07/FeeCalculator.java), [`Expr.java`](src/main/java/practice/j02/ex07/Expr.java)

You model data the modern Java way: a sealed interface whose records are the only possible cases, and `switch` expressions that the compiler checks for completeness. First come payment methods and their fees, with validation in compact constructors and `when` guards. Then you evaluate and simplify a small expression tree with nested record patterns.

**You'll practice:** records and compact constructors, sealed interfaces, pattern matching `switch`, guards, record patterns and nested patterns, the unnamed pattern `_`, recursion over a tree.

**Rules:** in the Javadoc of `PaymentMethod` (and its records), `FeeCalculator` and `Expr`.

<details>
<summary>Hint 1</summary>

In a compact constructor, assign the cleaned value back to the parameter: `accountNumber = digits;`. The record stores whatever the parameter holds at the end of the constructor.
</details>

<details>
<summary>Hint 2</summary>

Guarded cases come first: `case Card(_, boolean international) when international -> ...` before `case Card _ -> ...`. A guarded case can also throw: `case Points(long balance) when balance < amount -> throw new IllegalArgumentException(...);`.
</details>

<details>
<summary>Hint 3</summary>

Write `simplify()` in two steps. First rebuild the node from its simplified children. Then `switch` over that new node with nested patterns, such as `case Add(Expr left, Num(long value)) when value == 0 -> left`.
</details>

**Done?** Compare with the reference on the `solutions` branch, then try this: add a fourth payment method, `Voucher`, to `PaymentMethod`. Which lines does the compiler point you to? Why would a `default` branch have hidden them?

## Review questions

1. What's the difference between `==` and `equals` for objects? Which classes should override `equals`, and which shouldn't?
2. Why must `hashCode` change whenever `equals` changes? What goes wrong in a `HashSet` otherwise?
3. When do you throw `IllegalArgumentException`, and when `IllegalStateException`?
4. A field has no access modifier. Who can read it?
5. When would you choose an interface, and when an abstract class?
6. What goes wrong when a subclass overrides methods that the superclass calls itself? How does composition avoid it?
7. Why is `private final List<String> items` not immutable? How do you make it immutable?
8. What does a record generate for you, and what doesn't it protect you from?
9. Why should a `switch` over a sealed interface have no `default` branch?
10. What's the difference between a static nested class and an inner class, and why is `static` the safer default?
11. Why is `ordinal()` risky in business logic?
12. A `User` record and a `Customer` record have exactly the same components. Can you pass a `User` where a `Customer` is expected? Would TypeScript allow it?

<details>
<summary>Answers</summary>

1. `==` asks whether two references point to the same object; `equals` asks whether two objects have the same value. Value objects (an ISBN, money, a date) override `equals`. Entities with their own identity (a bank account, an order) keep the inherited identity-based `equals`.
2. `HashSet` and `HashMap` use `hashCode` to choose the bucket and `equals` only inside it. If two equal objects have different hash codes, the set looks in the wrong bucket: it keeps duplicates and fails to find elements that are there.
3. `IllegalArgumentException` when the argument is wrong no matter what (a negative amount). `IllegalStateException` when the argument would be fine, but the object's current state doesn't allow the call (withdrawing more than the balance).
4. Every class in the same package. It's package-private, not public as in TypeScript.
5. An interface for a capability that unrelated classes share, or when a class needs several of them; it also lets you swap implementations, for example in tests. An abstract class to share fields and code between closely related classes, often with a final template method.
6. The subclass's overridden method runs inside the superclass's own calls (self-use), so one action can be counted or handled twice, and a new version of the superclass can change the behavior. Composition calls the wrapped object only through its public interface, so its internal calls never reach your code.
7. `final` only stops you from assigning another list to `items`; the list itself can still change. Store an unmodifiable copy (`List.copyOf(...)`) and never hand out a modifiable list.
8. Private final fields, a canonical constructor, accessors, `equals`, `hashCode` and `toString`. It doesn't make mutable components (such as a `List`) immutable, and it doesn't validate anything unless you write a compact constructor.
9. Without `default`, the compiler checks that every permitted type is handled. When you add a new type, every such `switch` stops compiling and points you to the code you must update. A `default` branch would quietly handle the new type in a way nobody chose.
10. A static nested class has no link to an outer object. An inner class holds a hidden reference to one (`Outer.this`), which costs memory and can keep the outer object alive longer than expected. Use an inner class only when you need the outer object.
11. `ordinal()` is the position in the declaration. Adding or reordering constants silently changes it, and every piece of logic built on it. Keep the information in a field or a `switch`.
12. No. Java types are nominal: `User` and `Customer` are unrelated unless one is declared as the other. TypeScript's structural typing would allow it, because the shapes match.
</details>

## Further reading

- Official docs: [Java 25 API](https://docs.oracle.com/en/java/javase/25/docs/api/) · [dev.java/learn](https://dev.java/learn/) · [The Java Language Specification, Java SE 25](https://docs.oracle.com/javase/specs/jls/se25/html/index.html)
- dev.java tutorials: [Classes and objects](https://dev.java/learn/language/oop/classes-objects/) · [Inheritance](https://dev.java/learn/language/oop/inheritance/) · [Interfaces](https://dev.java/learn/language/oop/interfaces/) · [Records](https://dev.java/learn/language/oop/records/) · [Enums](https://dev.java/learn/language/oop/classes-objects/enums/) · [Nested classes](https://dev.java/learn/language/oop/classes-objects/nested-classes/)
- The JEPs behind the modern features: [395 Records](https://openjdk.org/jeps/395) · [409 Sealed classes](https://openjdk.org/jeps/409) · [440 Record patterns](https://openjdk.org/jeps/440) · [441 Pattern matching for switch](https://openjdk.org/jeps/441) · [456 Unnamed variables and patterns](https://openjdk.org/jeps/456) · [513 Flexible constructor bodies](https://openjdk.org/jeps/513)
- Joshua Bloch, *Effective Java*, 3rd edition: items 1 (static factories), 2 (builders), 10–12 (`equals`, `hashCode`, `toString`), 17 (immutability), 18 (composition over inheritance), 19 (design for inheritance or prohibit it), 24 (static nested classes), 34–37 (enums).
