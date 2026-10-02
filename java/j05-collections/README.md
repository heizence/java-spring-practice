# J05 · Collections and data structures

⏱ 10 sessions · weeks 3–4 · 9 exercises

<!-- AUTHOR: intro, lesson, TypeScript → Java table, "In real projects", and exercises J05-01 … J05-08 go here.
     Keep the J05-09 section below as it is (it is the format example for every exercise section). -->

## Exercises

### J05-09 · LRU cache

**Build** · 30 min · [`LruCache.java`](src/main/java/practice/j05/ex09/LruCache.java)

A cache with a size limit has to decide what to throw away when it's full. *Least recently used* (LRU) evicts the entry nobody has touched for the longest time. It's the policy behind most in-memory caches, including the ones you'll plug into Spring in S10.

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
