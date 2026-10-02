# J05 · Collections and data structures

⏱ 10 sessions · weeks 3–4 · 9 exercises

Almost every backend method takes, builds or returns a collection: the rows of a query, the items of an order, a cache of prices, a queue of jobs. This module teaches the Java Collections Framework the way you use it at work: which interface to declare, which class to pick, what each operation costs, and which traps cause real bugs. You also build small versions of the classic data structures (a growing array, a linked stack, a hash table, a search tree, a heap and a graph) to see how they work inside. The goal isn't coding-test practice. It's knowing *why* `HashMap.get` is fast, when a `TreeMap` or a `PriorityQueue` is the right tool, and why changing a map key loses your data.

## Lesson

### 1. The map of the Collections Framework

A *collection* is an object that holds other objects, its *elements*. Java's collections live in the package `java.util`. A few **interfaces** describe what a collection can do, and several **implementations** (classes) do it in different ways:

```
Iterable                    anything a for-each loop can walk through
└── Collection              a group of elements
    ├── List                ordered, indexed, duplicates allowed          ArrayList, LinkedList
    ├── Set                 no duplicates                                 HashSet, LinkedHashSet, TreeSet
    └── Queue               elements wait to be processed                 PriorityQueue, ArrayDeque
        └── Deque           add and remove at both ends                   ArrayDeque, LinkedList

Map                         keys mapped to values (not a Collection)      HashMap, LinkedHashMap, TreeMap
```

- A **`List`** keeps its elements in the order you add them, and you can read them by index. It's the closest thing to a TypeScript array.
- A **`Set`** holds each element at most once. `HashSet` has no defined order, `LinkedHashSet` keeps the insertion order, and `TreeSet` keeps its elements sorted.
- A **`Queue`** hands out its elements in a fixed order. Most queues are first in, first out (FIFO). A `PriorityQueue` hands out the smallest element first.
- A **`Deque`** (say "deck", short for double-ended queue) adds and removes at both ends, so it works as a queue *and* as a stack.
- A **`Map`** stores values under keys, like a TypeScript `Map`. It isn't a `Collection`, but it gives you collection views of its contents: `keySet()`, `values()` and `entrySet()`.

Sorted collections have extra interfaces: `SortedSet` and `NavigableSet` (implemented by `TreeSet`), `SortedMap` and `NavigableMap` (implemented by `TreeMap`). Section 11 shows the *sequenced* interfaces that Java 21 added.

### 2. Program to interfaces

Declare variables, fields, parameters and return types with the interface. Name the implementation only where you create the object:

```java
List<Order> orders = new ArrayList<>();
Map<String, Integer> stockBySku = new HashMap<>();

public List<Order> openOrders(Customer customer) { ... }   // callers get a List, not an ArrayList
```

This has three benefits:

- You can change the implementation later by changing one line, for example from `HashMap` to `LinkedHashMap` to get a stable order.
- Callers depend only on what they need. A method that only loops over its argument can accept a `Collection<Order>`, or even an `Iterable<Order>`.
- You can return any implementation, including the unmodifiable lists of `List.of` (section 10).

Use a more specific type when callers rely on the extra behavior: `NavigableMap` when they call `floorKey`, `Deque` for a stack. Inside a method, `var orders = new ArrayList<Order>();` is fine, because a local variable's type doesn't leak out of the method.

### 3. A Big-O cheat sheet

*Big-O notation* describes how the cost of an operation grows with the number of elements, *n*. O(1) ("constant") means the cost doesn't grow with n. O(log n) grows by one step each time n doubles: about 20 steps for a million elements. O(n) ("linear") grows in proportion to n. *Amortized* O(1) means O(1) on average over many calls, although a single call is sometimes slower.

| Class | Find | Add | Remove | Notes |
|---|---|---|---|---|
| `ArrayList` | `get(i)` O(1) · `contains` O(n) | at the end O(1) amortized · at index i O(n) | at the end O(1) · at index i O(n) | the default `List` |
| `LinkedList` | `get(i)` O(n) · `contains` O(n) | at either end O(1) | at either end O(1) | rarely the best choice (section 4) |
| `ArrayDeque` | `peekFirst`, `peekLast` O(1) · `contains` O(n) | at either end O(1) amortized | at either end O(1) | the default stack and queue |
| `HashMap`, `HashSet` | O(1) on average | O(1) on average | O(1) on average | needs a good `hashCode()` |
| `LinkedHashMap`, `LinkedHashSet` | O(1) on average | O(1) on average | O(1) on average | keeps the insertion order |
| `TreeMap`, `TreeSet` | O(log n), also `floorKey`, `ceilingKey`, `firstKey` | O(log n) | O(log n) | sorted by key |
| `PriorityQueue` | `peek` O(1) · `contains` O(n) | `offer` O(log n) | `poll` O(log n) · `remove(x)` O(n) | smallest first |

Sorting a list with `list.sort(comparator)` costs O(n log n).

Big-O matters when n grows with your data. For 20 elements every choice is fast. For 100,000 orders, an O(n) `contains` inside a loop over the orders is O(n²): billions of comparisons. The fix is usually one line: put the values you look up in a `HashSet` first.

```java
// blockedIds is a List: contains() would compare with every element, for every order.
Set<String> blocked = new HashSet<>(blockedIds);   // copy once: O(n)
for (Order order : orders) {
    if (blocked.contains(order.customerId())) {    // O(1) per order
        reject(order);
    }
}
```

### 4. ArrayList vs LinkedList

An `ArrayList` stores its elements in an array. Reading element `i` takes one step, because the computer can calculate where it is. When the array is full, `add` creates an array that is 50% larger and copies the elements over. That copy is O(n), but it happens so rarely that `add` costs O(1) on average. Inserting or removing in the middle moves every later element by one place. That's O(n), but the move is a fast block copy (`System.arraycopy`).

A `LinkedList` stores each element in a separate *node* object that points to the previous and the next node. Adding or removing at either end changes a few pointers: O(1). But `get(i)` has to walk from one end, node by node: O(n).

In theory, `LinkedList` wins at inserting in the middle. In practice, `ArrayList` is faster almost every time:

- To insert in the middle of a `LinkedList`, you first have to walk to that position, which is O(n) anyway.
- An `ArrayList` keeps its elements next to each other in memory. The processor loads neighboring memory into its cache together, so walking through an array is very fast. A linked list's nodes are spread across memory.
- Each `LinkedList` node is an extra object of about 24 bytes, while an `ArrayList` slot is one reference of 4 or 8 bytes. More objects also mean more work for the garbage collector.

So use `ArrayList` for lists, and `ArrayDeque` (section 9) when you add and remove at both ends. In J05-02 you build a small `ArrayList` yourself.

### 5. How HashMap works

A `HashMap` keeps an array of *buckets*. To store a key, it calls `key.hashCode()`, an `int` computed from the key's contents, and turns that number into a bucket index. To find the key later, it computes the same index and looks only in that one bucket. That's why `get` and `put` take O(1) on average, however many entries the map holds.

Two keys can land in the same bucket. This is called a *collision*. The bucket then holds a short chain of entries, and the map compares the keys in the chain with `equals()` to find the right one. This design is called *separate chaining*. Here it is with 16 buckets and the simple index rule that you use in J05-04 (`HashMap` itself mixes the bits of the hash code first):

```
"apple".hashCode() = 93029210    Math.floorMod(93029210, 16) = 10    → bucket 10
"Aa".hashCode()    = 2112        Math.floorMod(2112, 16)     = 0     → bucket 0
"BB".hashCode()    = 2112        the same hash code!                 → bucket 0

bucket  0: ("Aa", 1) → ("BB", 2)     a collision: get("BB") walks the chain and compares with equals()
bucket  1: (empty)
   …
bucket 10: ("apple", 3)
```

This only works if `equals` and `hashCode` agree: **equal objects must have equal hash codes**. Otherwise two equal keys land in different buckets, and `get` looks in the wrong one. (Unequal objects may share a hash code; that only costs speed.) So if you override `equals`, override `hashCode` with the same fields. Records do both for you, from all of their components, and so do `String`, `Integer`, `LocalDate` and enums:

```java
record Sku(String code) {}                  // equals() and hashCode() come for free

Map<Sku, Integer> stock = new HashMap<>();
stock.put(new Sku("BK-1"), 3);
stock.get(new Sku("BK-1"));                 // 3: a different object, but an equal one
```

**Load factor and resizing.** The *load factor* is the number of entries divided by the number of buckets. A `HashMap` starts with 16 buckets and allows a load factor of 0.75. When a `put` makes the size greater than 16 × 0.75 = 12, the map doubles its array to 32 buckets and *rehashes*: it moves every entry to its bucket in the new array, because the index depends on the array length. A resize is O(n), but it happens rarely enough that `put` stays O(1) on average. If you know the size in advance, `HashMap.newHashMap(10_000)` (Java 19+) creates a map that holds 10,000 entries without resizing. Watch out: the argument of `new HashMap<>(100)` is a number of buckets, not of entries, so that map still grows after 96 entries.

**Tree bins.** If many keys share one bucket, because of a bad `hashCode` or an attacker who sends crafted keys, the chain gets long and lookups slow down to O(n). So since Java 8, `HashMap` turns a chain into a small red-black tree (a balanced search tree, section 7) when the chain grows beyond 8 entries and the map has at least 64 buckets. That limits the worst case to O(log n). A tree bin that shrinks to about 6 entries becomes a chain again.

**No order.** `HashMap` and `HashSet` don't define an iteration order, and the order can change when the map grows or when you upgrade the JDK. Never rely on it. A `LinkedHashMap` is a `HashMap` that also links its entries in a list, in insertion order or, if you ask for it, in access order (J05-09 uses that). Use it, or a `TreeMap` (sorted), when the order matters.

### 6. The mutable-key trap

A hash map finds a key through its `hashCode()`. If a key changes after you put it in, its hash code changes, and the map looks for it in the wrong bucket:

```java
class Isbn {                                   // a mutable key: don't write keys like this
    String value;

    Isbn(String value) { this.value = value; }

    @Override public boolean equals(Object o) { return o instanceof Isbn other && other.value.equals(value); }
    @Override public int hashCode() { return value.hashCode(); }
}

var isbn = new Isbn("978-1");
Set<Isbn> seen = new HashSet<>();
seen.add(isbn);
isbn.value = "978-2";       // the hash code changes while the key is in the set
seen.contains(isbn);        // false: the set looks in the bucket of the new hash code
seen.size();                // 1: the entry is still there, but nothing can reach it
```

The rule: **map keys and set elements must not change** in a way that changes their `equals` or `hashCode`. Use immutable types as keys: `String`, `Integer`, `LocalDate`, enums, and records whose components are immutable. The same rule applies to `TreeMap` and `TreeSet`, with `compareTo` instead of `hashCode`. A test in J05-04 shows the trap in your own hash table.

### 7. TreeMap: a sorted map that can navigate

A `TreeMap` keeps its keys sorted, either by their natural order (`compareTo`) or by a `Comparator` that you pass to the constructor. Inside, it's a *red-black tree*: a binary search tree (J05-05) that rebalances itself after every change, so every operation stays O(log n).

Sorted keys let you ask questions that a `HashMap` can't answer. These methods come from the `NavigableMap` interface:

```java
NavigableMap<Integer, Long> shippingFeeByMinWeight = new TreeMap<>(Map.of(
        0, 3_000L,         // from 0 g
        2_000, 5_000L,     // from 2 kg
        10_000, 9_000L));  // from 10 kg

shippingFeeByMinWeight.floorEntry(2_500).getValue();   // 5000: the entry with the greatest key <= 2,500
shippingFeeByMinWeight.ceilingKey(2_500);              // 10000: the least key >= 2,500
shippingFeeByMinWeight.headMap(2_000);                 // {0=3000}: the keys < 2,000, as a view
```

| Method | Returns |
|---|---|
| `floorKey(k)`, `ceilingKey(k)` | the greatest key ≤ k, the least key ≥ k (or `null`) |
| `lowerKey(k)`, `higherKey(k)` | the greatest key < k, the least key > k (or `null`) |
| `firstKey()`, `lastKey()` | the smallest and the largest key |
| `headMap(k)`, `tailMap(k)`, `subMap(a, b)` | views of a range of keys |

Each `...Key` method has an `...Entry` twin that returns the key and the value together. Typical uses: price and tax tiers, "which exchange rate was valid at this time?", "the next scheduled job after now", leaderboards.

One trap: a `TreeMap` decides whether two keys are the same with `compareTo`, not with `equals`. `new BigDecimal("1.0")` and `new BigDecimal("1.00")` aren't `equals`, but `compareTo` returns 0 for them, so a `TreeSet` keeps only one of them while a `HashSet` keeps both.

### 8. PriorityQueue: a binary heap

A `PriorityQueue` hands out its smallest element first, by natural order or by a `Comparator`. Inside, it's a *binary heap*: a tree stored in an array, where every parent is smaller than or equal to its children. The tree needs no links, because positions are computed: the children of index `i` are at `2i + 1` and `2i + 2`, and its parent is at `(i - 1) / 2`.

```
array:  [1, 3, 2, 7, 4, 5]          the same heap as a tree:        1
index:   0  1  2  3  4  5                                         /   \
                                                                 3     2
                                                                / \   /
                                                               7   4 5
```

- `peek()` returns index 0, the smallest element: O(1).
- `offer(x)` puts x at the end of the array, then swaps it with its parent as long as it is smaller (*sift up*). The tree is about log n levels high, so that's O(log n).
- `poll()` removes index 0, moves the last element to the top, then swaps it with its smaller child as long as it is larger (*sift down*): O(log n).

A heap is only partly sorted: each parent is smaller than or equal to its children, but there's no order between siblings or cousins. So `contains` is O(n), and **iterating over or printing a `PriorityQueue` doesn't show the elements in sorted order**. Only repeated `poll()` hands them out in order.

```java
PriorityQueue<Job> byDeadline = new PriorityQueue<>(Comparator.comparing(Job::deadline));
PriorityQueue<Integer> largestFirst = new PriorityQueue<>(Comparator.reverseOrder());
```

Typical uses: scheduling by deadline or priority, merging sorted streams of data, shortest paths in weighted graphs (Dijkstra's algorithm), and "top k" problems (J05-06).

### 9. ArrayDeque: the stack and the queue

An `ArrayDeque` is a *circular array*: the first element can be anywhere in the array, and the elements wrap around from the end to the start. So adding and removing at both ends is O(1) without moving anything. It's faster than `LinkedList`, and it's the class to use for both stacks and queues:

```java
Deque<String> stack = new ArrayDeque<>();      // last in, first out (LIFO)
stack.push("a");
stack.push("b");
stack.pop();                                   // "b"

Queue<String> queue = new ArrayDeque<>();      // first in, first out (FIFO)
queue.offer("a");
queue.offer("b");
queue.poll();                                  // "a"
```

On a `Deque`, `push`, `pop` and `peek` work on the head, so the head is the top of the stack. The `Queue` interface has two methods for each action: one throws an exception when it can't do its job, the other returns a special value.

| Action | Throws an exception | Returns `false` or `null` |
|---|---|---|
| add at the tail | `add(x)` | `offer(x)` |
| remove the head | `remove()` | `poll()` |
| look at the head | `element()` | `peek()` |

Because `poll()` and `peek()` use `null` to mean "empty", `ArrayDeque` doesn't accept `null` elements.

Don't use the classes `Stack` and `Vector`. They come from Java 1.0. Every method is `synchronized`, which costs time and still doesn't make a sequence of calls thread-safe (J08). And `Stack` extends `Vector`, so it lets anyone insert into the middle of your "stack". The Javadoc of `Stack` itself recommends `Deque`.

### 10. Unmodifiable collections: copies and views

`List.of`, `Set.of` and `Map.of` create *unmodifiable* collections: every method that would change them throws `UnsupportedOperationException`. They also reject `null` elements (`NullPointerException`), and `Set.of` and `Map.of` reject duplicates (`IllegalArgumentException`). The iteration order of `Set.of` and `Map.of` is unspecified and can differ from one run of the program to the next.

When you hand out a list that callers must not change, you can give them a copy or a view. They behave differently:

```java
List<String> source = new ArrayList<>(List.of("a"));

List<String> copy = List.copyOf(source);                    // an unmodifiable copy
List<String> view = Collections.unmodifiableList(source);   // a read-only view

source.add("b");
System.out.println(copy);   // [a]      a snapshot: later changes to source don't show up
System.out.println(view);   // [a, b]   the view reads through to source
```

| You write | Can callers change it? | Does it show later changes to the source? |
|---|---|---|
| `new ArrayList<>(source)` | yes | no: a copy |
| `List.copyOf(source)` | no | no: a copy |
| `Collections.unmodifiableList(source)` | no | yes: a view |
| `Arrays.asList(array)` | `set` only, no `add` or `remove` | yes: it *is* the array |
| `List.of(a, b)` | no | (no source) |

Use `List.copyOf` to store or return a collection safely: a *defensive copy*. A view is right when callers must see live data and only you change the source. Two more points:

- Unmodifiable isn't the same as immutable. `List.of(order)` can't change, but the `order` inside it still can. Prefer immutable elements, such as records.
- In TypeScript, `readonly string[]` exists only at compile time. In Java, the type `List<String>` doesn't tell you whether `add` works; you find out at run time. So say it in the Javadoc of every method that returns a collection.

### 11. Sequenced collections (Java 21)

Java 21 added interfaces for collections with a defined first and last element: `SequencedCollection` (implemented by every `List` and `Deque`, by `LinkedHashSet` and by `TreeSet`) and `SequencedMap` (implemented by `LinkedHashMap` and `TreeMap`). They give all of these collections the same methods:

```java
List<String> steps = new ArrayList<>(List.of("validate", "charge", "ship"));
steps.getFirst();       // "validate"   before Java 21: steps.get(0)
steps.getLast();        // "ship"       before Java 21: steps.get(steps.size() - 1)
steps.addFirst("log");  // also addLast, removeFirst and removeLast
steps.reversed();       // [ship, charge, validate, log]: a view, not a copy

SequencedMap<String, Integer> scores = new LinkedHashMap<>();
scores.putLast("kim", 90);
scores.firstEntry();    // kim=90; also lastEntry, pollFirstEntry, putFirst and reversed
```

Two things to know:

- `getFirst()` and `getLast()` throw `NoSuchElementException` on an empty collection. (In TypeScript, `array[0]` gives `undefined`.)
- `reversed()` returns a *view*: changes to the original show up in it, and changes made through it change the original.

`HashSet` and `HashMap` have no defined order, so they aren't sequenced.

### 12. Removing elements while you iterate

A for-each loop uses an `Iterator` behind the scenes. `ArrayList` and the other `java.util` collections count their own changes. If a collection changes while an iterator walks through it, and the change doesn't go through that iterator, the iterator's next step throws `ConcurrentModificationException`. "Concurrent" doesn't mean threads here: one thread is enough.

```java
for (Order order : orders) {
    if (order.isCancelled()) {
        orders.remove(order);             // ConcurrentModificationException on the next step
    }
}

orders.removeIf(Order::isCancelled);      // the fix: removes every match in one pass

Iterator<Order> iterator = orders.iterator();
while (iterator.hasNext()) {              // or remove through the iterator itself
    if (iterator.next().isCancelled()) {
        iterator.remove();
    }
}
```

This check is called *fail-fast*. It exists to find bugs early, but it's best effort: sometimes the loop ends early without an exception and silently skips elements (a J05-01 puzzle shows when). `removeIf` is also faster on an `ArrayList`: it moves the remaining elements once, not once per removed element. For maps, use `map.entrySet().removeIf(...)` or `map.values().removeIf(...)`.

### 13. Comparable, Comparator, stable sorting and binary search

A class with one obvious order implements `Comparable<T>` and its method `compareTo`, which returns a negative number if `this` comes first, zero if both are equal, and a positive number if `this` comes after. `String`, `Integer`, `BigDecimal` and `LocalDate` are `Comparable`; their order is called the *natural order*.

For any other order, you write a `Comparator<T>`. You rarely write one by hand. You build it from methods that extract the keys:

```java
Comparator<Book> catalogOrder = Comparator
        .comparing(Book::author)                                        // author A to Z
        .thenComparing(Comparator.comparingInt(Book::year).reversed())  // then the newest first
        .thenComparing(Book::title);                                    // then title A to Z
books.sort(catalogOrder);

// A key that can be null needs a comparator that knows where null goes:
Comparator<Book> bySeries = Comparator.comparing(Book::series, Comparator.nullsLast(Comparator.naturalOrder()));
```

Each `thenComparing` only decides when everything before it is equal. Watch out for two traps:

- `reversed()` reverses the whole comparator it's called on: `comparing(a).thenComparing(b).reversed()` reverses both a and b. To reverse one key, reverse only that key's comparator, as in the example.
- Never compare numbers by subtracting them, as in `(x, y) -> x.price() - y.price()`. The subtraction can overflow and give the wrong sign. Use `comparingInt`, `comparingLong`, `Integer.compare` or `Long.compare`.

**Stable sorting.** `List.sort` and `Collections.sort` are *stable*: elements that compare as equal keep their original order. (They use TimSort, a variant of merge sort, in O(n log n).) So you can sort by one key and then by another: the second sort keeps the first order among its ties. `Arrays.sort` on an `int[]` uses an algorithm that isn't stable, but for plain numbers you can't tell the difference.

**Binary search.** In a sorted array, you can find a value by halving the search range at every step: compare with the middle element, then continue in the left or the right half. That's O(log n), about 20 steps for a million elements. `Arrays.binarySearch(array, key)` and `Collections.binarySearch(list, key)` return the key's index if they find it (with duplicates, any one of them). If they don't, they return `-(insertion point) - 1`, a negative number that encodes where the key would go. In J05-08 you write two variants that also handle duplicates, *lower bound* and *upper bound*. Compute the middle as `low + (high - low) / 2`: the shorter `(low + high) / 2` overflows when `low + high` is greater than `Integer.MAX_VALUE`, a bug that was in the JDK's own binary search until 2006.

### 14. Recursion

A *recursive* method calls itself to solve a smaller part of the problem. It needs a *base case* that returns without calling itself, and a *recursive case* that moves toward the base case. Trees are recursive by nature, because a tree is a node with smaller trees below it. So recursion often gives the clearest code for trees:

```java
record Category(String name, List<Category> children) {}

/** The number of categories in this tree, including the root. */
static int count(Category category) {
    int total = 1;                                  // this category itself
    for (Category child : category.children()) {    // base case: no children, so no recursive call
        total += count(child);                      // recursive case: a smaller tree
    }
    return total;
}
```

Every call waits for the calls it makes, and each waiting call keeps a *stack frame* on the thread's call stack. That stack is limited, typically to a few thousand or tens of thousands of nested calls, depending on its size and on the method. Recursing deeper throws `StackOverflowError`, and Java doesn't optimize tail calls away. Recursion is fine when the depth stays small, such as a balanced tree (about 20 levels for a million nodes) or a category tree. When the depth can grow with the input, as in a long linked list or an unbalanced tree, use a loop, with an `ArrayDeque` as an explicit stack if you need one.

### 15. Graphs: adjacency lists, BFS and DFS

A *graph* is a set of *nodes* (also called vertices) connected by *edges*. An edge can be *undirected*, like a subway track that trains travel both ways, or *directed*, like "course B requires course A". Graphs appear everywhere in backend work: dependencies between services, foreign keys between tables, the order of build tasks and database migrations, social and recommendation features.

The usual way to store a graph is an *adjacency list*: a map from each node to its neighbors.

```java
Map<String, List<String>> neighbors = new HashMap<>();
neighbors.computeIfAbsent("A", node -> new ArrayList<>()).add("B");   // the edge A → B
neighbors.computeIfAbsent("B", node -> new ArrayList<>()).add("A");   // undirected: also B → A
```

There are two basic ways to explore a graph from a start node:

- **Breadth-first search (BFS)** uses a queue. It visits every node 1 edge away, then every node 2 edges away, and so on. So the first time it reaches a node, it has found a path with the fewest edges. Use it for shortest paths in graphs without weights: "fewest stops", "degrees of separation".
- **Depth-first search (DFS)** uses a stack, or recursion. It follows one path as deep as it can, then backs up and tries the next one. Use it to find everything that is reachable or to detect cycles. It doesn't find shortest paths.

Both need a set of visited nodes. Without it, a cycle makes them run forever.

```java
/** The number of edges from start to every node that can be reached from it. */
static Map<String, Integer> distancesFrom(String start, Map<String, List<String>> neighbors) {
    Map<String, Integer> distance = new HashMap<>();   // also serves as the set of visited nodes
    Deque<String> queue = new ArrayDeque<>();
    distance.put(start, 0);
    queue.add(start);
    while (!queue.isEmpty()) {
        String node = queue.poll();
        for (String next : neighbors.getOrDefault(node, List.of())) {
            if (!distance.containsKey(next)) {         // not seen yet
                distance.put(next, distance.get(node) + 1);
                queue.add(next);
            }
        }
    }
    return distance;
}
```

To return the path itself, also remember, for each node, the node you reached it from. J05-07 does that.

**Topological sort.** In a directed graph without cycles, a *topological order* lists every node after all the nodes it depends on: courses after their prerequisites, Gradle tasks after the tasks they depend on, database migrations, or the objects in a dependency injection container (J09). *Kahn's algorithm* computes it. Count each node's unmet dependencies. Then, again and again, take a node whose count is zero, add it to the result, and decrement the count of every node that depends on it. If some nodes never reach zero, they are on a cycle (or depend on one), and no valid order exists.

Graphs with weights, such as travel times, need Dijkstra's algorithm, which replaces BFS's queue with a `PriorityQueue`. That's beyond this module, but you'll have practiced every part it uses.

## TypeScript → Java

| TypeScript | Java | Watch out for |
|---|---|---|
| `const items: string[] = []` | `List<String> items = new ArrayList<>();` | A Java array (`String[]`) has a fixed length. Use a `List`. |
| `items.length` | `items.size()` | Arrays use `array.length`, strings `text.length()`. |
| `items[i]` | `items.get(i)` | An index out of range throws `IndexOutOfBoundsException` instead of giving `undefined`. |
| `items[0]`, `items.at(-1)` | `items.getFirst()`, `items.getLast()` | They throw `NoSuchElementException` on an empty list. |
| `items.push(x)`, `items.pop()` | `items.add(x)`, `items.removeLast()` | `removeLast()` on an empty list throws; JavaScript's `pop()` gives `undefined`. |
| `items.unshift(x)`, `items.shift()` | `deque.addFirst(x)`, `deque.pollFirst()` | O(n) on an `ArrayList`. Use an `ArrayDeque`. |
| `items.includes(x)`, `items.indexOf(x)` | `items.contains(x)`, `items.indexOf(x)` | They compare with `equals()`, not by reference. |
| `items.sort()` | `items.sort(Comparator.naturalOrder())` | Without a comparator, JavaScript compares as strings: `[10, 9, 1]` becomes `[1, 10, 9]`. |
| `items.sort((a, b) => a.price - b.price)` | `items.sort(Comparator.comparingInt(Item::price))` | Don't subtract to compare in Java: `int` can overflow. |
| `[...items].reverse()` | `items.reversed()` | Java returns a view of the same list, not a copy. |
| `items = items.filter(x => keep(x))` | `items.removeIf(x -> !keep(x))` | `removeIf` changes the list itself. Streams (J06) build a new one. |
| `ReadonlyArray<string>` | `List.copyOf(items)`, `List.of(...)` | Enforced at run time with `UnsupportedOperationException`, not by the type. |
| `new Map<K, V>()` | `new HashMap<K, V>()` | No defined order. `LinkedHashMap` keeps the insertion order, like a JavaScript `Map`. |
| `map.get(k)` gives `undefined` | `map.get(k)` gives `null` | Use `getOrDefault`, or return `Optional` from your own methods. |
| `map.set(k, (map.get(k) ?? 0) + 1)` | `map.merge(k, 1, Integer::sum)` | `map.get(k) + 1` throws `NullPointerException` for a missing key. |
| `new Set<T>()` | `new HashSet<T>()`, `new LinkedHashSet<T>()` | A JavaScript `Set` keeps the insertion order; a `HashSet` doesn't. |
| Map keys use SameValueZero: objects by reference | Map keys use `equals()` and `hashCode()` | Two equal records are the same key. A JavaScript `Map` treats two equal objects as two keys. |
| no built-in heap | `PriorityQueue<T>` | Iterating over it isn't sorted; only `poll()` is. |
| no built-in sorted map or set | `TreeMap<K, V>`, `TreeSet<T>` | Sorted by `compareTo` or by a `Comparator`. |
| `for (const item of items)` | `for (var item : items)` | Don't add or remove elements inside the loop. |

## In real projects

- **Declare interfaces, return empty collections.** Fields, parameters and return types use `List`, `Set` and `Map`. A method that finds nothing returns an empty collection, never `null`. See [Collections and generics](../../docs/handbook/collections-and-generics.md) and [Null and Optional](../../docs/handbook/null-and-optional.md).
- **Name a map after what it maps:** `priceBySku`, `ordersByCustomerId`. See [Java style](../../docs/handbook/java-style.md).
- **Copy at the boundaries.** A constructor stores `List.copyOf(items)`, and a record with a collection component does the same in its compact constructor. Then no caller can change the object's state behind its back. See [Immutability](../../docs/handbook/immutability.md).
- **Immutable keys.** Map keys and set elements are records, strings, numbers or enums. Never a mutable object, such as a JPA entity whose id is set only when it's saved (S06).
- **Choose by how you access the data.** Start with `ArrayList`, `HashMap`, `HashSet` and `ArrayDeque`. Choose `LinkedHashMap` when the order matters, `TreeMap` for sorted keys and range questions, and `PriorityQueue` for "the next one by priority". Avoid `LinkedList`, `Stack`, `Vector` and `Hashtable`.
- **Watch for hidden O(n²).** A `list.contains` or `list.remove(object)` inside a loop over another large collection is the most common performance bug in collection code. Put the side you look up into a `HashSet` or a `HashMap` first.
- **Sort deterministically.** End every sort with a unique tie-breaker, usually an id, so the same data always comes out in the same order. Paged APIs need this: without it, rows with equal sort keys can appear on two pages or on none. The same applies to `ORDER BY` in SQL.
- **Remove with `removeIf`**, not inside a loop, and use streams (J06) when you build a new collection from an old one. See [Functional style](../../docs/handbook/functional-style.md).
- **Not thread-safe.** `HashMap`, `ArrayList` and the other classes in this module break when several threads change them. Use `ConcurrentHashMap` and the other `java.util.concurrent` classes, or don't share (J08). See [Concurrency](../../docs/handbook/concurrency.md).
- **Exceptions say what went wrong:** `IndexOutOfBoundsException` for a bad index, `NoSuchElementException` when there's nothing to return, `IllegalArgumentException` with the bad value in the message for an invalid argument. See [Exceptions](../../docs/handbook/exceptions.md).
- **Money in whole cents.** Prices are `long` cents, as in J05-05, or `BigDecimal` (J07), never `double`. See [Dates and money](../../docs/handbook/dates-and-money.md).
- **Test a data structure against a model.** Compare your class with a JDK class over thousands of random operations with a fixed seed, as this module's tests do. See [Testing](../../docs/handbook/testing.md).

## Exercises

### J05-01 · Collection traps and Big-O

**Predict** · 30 min · [`Puzzles.java`](src/main/java/practice/j05/ex01/Puzzles.java) → answers in [`Answers.java`](src/main/java/practice/j05/ex01/Answers.java)

Sixteen small puzzles about collection behavior that surprises developers who come from TypeScript, and twelve questions about what common operations cost. Each of these traps has caused real production bugs.

**Before you start:** lesson sections 1 to 4 and 10 to 12, and the TypeScript → Java table.

**How:** read each method in `Puzzles` without running it, and write your prediction in the matching constant in `Answers`. Answer the Big-O questions from memory before you look at the cheat sheet again. Then run `PuzzlesTest`. Each failing test tells you what you predicted and what the code really gives.

<details>
<summary>Explanations (open after you've run the tests)</summary>

- **`List.of(...).add(...)` throws `UnsupportedOperationException`.** `List.of` returns an unmodifiable list. Its type is still `List<String>`, so the compiler can't warn you.
- **`set` through `Arrays.asList(array)` gives `[a, z, c]`.** `Arrays.asList` doesn't copy. It returns a fixed-size list *view* of the array, so `set` writes into the array itself.
- **`Arrays.asList(...).add(...)` throws `UnsupportedOperationException`.** The view can't change the array's length. When you need a list that grows, write `new ArrayList<>(List.of(...))`.
- **`Collections.unmodifiableList(source)` shows `[a, b]` after `source.add("b")`.** It's a read-only view: you can't change the list through it, but it reads through to the source.
- **`List.copyOf(source)` still shows `[a]`.** It's a copy, so later changes to the source don't show up.
- **`numbers.reversed()` shows `[4, 3, 2, 1]`.** `reversed()` returns a view in reverse order, not a reversed copy, so it sees the 4 that was added later.
- **`getFirst()` on an empty list throws `NoSuchElementException`.** TypeScript's `array[0]` gives `undefined`; Java fails loudly instead.
- **Removing inside a for-each loop throws `ConcurrentModificationException`.** The loop's hidden iterator notices on its next step that the list changed behind its back. Write `numbers.removeIf(n -> n % 2 == 0)` instead.
- **Removing the second-to-last element gives `[a, b, d]`, without an exception.** After the removal the list has 3 elements, and the iterator's position is already 3, so `hasNext()` returns `false`. The loop ends without looking at `"d"`. The fail-fast check runs in `next()`, which is never called again. Fail-fast is best effort: never rely on the exception to find this bug for you.
- **`numbers.remove(1)` gives `[1, 3]`.** For a `List<Integer>`, the `int` argument matches `remove(int index)` exactly, so it removes the element at index 1, which is 2.
- **`numbers.remove(Integer.valueOf(1))` gives `[2, 3]`.** An `Integer` argument matches `remove(Object)`, which removes the element equal to 1.
- **The `TreeSet` gives `[Apple, apple, banana, cherry]`.** `String.compareTo` compares character codes, and every uppercase letter (A is 65) comes before every lowercase letter (a is 97). `new TreeSet<>(String.CASE_INSENSITIVE_ORDER)` ignores case, but then it also treats `"Apple"` and `"apple"` as the same element and keeps only one.
- **The `LinkedHashSet` gives `[c, a, b]`.** It keeps the order in which the elements were first added, and adding `"a"` again changes nothing. A `HashSet` has no defined order at all, which is why no puzzle here prints one.
- **`"pears: " + stock.get("pear")` is `"pears: null"`.** `get` returns `null` for a missing key, and string concatenation turns `null` into `"null"`.
- **`stock.put("pear", stock.get("pear") + 1)` throws `NullPointerException`.** `stock.get("pear")` is `null`, and `+ 1` has to unbox it to an `int`. Count with `stock.merge("pear", 1, Integer::sum)`.
- **`getOrDefault("apple", 0) + getOrDefault("pear", 0)` is `3`.** The stored 3 for `"apple"`, plus the default 0 for the missing `"pear"`.
- **Big-O.** `ArrayList`: `get(i)` O(1), `add(x)` O(1) amortized, `add(0, x)` O(n), `contains` O(n). `LinkedList`: `get(n / 2)` O(n), `addFirst` O(1). `HashMap.get` O(1). `TreeMap.get` O(log n). `PriorityQueue`: `peek` O(1), `poll` O(log n), `contains` O(n). Sorting: O(n log n). Lesson sections 3, 4, 5, 7 and 8 explain why.
</details>

### J05-02 · Dynamic array

**Build** · 30 min · [`DynamicArray.java`](src/main/java/practice/j05/ex02/DynamicArray.java)

`ArrayList` is the collection you'll use most. You build a small version of it on top of a plain array, to see where its costs come from: why `get` takes one step, why inserting at the front is slow, and why `add` stays fast although the array has to grow.

**Before you start:** lesson sections 3 and 4.

**You'll practice:** arrays and `System.arraycopy`, a generic class backed by an `Object[]`, bounds checks, implementing `Iterable` and `Iterator`, growing by doubling.

**Rules:** in the Javadoc of `DynamicArray`.

<details>
<summary>Hint 1</summary>

You need two fields: the `Object[]` and an `int size`. The array's length is the capacity; `size` counts the slots in use. Java can't create a generic array (`new E[10]` doesn't compile), so store `Object`s and cast when you read: `(E) elements[index]`, in a private method annotated with `@SuppressWarnings("unchecked")`.
</details>

<details>
<summary>Hint 2</summary>

`Objects.checkIndex(index, size)` throws `IndexOutOfBoundsException` unless `0 <= index < size`. For `add(index, element)`, the valid range ends one later. `System.arraycopy(source, from, target, to, count)` moves a block of elements, and it handles overlapping ranges correctly. `Arrays.copyOf(array, newLength)` returns a longer copy.
</details>

<details>
<summary>Hint 3</summary>

`iterator()` can return an anonymous class: `return new Iterator<>() { private int next; ... };` with the two methods `hasNext()` and `next()`.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Does your `remove` leave a reference to an element in a slot that is no longer used? Why does doubling keep `add` O(1) on average, when growing by a fixed 10 slots wouldn't?

### J05-03 · Stacks and queues

**Build** · 30 min · [`LinkedStack.java`](src/main/java/practice/j05/ex03/LinkedStack.java) · [`Brackets.java`](src/main/java/practice/j05/ex03/Brackets.java)

Undo histories, the call stack of every program and every parser that handles nesting use a stack: last in, first out. You build one from linked nodes. Then you use `ArrayDeque` as a stack to check brackets, the way a JSON parser or a code editor does. You'll use `ArrayDeque` as a queue in J05-07.

**Before you start:** lesson section 9.

**You'll practice:** linked nodes with a nested record, `NoSuchElementException`, `ArrayDeque` as a stack, `switch` on a `char`.

**Rules:** in the Javadoc of `LinkedStack` and `Brackets`.

<details>
<summary>Hint 1</summary>

A stack only needs a reference to its top node, and each node points to the node below it: `private record Node<E>(E value, Node<E> below) {}`. `push` creates a new top node that points to the old top.
</details>

<details>
<summary>Hint 2</summary>

In `Brackets`, push the closing bracket you *expect* whenever you see an opening one. When a closing bracket arrives, it must match the top of the stack. And what must be true about the stack when the text ends?
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Why does `ArrayDeque` refuse `null` elements? Would `Brackets` still work for 100,000 nested brackets if you had written it with recursion?

### J05-04 · Hash table

**Build** · 60 min · [`SimpleHashMap.java`](src/main/java/practice/j05/ex04/SimpleHashMap.java)

`HashMap` is the most used data structure in backend code: caches, indexes, lookups by id, counting. You build your own, with separate chaining, a load factor of 0.75 and resizing. While you build it, you see the contract between `equals` and `hashCode` from the inside, including what happens when someone changes a key.

**Before you start:** lesson sections 5 and 6.

**You'll practice:** an array of linked nodes, `hashCode` and `equals`, `Math.floorMod`, resizing and rehashing, `Optional` return values, unmodifiable snapshots.

**Rules:** in the Javadoc of `SimpleHashMap`. Also look at the test helpers `CollidingKey` and `MutableKey` at the top of `SimpleHashMapTest`.

<details>
<summary>Hint 1</summary>

Two common designs for the buckets: an array of linked nodes, `Node<K, V>[]`, where each node has a key, a value and a `next` reference (like the JDK), or a `List<List<Entry<K, V>>>`. For the array, `(Node<K, V>[]) new Node<?, ?>[capacity]` creates it (with `@SuppressWarnings("unchecked")`). Write a private `find(key)` method first: `get`, `containsKey` and `put` all need it.
</details>

<details>
<summary>Hint 2</summary>

Why `Math.floorMod` and not `%`? Compare `-7 % 16` and `Math.floorMod(-7, 16)` in JShell. `Math.abs(hash) % capacity` looks like a fix, but it fails for one value: `Math.abs(Integer.MIN_VALUE)` is still negative.
</details>

<details>
<summary>Hint 3</summary>

When the map grows, you can't copy the old array into the new one as it is, because a key's bucket depends on the capacity. Walk through every chain and put each entry into its bucket in the new array.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: What happens to `get` if a key class overrides `equals` but not `hashCode`? Why does your map compare keys with `equals()` and never with `==`?

### J05-05 · Binary search tree

**Build** · 30 min · [`BinarySearchTree.java`](src/main/java/practice/j05/ex05/BinarySearchTree.java) · [`TieredPricing.java`](src/main/java/practice/j05/ex05/TieredPricing.java)

`TreeMap`, `TreeSet` and the indexes of your database keep keys sorted in a tree, so that lookups and range questions both take O(log n). You build a simple binary search tree, then use `TreeMap.floorEntry` for a real pricing rule: "from 10 items, each costs 4.50".

**Before you start:** lesson sections 7 and 14.

**You'll practice:** recursion on trees, `Comparable` and `compareTo`, `Optional` results, in-order traversal, `TreeMap` navigation, validating and copying constructor arguments.

**Rules:** in the Javadoc of `BinarySearchTree` and `TieredPricing`.

<details>
<summary>Hint 1</summary>

A recursive `add` reads well when it returns the root of the subtree it worked on: `private Node<K> add(Node<K> node, K key)` returns a new node when `node` is `null`. Otherwise it sets `node.left = add(node.left, key)` or `node.right = add(node.right, key)` and returns `node`.
</details>

<details>
<summary>Hint 2</summary>

For `floor`, walk down from the root. When a node's key is larger than the given key, go left. When it's smaller, remember it as the best answer so far, then go right to look for a larger one that still fits.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: What shape does your tree get when the keys arrive in sorted order, and what does that do to `contains`? Can a caller change the prices of a `TieredPricing` after creating it?

### J05-06 · Heap and priority queue

**Build** · 30 min · [`MinHeap.java`](src/main/java/practice/j05/ex06/MinHeap.java) · [`TopWords.java`](src/main/java/practice/j05/ex06/TopWords.java)

Job schedulers, timers and "top 10" dashboards all need the smallest or the largest element quickly. You build the binary heap behind `PriorityQueue`, then use `PriorityQueue` for a top-k query, the kind behind "trending searches".

**Before you start:** lesson section 8.

**You'll practice:** index arithmetic on a tree stored in a list, sift up and sift down, `Comparator` chains, counting with `Map.merge`, keeping only the k best elements in a heap.

**Rules:** in the Javadoc of `MinHeap` and `TopWords.topK`.

<details>
<summary>Hint 1</summary>

Store the heap in an `ArrayList<E>`. `offer` adds at the end and sifts up. `poll` saves the element at index 0, moves the last element to index 0, and sifts it down. `Collections.swap(list, i, j)` swaps two elements.
</details>

<details>
<summary>Hint 2</summary>

For `topK`, count with `counts.merge(word, 1, Integer::sum)`, and put each word with its count in a small record. The simple way: put every word into a `PriorityQueue` ordered best first, and poll k times. The way that scales: keep at most k words in a heap ordered *worst* first. After each `offer`, if the heap holds more than k words, `poll()` drops the worst one.
</details>

<details>
<summary>Hint 3</summary>

`comparingInt(WordCount::count).reversed().thenComparing(WordCount::word)` and `comparingInt(WordCount::count).thenComparing(WordCount::word).reversed()` are different orders. Which one do you need?
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Why must sift down swap with the *smaller* child? Compared with sorting every word, what does the heap of k words save, and what doesn't it save?

### J05-07 · Graphs: BFS and topological sort

**Build** · 30 min · [`SubwayMap.java`](src/main/java/practice/j05/ex07/SubwayMap.java) · [`CoursePlanner.java`](src/main/java/practice/j05/ex07/CoursePlanner.java)

Many backend problems are graph problems, even when they don't look like it: routes, dependencies between services, the order of database migrations. You find the route with the fewest stops in a subway network with breadth-first search, and you order courses by their prerequisites with Kahn's algorithm. In J09, your dependency injection container uses the same idea to create every object after its dependencies.

**Before you start:** lesson sections 9 and 15.

**You'll practice:** an adjacency list with `Map<String, List<String>>` and `computeIfAbsent`, BFS with an `ArrayDeque`, rebuilding a path from a map of "came from" links, counting unmet dependencies, `PriorityQueue` for a deterministic order.

**Rules:** in the Javadoc of `SubwayMap` and `CoursePlanner.order`. The record `Connection` is finished.

<details>
<summary>Hint 1</summary>

For BFS, mark a station as visited when you add it to the queue, and record where you came from in a `Map<String, String> cameFrom`. When you take `to` out of the queue, follow `cameFrom` back to the start, then reverse the list.
</details>

<details>
<summary>Hint 2</summary>

For Kahn's algorithm, build two maps: `unlocks` (a prerequisite → the courses that need it) and `waitingFor` (a course → how many of its prerequisites aren't placed yet). Put every course with 0 into a `PriorityQueue<String>`. Poll a course, add it to the result, and decrement the count of every course it unlocks; a course that reaches 0 joins the queue. If the result has fewer courses than `waitingFor` at the end, there is a cycle.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Why does DFS find *a* route, but not always the shortest one? Why does `CoursePlanner` need a `PriorityQueue` and not a plain FIFO queue to follow the alphabetical rule?

### J05-08 · Sorting and binary search

**Build** · 30 min · [`EmployeeOrder.java`](src/main/java/practice/j05/ex08/EmployeeOrder.java) · [`Search.java`](src/main/java/practice/j05/ex08/Search.java)

Reports, paged APIs and exports all need a precise, repeatable order. You build a comparator with several keys for a payroll report and see why stable sorting matters. Then you write the binary searches that database indexes use to find a range of values.

**Before you start:** lesson section 13.

**You'll practice:** `Comparator.comparing`, `thenComparing`, `reversed` and `nullsLast`, stable sorting, binary search with a precise rule for each step, avoiding integer overflow.

**Rules:** in the Javadoc of `EmployeeOrder` and `Search`. The record `Employee` is finished.

**Compared with `Arrays.binarySearch`:** the JDK's binary search returns the key's index if it finds the key, and `-(insertion point) - 1` if it doesn't. For a missing key, its insertion point is exactly your `lowerBound`. But when the key appears more than once, `Arrays.binarySearch` returns *any* of the matching indexes, so it can't tell you where a run of equal values starts or ends. `lowerBound` and `upperBound` can, and the number of values between `low` and `high` is `upperBound(high) - lowerBound(low)`, without a loop.

| `sorted = {10, 20, 20, 30}` | `lowerBound` | `upperBound` | `Arrays.binarySearch` |
|---|---|---|---|
| target 20 | 1 | 3 | 1 or 2: any match |
| target 25 | 3 | 3 | -4, so the insertion point is 3 |
| target 5 | 0 | 0 | -1, so the insertion point is 0 |
| target 99 | 4 | 4 | -5, so the insertion point is 4 |

<details>
<summary>Hint 1</summary>

Write `lowerBound` with `low = 0` and `high = sorted.length`, and keep two facts true at every step: everything before `low` is smaller than the target, and nothing from `high` on is. When `low == high`, that's the answer. `upperBound` is the same with one comparison changed.
</details>

<details>
<summary>Hint 2</summary>

`Comparator.comparing` has a second form that takes a comparator for the key: `comparing(keyExtractor, keyComparator)`. `Comparator.nullsLast(...)` wraps another comparator and puts `null` after every other value.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Why does `countInRange` use `upperBound(high)` and not `lowerBound(high + 1)`? Why is `low + (high - low) / 2` safer than `(low + high) / 2`?

### J05-09 · LRU cache

**Build** · 30 min · [`LruCache.java`](src/main/java/practice/j05/ex09/LruCache.java)

A cache with a size limit has to decide what to throw away when it's full. *Least recently used* (LRU) evicts the entry nobody has touched for the longest time. It's the policy behind most in-memory caches, including the ones you'll plug into Spring in S10.

**Before you start:** lesson sections 5 and 10.

**You'll practice:** `LinkedHashMap` access order, overriding a `protected` hook method, `Optional` return values, returning defensive copies.

**Rules:** in the Javadoc of `LruCache`: the capacity limit, what counts as "using" an entry, `null` handling, and the order of `keys()`.

<details>
<summary>Hint 1</summary>

You don't need to build a linked list yourself. Read the Javadoc of the `LinkedHashMap(int, float, boolean)` constructor: what does its `accessOrder` argument change?
</details>

<details>
<summary>Hint 2</summary>

`LinkedHashMap` has a `protected boolean removeEldestEntry(Map.Entry<K, V> eldest)` method that it calls after every insertion. You can override it in an anonymous subclass: `new LinkedHashMap<>(16, 0.75f, true) { ... }`.
</details>

**Done?** Compare with the reference on the `solutions` branch, then check: Does an object with an invalid capacity ever get created? Can a caller change your cache through the list `keys()` returns?

## Review questions

1. Your service checks each of 100,000 incoming orders against a blocklist of 50,000 customer ids, stored as a `List<String>`. Why is that slow, and what's the one-line fix?
2. Why must a class that overrides `equals` also override `hashCode`? What goes wrong in a `HashMap` if it doesn't?
3. `map.get(customer)` returns `null`, but you're sure you put that customer into the map. Name two possible causes.
4. Name two backend tasks where a `TreeMap` is a better fit than a `HashMap`, and the `TreeMap` method you'd use for each.
5. What's the difference between `List.copyOf(list)` and `Collections.unmodifiableList(list)`? When does the difference matter?
6. Why is `ArrayDeque` a better stack than `Stack`, and a better queue than `LinkedList`?
7. You print a `PriorityQueue<Integer>`, and the numbers aren't sorted. Is it broken?
8. A paged API sorts products by price. Products with the same price sometimes show up on two pages, or on none. Why, and how do you fix it?
9. Why does BFS find the route with the fewest stops, while DFS might not?
10. How does Kahn's algorithm notice a cycle?
11. When is recursion a bad choice in Java?

<details>
<summary>Answers</summary>

1. `List.contains` compares with every element: up to 100,000 × 50,000 = 5 billion comparisons. Copy the blocklist into a `HashSet` once, with `new HashSet<>(blocklist)`; each lookup is then O(1).
2. A `HashMap` uses `hashCode()` to choose the bucket, and `equals()` only inside that bucket. Two equal objects with different hash codes usually land in different buckets. So `get` with an equal key looks in the wrong bucket and finds nothing, and `put` can store the "same" key twice.
3. The key changed after `put`, so its hash code changed (the mutable-key trap). Or the key's class overrides `equals` but not `hashCode`, or neither, so two equal-looking objects aren't equal. A third possibility: the key you look up differs in a detail, such as letter case or trailing spaces.
4. For example: a report sorted by date, without a separate sort (iterate over the `TreeMap`); "the price tier for this quantity" or "the exchange rate valid at this time" (`floorEntry`); "the next scheduled job after now" (`ceilingEntry` or `higherEntry`); "all events between two times" (`subMap`).
5. `List.copyOf` makes an unmodifiable copy, so later changes to the source don't show up in it. `unmodifiableList` is a read-only view, so they do. It matters whenever someone still holds the source: a getter that returns a view of a field lets callers watch the object change under them, while a copy gives them a stable snapshot.
6. `Stack` extends `Vector`: every method is `synchronized`, which costs time, and anyone can insert into the middle of the stack. `LinkedList` creates a node object for every element and scatters them across memory. `ArrayDeque` is a compact circular array with O(1) operations at both ends.
7. No. A heap only promises that each parent is smaller than or equal to its children. Printing and iterating walk through the array in index order. Only repeated `poll()` returns the elements sorted.
8. Products with equal prices have no defined order, so each request may return them in a different order, and a product can move across a page boundary between two requests. Add a unique tie-breaker as the last sort key, such as the product id: `ORDER BY price, id` in SQL, or `.thenComparing(Product::id)` in Java.
9. BFS takes stations out of its queue in order of distance: every station 1 stop away, then every station 2 stops away, and so on. So when it first reaches the destination, no shorter route can exist. DFS follows one branch as deep as it can, so it may reach the destination through a long detour first.
10. Courses on a cycle wait for each other, so their counts never reach zero and they never enter the queue of ready courses. At the end, fewer courses are placed than exist.
11. When the depth of the recursion grows with the input, as with a linked list of 100,000 nodes or a tree that isn't balanced. Each call needs a stack frame, the stack is limited, and Java doesn't optimize tail calls, so deep recursion throws `StackOverflowError`. Use a loop instead, with an `ArrayDeque` as a stack if you need one.
</details>

## Further reading

- Official docs: [the `java.util` package](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/package-summary.html) · [Collections Framework overview](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/doc-files/coll-overview.html) · [The Collections Framework on dev.java](https://dev.java/learn/api/collections-and-streams/collections-framework/)
- [JEP 431: Sequenced Collections](https://openjdk.org/jeps/431): why Java 21 added `getFirst`, `getLast` and `reversed`.
- [The source code of `HashMap`](https://github.com/openjdk/jdk/blob/master/src/java.base/share/classes/java/util/HashMap.java): the "Implementation notes" comment near the top explains buckets, resizing and tree bins.
- Joshua Bloch, *Effective Java*, 3rd edition: items 10 and 11 (`equals` and `hashCode`), 14 (`Comparable`), 17 (minimize mutability), 54 (return empty collections, not `null`) and 64 (refer to objects by their interfaces).
- Joshua Bloch, [Nearly All Binary Searches and Mergesorts are Broken](https://research.google/blog/extra-extra-read-all-about-it-nearly-all-binary-searches-and-mergesorts-are-broken/) (2006): the overflow bug from J05-08, found in the JDK.
- [VisuAlgo](https://visualgo.net/en): animations of hash tables, binary search trees, heaps and graph searches.
- [Topological sorting](https://en.wikipedia.org/wiki/Topological_sorting) on Wikipedia: Kahn's algorithm and an alternative based on DFS.
