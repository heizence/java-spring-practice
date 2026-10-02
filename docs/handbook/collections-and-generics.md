# Collections and generics

## Declare the interface, choose the implementation

```java
List<Order> orders = new ArrayList<>();              // not: ArrayList<Order> orders = …
Map<String, Customer> byEmail = new HashMap<>();
```

Callers depend only on what they need (a `List`), so you can change the implementation later without touching them. The same goes for method parameters and return types: accept and return `List`, `Set`, `Map`, `Collection`, never a specific implementation unless its behavior matters.

## Which implementation?

| You need | Use | Notes |
|---|---|---|
| An ordered list (the default) | `ArrayList` | Fast index access. `LinkedList` is almost never faster in practice. |
| Unique values, any order | `HashSet` | Iteration order is unspecified. Never rely on it. |
| Unique values in insertion order | `LinkedHashSet` | |
| Unique values, sorted | `TreeSet` | Also gives `first()`, `floor()`, `headSet()` … |
| Key → value (the default) | `HashMap` | |
| Key → value in insertion (or access) order | `LinkedHashMap` | Access order makes LRU caches easy (J05-09). |
| Key → value, sorted by key, range queries | `TreeMap` | `floorEntry`, `ceilingKey`, `subMap` … |
| Enum keys | `EnumMap`, `EnumSet` | Faster and ordered by declaration. |
| Stack (LIFO) or queue (FIFO) | `ArrayDeque` | Don't use the legacy `Stack` or `Vector`. |
| "Always give me the smallest" | `PriorityQueue` | A binary heap. Iteration order is *not* sorted. |
| Shared between threads | `ConcurrentHashMap`, `CopyOnWriteArrayList`, `BlockingQueue` | See [Concurrency](concurrency.md). |

## Immutable by default

- `List.of(…)`, `Set.of(…)`, `Map.of(…)` create **unmodifiable** collections. They reject `null` elements.
- `List.copyOf(collection)` makes an unmodifiable **copy**. Use it for defensive copies in constructors and getters.
- `Collections.unmodifiableList(list)` is a read-only **view**: you can't change it, but changes to `list` show through it.
- `stream.toList()` returns an unmodifiable list.

## Rules

- **Return an empty collection, never `null`.**
- **Don't expose internal mutable collections.** Return a copy or an unmodifiable view, or the caller can change your object behind your back.
- **Don't modify a collection while iterating over it** with a for-each loop. It throws `ConcurrentModificationException`. Use `removeIf(…)`, an `Iterator`'s `remove()`, or build a new collection.
- **Keys need correct `equals` and `hashCode`**, and must not change while they're in a map or set (see [Immutability](immutability.md)).
- **Watch out for `List<Integer>.remove(1)`.** It removes the element at *index* 1. To remove the *value* 1, write `remove(Integer.valueOf(1))`.
- **Java 21 sequenced collections** add `getFirst()`, `getLast()`, `addFirst()`, `removeLast()` and `reversed()` to lists, deques and ordered sets and maps. Prefer them over `list.get(list.size() - 1)`.

## Generics

- **Never use raw types.** `List list = new ArrayList();` turns off type checking. Always write the type argument: `List<String>`.
- **Wildcards follow PECS: "Producer `extends`, Consumer `super`."**
  - A parameter you only **read from** (it *produces* values for you): `Collection<? extends Number>`. Callers can pass a `List<Integer>` or a `List<Double>`.
  - A parameter you only **write into** (it *consumes* your values): `List<? super Integer>`. Callers can pass a `List<Integer>`, `List<Number>` or `List<Object>`.
  - A parameter you read and write: no wildcard.
  ```java
  static <T> void copy(List<? super T> destination, List<? extends T> source) { … }
  static <T> T max(Collection<? extends T> items, Comparator<? super T> comparator) { … }
  ```
- **Don't put wildcards in return types.** They force every caller to deal with them.
- **Bounded type parameters** say what a type must support: `<T extends Comparable<? super T>>` means "any T that can be compared with itself or a supertype".
- **Generics are erased at runtime.** Code can't do `new T()`, `T.class` or `instanceof List<String>`. When you need the type at runtime, pass a `Class<T>` (a "type token").
