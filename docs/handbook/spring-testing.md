# Testing Spring applications

[Testing](testing.md) covers the basics that apply everywhere. This page adds what's specific to Spring.

## Three kinds of tests

| Kind | Starts | Use for | Speed |
|---|---|---|---|
| **Unit test** | no Spring at all: `new OrderService(fakeRepository, …)` | business rules in entities and services | milliseconds |
| **Slice test** | one layer of Spring: `@WebMvcTest` (controllers, JSON, validation, error handling), `@DataJpaTest` (repositories and JPA mappings, with a real database) | one layer's Spring behavior | about a second |
| **Integration test** | the whole application: `@SpringBootTest` | use cases end to end, transactions, security, wiring | seconds |

Write most tests as unit tests, and add slice and integration tests where Spring's behavior is part of what you check. Mapping, transactions, JSON and security can only be tested with Spring.

## Use the real database

Test with the same database as production (MySQL here), started by [Testcontainers](https://testcontainers.com/) in Docker. In-memory databases like H2 behave differently in exactly the places that matter: locking, isolation, SQL dialect and constraints.

```java
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {
    @Bean
    @ServiceConnection // Spring Boot points the DataSource at this container
    MySQLContainer mysql() {
        return new MySQLContainer("mysql:9.7");
    }
}
```

## Keep Spring tests fast: context caching

Starting an application context takes seconds. Spring caches each context and reuses it for every test class with **the same configuration**. Each variation creates a new context:

- a different set of `@MockitoBean`s,
- different `properties` or profiles,
- `@DirtiesContext`, which throws the context away.

Keep test configurations uniform, for example through one shared annotation like `@IntegrationTest`, and the suite stays fast.

## Calling the API: MockMvcTester

`MockMvcTester` sends requests through the full Spring MVC stack without a real HTTP server, with AssertJ-style assertions:

```java
@IntegrationTest
class BookApiTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    @DisplayName("returns a book by id")
    void getBook() {
        assertThat(mvc.get().uri("/api/books/1"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.title").isEqualTo("Effective Java");
    }
}
```

## Mocks in Spring tests

- **`@MockitoBean`** replaces a bean in the context with a Mockito mock (it replaced `@MockBean`, which Spring Boot 4 removed). Use it in slice tests (`@WebMvcTest` with a mocked service) and for external systems (a payment client).
- Don't mock what you can run for real: your own repositories with Testcontainers, your own services.

## Data isolation

Tests share one database, so each test must leave it as it found it:

- **`@Transactional` on the test class** rolls back each test's changes. MockMvc requests run in the test's thread, so they join that transaction and are rolled back too. See the caveats in [Transactions](transactions.md#testing-transactional-code).
- **Tests that need real commits** (concurrency tests, after-commit events) clean up after themselves.
- **Rely only on documented seed data** ([spring/BOOKSTORE.md](../../spring/BOOKSTORE.md)), and create anything else in the test.

## Testing time, async code and queries

- **Time:** inject a `Clock` bean everywhere, and replace it with a fixed clock in tests.
- **Async code:** wait for the result (a future, a latch) with a timeout instead of sleeping.
- **SQL statements:** count them (the bookstore's `SqlStatementCounter`) to catch N+1 problems. A test that only checks the response passes even when 101 queries produce it.
