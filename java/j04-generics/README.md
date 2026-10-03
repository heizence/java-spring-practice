# J04 · Generics

⏱ 3 sessions · week 3 · 3 exercises

Generics let you write a class or a method once and use it with many types, while the compiler still checks every use. Every collection is generic, and so are `Optional`, streams and most of Spring: `JpaRepository<Book, Long>`, `ResponseEntity<BookResponse>`, `Page<Book>`. In this module you learn to write your own generic classes and methods, to choose wildcards with the PECS rule, to read signatures like `<T extends Comparable<? super T>>` without fear, and to work around type erasure, the place where Java's generics surprise people most.

## Lesson

| Session | Read | Then do |
|---|---|---|
| 1 | 1. Why generics · 2. Generic classes and methods · 3. Lambdas and `Function` | J04-01 |
| 2 | 4. Bounded type parameters · 5. Wildcards and PECS | J04-02 |
| 3 | 6. Type erasure · 7. Raw types · 8. TypeScript and Java | J04-03 |

### 1. Why generics exist

Before Java 5 (2004), a collection held plain `Object`s:

```java
List names = new ArrayList();              // a list of anything
names.add("Minji");
names.add(42);                             // compiles: anything goes in
String second = (String) names.get(1);     // compiles, then throws ClassCastException at runtime
```

With generics, the element type becomes part of the list's type:

```java
List<String> names = new ArrayList<>();
names.add("Minji");
names.add(42);                             // compile error: 42 isn't a String
String first = names.get(0);               // no cast needed
```

The compiler reports the mistake on the line where it happens, instead of a `ClassCastException` somewhere else, later, in production. Moving type errors from runtime to compile time is the whole point, in Java as in TypeScript.

### 2. Generic classes, records and methods

A generic class or record declares *type parameters* in angle brackets after its name, and then uses them like types:

```java
public record Box<T>(T value) {

    public boolean holds(T other) {
        return value.equals(other);
    }
}

Box<String> greeting = new Box<>("hello");
String text = greeting.value();            // no cast
```

`T` is a type parameter. `String` in `Box<String>` is a *type argument*. By convention, type parameters are single capital letters: `T` for a type, `E` for an element, `K` and `V` for a key and a value, `R` for a result.

A generic *method* declares its own type parameters, in front of the return type:

```java
static <T> T firstOrDefault(List<T> list, T fallback) {
    return list.isEmpty() ? fallback : list.getFirst();
}

String name = firstOrDefault(names, "nobody");          // T is String
var none = Collections.<String>emptyList();             // an explicit type argument, rarely needed
```

The compiler *infers* the type arguments from the arguments and from where the result goes, so you almost never write them. A `static` method can't use the type parameters of its class, because those belong to an instance: one `Box<String>`, another `Box<Integer>`. A static method declares its own.

**The diamond.** In `Map<String, List<Integer>> stock = new HashMap<>();`, the empty `<>` (the *diamond*) tells the compiler to infer the type arguments from the variable. Careful with `var`: in `var names = new ArrayList<>();` there's nothing to infer from, so `names` becomes an `ArrayList<Object>`. With `var`, write the type argument: `var names = new ArrayList<String>();`.

**No primitives.** Type arguments must be reference types: `List<Integer>`, not `List<int>`. The values are boxed automatically.

### 3. Lambdas and `Function<T, R>`

J04 uses `Function<T, R>`, the JDK's interface for a function from `T` to `R`. It's the Java version of the TypeScript type `(t: T) => R`. You pass a *lambda* or a *method reference* where a `Function` is expected, and call it with `apply`:

```java
Function<String, Integer> length = text -> text.length();   // a lambda, like text => text.length
Function<String, Integer> sameThing = String::length;        // a method reference: the same function
int five = length.apply("hello");
```

`Predicate<T>` is a function from `T` to `boolean`, called with `test`. J06 covers lambdas and the other functional interfaces in depth; for this module, that's all you need.

### 4. Bounded type parameters

A *bound* limits which types are allowed, and in exchange lets you call the bound's methods:

```java
static <T extends Number> double total(List<T> values) {
    double sum = 0;
    for (T value : values) {
        sum += value.doubleValue();          // allowed: every T is a Number
    }
    return sum;
}
```

Here `extends` means "is a subtype of", for classes and interfaces alike. Several bounds are joined with `&`: `<T extends Number & Comparable<T>>`.

The bound you'll meet most is `Comparable`, which means "can be put in order":

```java
static <T extends Comparable<T>> T larger(T a, T b) {
    return a.compareTo(b) >= 0 ? a : b;
}
```

Read it as "T must be comparable to T". That works for `String`, `Integer` and most types. Now the scary-looking version, from `java.util.Collections`:

```java
static <T extends Comparable<? super T>> void sort(List<T> list)
```

`Comparable<? super T>` means "comparable to T, *or to a supertype of T*" (section 5 explains `? super`). Why would anyone need that? Because some classes inherit their `compareTo` from a supertype. `LocalDate` implements the interface `ChronoLocalDate`, and `ChronoLocalDate` is a `Comparable<ChronoLocalDate>`. So a `LocalDate` can be compared to other dates, but it is *not* a `Comparable<LocalDate>`. With the strict bound `<T extends Comparable<T>>`, the line `LocalDate later = larger(today, tomorrow);` doesn't compile. With `<T extends Comparable<? super T>>`, it does. When you write a method that compares `T` values, use the second form.

### 5. Wildcards and PECS

Generic types are *invariant*: a `List<Integer>` is not a `List<Number>`, even though every `Integer` is a `Number`. If it were, this would compile:

```java
List<Integer> integers = new ArrayList<>();
List<Number> numbers = integers;           // compile error, because of the next two lines:
numbers.add(3.14);                         // a Double in a list of Integers
Integer first = integers.get(0);           // ClassCastException
```

*Wildcards* give back flexibility, safely. A `?` stands for "some type that I don't know exactly":

| Parameter type | Accepts | Inside the method you can | But you can't |
|---|---|---|---|
| `List<? extends Number>` | `List<Number>`, `List<Integer>`, `List<Double>` | read elements as `Number` | add elements (except `null`) |
| `List<? super Integer>` | `List<Integer>`, `List<Number>`, `List<Object>` | add `Integer`s | read elements as anything but `Object` |
| `List<?>` | any list | read elements as `Object` | add elements (except `null`) |

Which wildcard do you need? Remember **PECS: Producer `extends`, Consumer `super`.** Look at each parameter from inside the method:

- If the method only *takes `T` values out* of it, the parameter *produces* them: use `? extends T`.
- If the method only *puts `T` values in*, the parameter *consumes* them: use `? super T`. Functions you pass values to are consumers too: a `Comparator<T>` receives `T` values in `compare`, and a `Predicate<T>` in `test`.
- If the method does both, use plain `T`.

The JDK follows the rule everywhere, which makes its signatures look complicated and its methods easy to call:

```java
// java.util.Collections
static <T> void copy(List<? super T> dest, List<? extends T> src)
static <T> T max(Collection<? extends T> coll, Comparator<? super T> comp)
// java.util.List
boolean addAll(Collection<? extends E> c)
// java.lang.Iterable
void forEach(Consumer<? super T> action)
```

Two more guidelines. Put wildcards in parameters, not in return types: a caller who gets back a `List<? extends Number>` can't add to it and has to deal with the `?`. And when a type parameter would appear only once in a signature, a wildcard says the same thing with less noise: `double sum(Collection<? extends Number> numbers)` instead of `<T extends Number> double sum(Collection<T> numbers)`.

### 6. Type erasure

Java checks generic types at compile time and then *erases* them: in the bytecode, every type parameter becomes its bound (`Object` if it has none), and the compiler inserts casts where values come out. At runtime, a `List<String>` and a `List<Integer>` are both a plain `ArrayList`. Erasure kept generics compatible with all the code written before Java 5, but it means that some things can't work:

```java
class Box<T> {
    T create() { return new T(); }                          // error: T is unknown at runtime
    T[] many() { return new T[10]; }                        // error: generic array creation
    boolean holds(Object o) { return o instanceof T; }      // error
}

obj instanceof List<String> list                            // error: at runtime it's only a List;
                                                            //   obj instanceof List<?> list is fine
List<String>.class                                          // error: there is only List.class
void print(List<String> names) { }                          // error: these two methods have
void print(List<Integer> numbers) { }                       //   the same erasure, print(List)
```

The way around it is to pass the type explicitly, as a *type token*: a `Class<T>` object, which does exist at runtime. `Class<T>` has the runtime versions of `instanceof` and of a cast:

```java
static <T> T convert(Object value, Class<T> type) {
    if (!type.isInstance(value)) {                  // like value instanceof T
        throw new IllegalArgumentException(value + " is not a " + type.getSimpleName());
    }
    return type.cast(value);                        // a checked cast to T
}

static <T> T create(Class<T> type) throws ReflectiveOperationException {
    return type.getDeclaredConstructor().newInstance();  // like new T()
}
```

For arrays, pass a constructor reference instead: `names.toArray(String[]::new)`.

A `Class` object can't describe `List<String>`, only `List`. Libraries get around that with a trick: erasure removes type arguments from *objects*, but not from *class declarations*. When you write `new TypeReference<List<Book>>() { }`, you create an anonymous subclass whose declaration says `extends TypeReference<List<Book>>`. The class file keeps that, and the library reads it back with reflection. You'll use this as Jackson's `TypeReference` and Spring's `ParameterizedTypeReference`. The predictions in J04-03 show it in action.

### 7. Raw types

A *raw type* is a generic type used without type arguments: `List` instead of `List<String>`. Raw types exist only so that code from before 2004 still compiles. A raw type switches the type checks off, and the compiler warns about it (*found raw type*, *unchecked call*). Worse, it lets a value of the wrong type into a generic collection, which is called *heap pollution*, and the program fails much later, at a cast the compiler inserted:

```java
List<String> names = new ArrayList<>();
List raw = names;                 // warning: found raw type: List
raw.add(42);                      // warning: unchecked call. The Integer goes in.
String first = names.get(0);      // ClassCastException, far from the real mistake
```

Never write raw types in new code. When you don't know or care about the element type, write `List<?>`: it accepts every list and keeps the compiler on your side. Fix every *unchecked* warning. If one can't be fixed, suppress it with `@SuppressWarnings("unchecked")` on the smallest possible scope, with a comment that explains why it's safe.

### 8. Generics in TypeScript and Java

The syntax looks almost the same, so the differences are easy to miss:

- **Both erase, differently.** TypeScript erases all types. Java erases type arguments but keeps classes: `x instanceof String` works, `x instanceof List<String>` doesn't. Java also keeps generic declarations in class files, where reflection can read them (section 6).
- **Structural versus nominal.** In TypeScript, `<T extends { name: string }>` accepts any object that has a `name`. In Java, `<T extends Named>` accepts only types that declare `implements Named`.
- **Variance.** TypeScript lets you assign a `Dog[]` to an `Animal[]` and then push a `Cat` into it: convenient, but not type-safe, on purpose. Java's generics are invariant, and you choose the variance each time you *use* a type, with `? extends` and `? super`. TypeScript has nothing like `? super` at the use site. (Since TypeScript 4.7 you can mark a type parameter `in` or `out` where you *declare* it; Java has no such declaration-site variance.)
- **What TypeScript has and Java doesn't:** conditional types, mapped types, `keyof`, literal types, default type arguments (`<T = string>`).
- **Arrays.** Java arrays are covariant like TypeScript arrays, but the JVM checks every store at runtime: putting an `Integer` into an `Object[]` that is really a `String[]` throws `ArrayStoreException`. Arrays and generics follow different rules, which is why there are no generic arrays.

## TypeScript → Java

| TypeScript | Java | Watch out for |
|---|---|---|
| `function first<T>(xs: T[]): T` | `static <T> T first(List<T> xs)` | the type parameters come before the return type |
| `class Box<T> { }` | `class Box<T> { }` or `record Box<T>(T value) { }` | |
| `<T extends HasName>` | `<T extends HasName>` | `T` must *declare* `implements HasName` |
| `Array<number>` | `List<Integer>` | no primitives as type arguments |
| `readonly Animal[]` (accepts a `Dog[]`) | `List<? extends Animal>` | a `List<Dog>` isn't a `List<Animal>` |
| (none) | `List<? super Dog>` | |
| `unknown[]` | `List<?>` | |
| `any[]` | raw `List` | never in new code |
| `<T = string>` | (none) | |
| `value as T` | `type.cast(value)`, or `(T) value` | `(T) value` checks nothing at runtime and causes an *unchecked* warning |
| `(t: T) => R` | `Function<T, R>` | call it with `apply` |
| `x instanceof Foo` | `x instanceof Foo` | `x instanceof List<String>` doesn't compile |

## In real projects

- **Accept general types, return specific ones.** Use wildcards in parameters (PECS) and interfaces such as `List` or `Collection` instead of `ArrayList`. Return plain types without wildcards. → [Collections and generics](../../docs/handbook/collections-and-generics.md)
- **No raw types, no ignored warnings.** Treat *unchecked* warnings like errors. Suppress one only on the smallest scope, with a comment that says why it's safe. → [Collections and generics](../../docs/handbook/collections-and-generics.md)
- **Prefer `List<T>` to `T[]` in your APIs.** Arrays and generics don't mix well: there are no generic arrays, and array covariance is only checked at runtime. → [Collections and generics](../../docs/handbook/collections-and-generics.md)
- **Generic value types are immutable records** that copy the collections they receive, like `Pair` and `Page` in J04-01. → [Immutability](../../docs/handbook/immutability.md)
- **A lookup returns `Optional<T>`, never `null`**, like `TypedMap.get` in J04-03. → [Null and Optional](../../docs/handbook/null-and-optional.md)
- **A mapper parameter is a `Function<? super T, ? extends R>`.** `Optional.map`, `Stream.map` and Spring's `Page.map` all take one. → [Functional style](../../docs/handbook/functional-style.md)

## Exercises

### J04-01 · Generic classes and methods

**Build** · 30 min · [`Pair.java`](src/main/java/practice/j04/ex01/Pair.java) · [`Page.java`](src/main/java/practice/j04/ex01/Page.java) · [`ListHelpers.java`](src/main/java/practice/j04/ex01/ListHelpers.java)

Three generic building blocks that backend code is full of: a `Pair` of two values, a `Page` of results like the one Spring Data returns for a paginated query, and two generic helpers for lists. The signatures are given; you make them work, including at the edges, where page numbers come from a URL and anyone can send `?page=2147483647`.

**You'll practice:** generic records, compact constructors that check their arguments, generic methods, `Function<T, R>`, type inference and the diamond, defensive copies with `List.copyOf`, overflow at the edges of `int` and `long`.

**Rules:** in the Javadoc of `Pair`, `Page` and `ListHelpers`.

<details>
<summary>Hint 1</summary>

In a record's compact constructor, `public Pair { ... }`, you check the parameters, and the record assigns the fields after your code runs. You can also replace a parameter there: `items = List.copyOf(items);` makes the record store the copy.
</details>

<details>
<summary>Hint 2</summary>

`Math.ceilDiv(a, b)` divides and rounds up. What happens to the well-known `(a + b - 1) / b` when `a` is close to `Long.MAX_VALUE`? And what is `number + 1` when `number` is `Integer.MAX_VALUE`?
</details>

<details>
<summary>Hint 3</summary>

`list.subList(from, to)` doesn't copy anything: it's a view of the original list.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Is there any way to change the items of a `Page` after it was created? Why do `mapFirst` and `map` take a `Function<? super T, ? extends R>` rather than a `Function<T, R>`? (After the next session, you'll know.)

### J04-02 · Bounded types and wildcards

**Build** · 30 min · [`CollectionTools.java`](src/main/java/practice/j04/ex02/CollectionTools.java)

Four utilities in the style of `java.util.Collections`, done in two steps. First you make them work. Then you make their signatures as flexible as the JDK's: the given signatures compile, but they reject calls that would be perfectly safe, such as summing a `List<Integer>` or clamping a `LocalDate`.

**You'll practice:** bounded type parameters, `? extends` and `? super`, the PECS rule, `<T extends Comparable<? super T>>`, `Comparator` and `Predicate` as consumers.

**Rules:** in the Javadoc of `CollectionTools`. In the signatures, change only type arguments and the bound of `T`. Keep the method names, the name `T`, the order of the parameters, the container types and the return types.

**How the signatures are checked:** a test that called a method in a way that only compiles with the flexible signature would break the whole module until your signature was right, and every test would fail. So the behavior tests only make calls that compile with both signatures, and the tests in the **Signatures** group read your signatures through reflection. Their failure messages tell you which parameter is still too strict, and why. Get the behavior tests green first.

<details>
<summary>Hint 1</summary>

For each parameter, ask one question: does the method take `T` values out of it, or put `T` values into it?
</details>

<details>
<summary>Hint 2</summary>

`Comparator<T>` and `Predicate<T>` receive `T` values in `compare` and `test`. That makes them consumers.
</details>

<details>
<summary>Hint 3</summary>

For `clamp`, the bound you need is the same as the one on `Collections.sort` (section 4).
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Why can't `copyIf` declare `target` as a `List<? extends T>`? Could `ListHelpers.firstOrDefault` from J04-01 use a wildcard, and which calls would that allow?

### J04-03 · Type erasure and type tokens

**Build** · 30 min · [`TypedMap.java`](src/main/java/practice/j04/ex03/TypedMap.java) · predictions in [`ErasureAnswers.java`](src/main/java/practice/j04/ex03/ErasureAnswers.java)

A map whose keys are types: `put(String.class, "Minji")`, and `get(String.class)` gives back an `Optional<String>`, without a cast. It works because a `Class<T>` carries its type to runtime, where erasure has removed everything else. Spring's `getBean(Class<T>)` uses a type token in the same way, and Guava's `ClassToInstanceMap` is the same container. The second part of the session is a short Predict tier on what erasure keeps and what it throws away.

**You'll practice:** `Class<T>` as a type token, `Class.cast` and `Class.isInstance`, `Map<Class<?>, Object>`, `Optional`, primitive and array classes, raw types and heap pollution.

**Rules:** in the Javadoc of `TypedMap`. For the predictions, read each method of [`ErasurePuzzles`](src/main/java/practice/j04/ex03/ErasurePuzzles.java) without running it, write your prediction in `ErasureAnswers`, then run `TypedMapTest`. The tests of the **Erasure predictions** group compare your answers with what the code really does.

<details>
<summary>Hint 1</summary>

Store the values in a `Map<Class<?>, Object>`. To turn an `Object` from the map back into a `T` without an unchecked cast, use `type.cast(...)`.
</details>

<details>
<summary>Hint 2</summary>

`Class` has a method `isPrimitive()`. And `Set.copyOf` makes an unmodifiable snapshot.
</details>

<details>
<summary>Explanations of the erasure predictions (open after you've run the tests)</summary>

- **`new ArrayList<String>().getClass() == new ArrayList<Integer>().getClass()` is `true`.** After erasure, both are a plain `ArrayList`: one class at runtime, whatever the type argument.
- **Adding `42` through a raw `List`, then `size()`, gives `1`.** The raw type switches off the compiler's checks, and at runtime the list holds `Object`s, so nothing stops the `Integer`.
- **Reading that element into a `String` throws `ClassCastException`.** The compiler inserted a hidden `(String)` cast where the element is read. The program fails there, far away from the line that caused the problem. This is heap pollution, and it's why `TypedMap.put` checks its values with `type.cast` right away.
- **Storing `42` in an `Object[]` that is really a `String[]` throws `ArrayStoreException`.** Arrays are *reified*: they know their element type at runtime, so the JVM checks every store. Generic collections can't do that, because of erasure.
- **`int.class.isInstance(42)` is `false`.** `42` is boxed to an `Integer` first, and nothing is ever an instance of a primitive type. That's why `TypedMap` rejects `int.class` and points to `Integer.class`.
- **The generic superclass of `new ArrayList<String>() { }` does mention `String`: its type name is `"java.util.ArrayList<java.lang.String>"`.** Erasure removes type arguments from objects, but not from class declarations. The anonymous class was declared as a subclass of `ArrayList<String>`, and its class file remembers that. Jackson's `TypeReference` and Spring's `ParameterizedTypeReference` are built on exactly this.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Why does your `get` need no `@SuppressWarnings`? What does `get(List.class)` return, and why can't a `TypedMap` keep a `List<String>` and a `List<Integer>` apart?

## Review questions

1. Why isn't a `List<Integer>` a `List<Number>`? What could go wrong if it were?
2. A method reads elements from one list and adds them to another. Which wildcard goes on each parameter?
3. Why do `Comparator` parameters in the JDK usually say `Comparator<? super T>`?
4. Why does `Collections.sort` declare `<T extends Comparable<? super T>>` instead of `<T extends Comparable<T>>`?
5. Why can't you write `new T()`, and what do you do instead?
6. What's wrong with `List list = new ArrayList<String>();`? What do you write when you don't know the element type?
7. Arrays are covariant and generics are invariant. Which of them catches a value of the wrong type, and when?
8. Name one thing Java generics can express that TypeScript generics can't, and one the other way round.

<details>
<summary>Answers</summary>

1. Generic types are invariant. If a `List<Integer>` were a `List<Number>`, code holding it as a `List<Number>` could add a `Double`, and the code holding it as a `List<Integer>` would later get a `ClassCastException`.
2. The list you read from produces elements: `List<? extends T>`. The list you add to consumes them: `List<? super T>`. Producer `extends`, consumer `super`.
3. A comparator only receives `T` values, so it's a consumer. With `? super T`, a comparator written for a supertype, such as a `Comparator<Number>` or a `Comparator<Object>`, also works for a `List<Integer>`.
4. Because some types inherit `compareTo` from a supertype. `LocalDate` is a `Comparable<ChronoLocalDate>`, not a `Comparable<LocalDate>`, so the strict bound would reject it. `Comparable<? super T>` accepts a `Comparable` of `T` or of any supertype of `T`.
5. Type erasure: at runtime there is no `T` to create. Pass a type token (`Class<T>`, then `type.getDeclaredConstructor().newInstance()`) or, better, a factory such as a `Supplier<T>` or a constructor reference like `ArrayList::new`.
6. It's a raw type: the compiler stops checking what goes into the list, and a wrong element fails later, at a hidden cast. Write `List<?>` when you don't know or care about the element type.
7. Arrays check every store at runtime and throw `ArrayStoreException`. Generics catch the mistake at compile time, because a `List<Integer>` isn't a `List<Number>`. Raw types and unchecked casts can get around that check, and then nothing catches the mistake until a later read fails.
8. Java: use-site variance with `? super` (TypeScript has no equivalent), and reading generic declarations at runtime through reflection. TypeScript: structural typing, conditional and mapped types, `keyof`, and default type arguments.
</details>

## Further reading

- Official docs: [Generics on dev.java](https://dev.java/learn/generics/) · [The Java Tutorials: Generics](https://docs.oracle.com/javase/tutorial/java/generics/index.html) · [JLS §4.5 Parameterized types and §4.6 Type erasure](https://docs.oracle.com/javase/specs/jls/se25/html/jls-4.html#jls-4.5)
- Joshua Bloch, *Effective Java*, 3rd edition, chapter 5 (items 26–33), especially item 26 "Don't use raw types", item 31 "Use bounded wildcards to increase API flexibility" (where PECS comes from) and item 33 "Consider typesafe heterogeneous containers" (`TypedMap`).
- Angelika Langer's [Java Generics FAQ](http://www.angelikalanger.com/GenericsFAQ/JavaGenericsFAQ.html): the answer to almost every "why doesn't this compile?" question.
- For comparison: [Generics in the TypeScript handbook](https://www.typescriptlang.org/docs/handbook/2/generics.html).
