# Testing

Tests are how you change code without fear, and how you check code that someone (or something) else wrote. That's even more true now that AI assistants write so much code. A test suite that only checks the happy path tells you very little.

## Structure

```java
@DisplayName("LRU cache")
class LruCacheTest {

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("get() marks an entry as recently used, so it survives the next eviction")
        void getRefreshesRecency() {
            // given
            var cache = new LruCache<String, Integer>(2);
            cache.put("a", 1);
            cache.put("b", 2);

            // when
            cache.get("a");
            cache.put("c", 3);

            // then
            assertThat(cache.keys()).containsExactly("a", "c");
        }
    }
}
```

- **One behavior per test.** When it fails, its name tells you what broke.
- **Name tests after behavior**, as a sentence: "rejects a capacity below 1". Not "testConstructor2".
- **Given / when / then**: set up, act once, check. Separate the three parts with blank lines; the comments are optional.
- **Group with `@Nested`**: by method, by scenario, or by kind (basic / edge cases / errors).
- **Tests are documentation.** Someone reading only the test names should understand the rules.

## Assertions (AssertJ)

- Be specific: `containsExactly("a", "c")` checks content *and* order, while `contains("a")` checks almost nothing.
- Check exceptions by type and message:
  ```java
  assertThatThrownBy(() -> new LruCache<>(0))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("capacity");
  ```
- Add context to assertions in loops: `assertThat(actual).as("step %d", step).isEqualTo(expected)`.

## Cover more than the happy path

Go through this checklist for every unit you test:

- empty input · a single element · duplicates · `null`
- boundaries: 0, 1, the maximum, and just inside and just outside every limit
- negative numbers · overflow · very large input
- ordering, and whether sorting is stable
- invalid arguments, with the exact exception type
- immutability: can a caller change your object through what it returned?
- state after a failed operation: did a failed transfer leave both accounts unchanged?

Bugs cluster at boundaries. If a rule says "between 8 and 64 characters", test 7, 8, 64 and 65.

## Test data

- **Build test data with small factory methods** (`validPasswordOfLength(8)`, `anOrder().withStatus(PAID)`), not long copy-pasted setups.
- **No shared mutable fixtures between tests.** Each test creates what it needs.
- **Make tests deterministic**: fixed `Clock`s, fixed random seeds (`new Random(42)`), `@TempDir` for files. Never `Thread.sleep` to wait for something: wait on a latch or a future with a timeout.
- **Parameterized tests** for tables of examples: `@ParameterizedTest` with `@ValueSource` or `@CsvSource`.

## Mocks

Use a mock (Mockito) for a **collaborator at a boundary**: a repository, a mail sender, a payment client. The question your test answers is "did my code talk to it correctly?".

- **Don't mock value objects or simple classes you own.** Use the real thing.
- **Verify interactions only when the interaction *is* the behavior** ("sends exactly one welcome mail, after saving"). Otherwise, assert on results.
- **Prefer a small fake** (an in-memory repository) when many tests need realistic behavior from a collaborator.
- Too many mocks in one test usually means the class under test does too much.

## Test levels

| Level | Tests | Speed | Share |
|---|---|---|---|
| Unit | one class, collaborators mocked or faked | milliseconds | most tests |
| Integration / slice | several real parts together: a controller with its JSON mapping, a repository with a real database | seconds | some |
| End-to-end | the whole application | slow | a few critical paths |

Part 2 covers Spring's slice tests (`@WebMvcTest`, `@DataJpaTest`) and real databases in tests with Testcontainers.
