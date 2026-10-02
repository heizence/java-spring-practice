# S13 · Testing strategy

⏱ 2 sessions · week 13 · 2 exercises

Most modules so far came with their tests. In real work you write them, and you decide which kind to write. A Spring service has risks in places a plain unit test can't see: a JSON field with the wrong name, a request parameter that never reaches the service, a query that sorts in the wrong order or counts the wrong rows. This module shows which kind of test catches which risk, how to make Spring tests fast, and what a good test asserts. The exercises give you a finished order API and finished order queries, and you write the tests that would catch the bugs someone will introduce next month. This module's copy of the bookstore adds that order API; to keep the tests focused, it has no login (S12). Read lesson sections 1–4 before S13-01 and sections 5–10 before S13-02.

## Lesson

### 1. Three kinds of tests

| Kind | Starts | Catches | Speed |
|---|---|---|---|
| **Unit test** | no Spring: `new`, plus fakes or mocks for collaborators | business rules in entities and services | milliseconds |
| **Slice test** | one layer of Spring: `@WebMvcTest` (web) or `@DataJpaTest` (JPA) | what that layer does with Spring: JSON, validation, binding and error responses; SQL, mappings and queries | about a second to start, then fast |
| **Integration test** | the whole application: `@SpringBootTest` | use cases across the layers: transactions, side effects, wiring, query counts | seconds to start, once per configuration |

The usual advice is a *pyramid*: many unit tests at the bottom, fewer slice tests, a few integration tests at the top. Lower tests are faster and point at the broken line; higher tests prove that the parts work together. How wide each level should be depends on where a service keeps its risk. A service with rich business rules needs many unit tests. A thin service that mostly moves data between JSON and MySQL keeps most of its risk in mappings, SQL and configuration, which only slice and integration tests can see.

Unit tests you know from J03. The rest of this lesson is about the two slices and the integration test.

### 2. Controller slices: `@WebMvcTest` and `@MockitoBean`

```java
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    MockMvcTester mvc;            // sends requests to the controller, without a network

    @MockitoBean
    OrderService orderService;    // a Mockito mock replaces the real service bean
}
```

`@WebMvcTest(OrderController.class)` starts the web layer for one controller: the controller itself, `@RestControllerAdvice` classes such as `GlobalExceptionHandler`, Jackson, bean validation, converters, filters, and Spring Data's web support (`Pageable`, `PagedModel`). It doesn't start services, repositories, other `@Configuration` classes or a database. (That's why `BookstoreApplication` carries no other annotations: whatever sits on the application class starts in every slice.)

The controller needs an `OrderService`, which the slice doesn't create. `@MockitoBean` puts a Mockito mock in its place. (It replaced `@MockBean`, which Spring Boot 4 removed.) Spring resets the mock after every test.

```java
when(orderService.findOrder(201L)).thenReturn(order);                        // stub an answer
when(orderService.findOrder(999L)).thenThrow(new BusinessException(ErrorCode.ORDER_NOT_FOUND, "Order 999 was not found."));

verify(orderService).findMemberOrders(7L, OrderStatus.PAID, PageRequest.of(2, 5)); // what reached the service
verifyNoInteractions(orderService);                                          // invalid input must stop before it
```

A method you didn't stub returns `null` (or an empty collection or `Optional`). A controller that uses that `null` fails with a `500`. So when a stub doesn't match the arguments the controller really passed, the test sees a `500` instead of the expected answer: a strong hint that the controller passed something you didn't expect. Records compare by value, so `when(orderService.placeOrder(expectedRequest))` also checks that the JSON became exactly that request.

A controller slice tests the controller's **HTTP contract**: paths and methods, status codes, headers, the JSON body (names, formats, nesting), which values reach the service and with which defaults, validation, and the error responses. The service's own logic belongs to other tests.

### 3. MockMvcTester and JSON assertions

`MockMvcTester` (Spring Framework 6.2) sends a request through the whole Spring MVC stack and checks the result with AssertJ:

```java
assertThat(mvc.post().uri("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
        { "memberId": 7, "lines": [ { "bookId": 1, "quantity": 2 } ] }"""))
        .hasStatus(HttpStatus.CREATED)
        .hasHeader(HttpHeaders.LOCATION, "/api/orders/201")
        .bodyJson().isLenientlyEqualTo("""
                { "id": 201, "status": "PENDING", "totalAmount": 72000 }""");
```

- `assertThat(mvc.get().uri("/api/orders/201"))` runs the request. When you need the result twice (to read a header and follow it, for example), `MvcTestResult result = mvc.get().uri(…).exchange();` keeps it.
- **Status and headers:** `hasStatusOk()`, `hasStatus(HttpStatus.CREATED)`, `hasHeader(name, value)`, `hasContentType(MediaType.APPLICATION_PROBLEM_JSON)`.
- **The whole body:** `bodyJson().isStrictlyEqualTo(json)` requires exactly these fields, and arrays in this order. `isLenientlyEqualTo(json)` requires at least these fields; extra fields and the order of array elements don't matter.
- **One value:** `bodyJson().extractingPath("$.lines[0].title").isEqualTo("Effective Java")`. The path is JSONPath: `$` is the root, `.name` a field, `[0]` an element, `[*]` every element: `extractingPath("$[*].title").asArray().containsExactly(…)`.
- **Numbers** come back as `Integer` or `Long`, depending on their size, so compare them as numbers: `hasPathSatisfying("$.page.totalElements", total -> assertThat(total).asNumber().isEqualTo(11))`.
- **As an object:** `bodyJson().convertTo(OrderResponse.class)` reads the body into a record for ordinary AssertJ checks.

Strict comparisons pin down the API exactly: a renamed field or an extra field fails the test, which is what you want for a contract. Lenient comparisons keep a test focused on the part it is about, such as the `code` and `errors` of a `400`.

### 4. What to assert

| Assert | Why | Example |
|---|---|---|
| the status | clients decide what to do by it | `hasStatus(HttpStatus.CREATED)` |
| headers | a create answers with the new resource's `Location` | `hasHeader(HttpHeaders.LOCATION, "/api/orders/201")` |
| the body | field names and formats *are* the API | `bodyJson().isStrictlyEqualTo(…)` |
| an error's `code` | clients compare codes, never messages | `extractingPath("$.code").isEqualTo("OUT_OF_STOCK")` |
| what reached a collaborator | binding, defaults and validation happen before the service | `verify(…)`, `verifyNoInteractions(…)` |
| side effects in the database | a response can be right while the data is wrong | the stock went from 10 to 8 |
| the number of SQL statements | an N+1 problem doesn't change the response | `assertThat(sqlStatements.count()).isEqualTo(3)` |

Two habits make assertions strong:

- **Use different values for different things.** Order 201 of member 7, not order 1 of member 1: a `Location` header that uses the wrong id is invisible when both ids are 1.
- **Check the whole answer a client sees,** not one field of it. "The status is 404" passes for a `404` with the wrong `code`, or with no body at all.

### 5. Repository slices: `@DataJpaTest` with Testcontainers

```java
@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaAuditingConfiguration.class, ClockConfiguration.class})
class OrderRepositoryTest {

    @Autowired
    OrderRepository orders;

    @Autowired
    TestEntityManager entityManager;  // persist, find, flush and clear, for test data
}
```

`@DataJpaTest` starts JPA, Hibernate, the Spring Data repositories and Flyway, and nothing else. Each test runs in a transaction that is rolled back. The imports matter:

- `TestcontainersConfiguration` provides the MySQL container. Spring Boot keeps a database that comes from `@ServiceConnection` instead of swapping in an embedded one.
- `JpaAuditingConfiguration` and `ClockConfiguration` turn on the auditing that fills `created_at` and `updated_at`. A slice doesn't scan `@Configuration` classes, so without them, saving a `Member` fails on `created_at`, which is `NOT NULL`.

Flyway also loads the seed data, so the tables are not empty. Create the rows your test is about (with a member of your own, nobody else's orders get in the way), and assert on those rows.

Three things about JPA that matter in repository tests:

- **A JPQL query flushes first.** Before it runs a query, Hibernate writes pending changes of the tables the query reads. Plain SQL through `JdbcClient` doesn't trigger that flush, and `find` returns an entity from the persistence context without asking the database. To test what's really stored, call `entityManager.flush()` and `entityManager.clear()`, then load again.
- **Entities compare by identity.** Inside one test, a query returns the same instances you saved. Comparing ids (`extracting(Order::getId)`) keeps a test correct after a `clear()` as well.
- **A `Page` runs a second query for its total,** the *count query*, but only when Spring Data can't work out the total from the page itself. A first page that isn't full needs no count. To test a custom count query, ask for a page that is smaller than the number of matching rows.

### 6. Full integration tests: `@SpringBootTest`

This course's [`@IntegrationTest`](src/test/java/com/example/bookstore/support/IntegrationTest.java) is a meta-annotation: one annotation that stands for several. It combines `@SpringBootTest` (the whole application), `@AutoConfigureMockMvc` (for `MockMvcTester`), the MySQL container, a fixed `TestClock`, a `SqlStatementCounter`, and `@Transactional` (each test rolls back).

[`OrderApiTest`](src/test/java/com/example/bookstore/order/OrderApiTest.java) is an example; read it now. It isn't part of an exercise. It checks what neither slice can: that placing an order really takes the copies out of stock, that a failed order saves nothing, and that a page of orders costs three SQL statements, not one per order.

A test that rolls back hides some behavior, so keep these blind spots in mind:

- **Nothing is written before a flush.** The service's changes stay in Hibernate's memory until the commit, and the test never commits. Call `entityManager.flush()` before you check the database with plain SQL (`OrderApiTest` does).
- **Lazy loading always works** inside the test's transaction, even where production code would throw `LazyInitializationException`.
- **After-commit listeners never run**, because nothing commits. Tests that need real commits, such as tests of concurrent requests (S08) and of after-commit events (S09), opt out of the test transaction (`@Transactional(propagation = Propagation.NOT_SUPPORTED)`) and restore the data afterwards (`seedData.restore()`).

### 7. Context caching: keeping the suite fast

Starting an application context takes seconds. The Spring TestContext framework caches every context it starts, keyed by the test class's configuration: its annotations, imports, `@MockitoBean`s, properties and profiles. Classes with the same configuration share one context; each difference starts another one.

| Creates another context | Instead |
|---|---|
| `@MockitoBean` on an `@IntegrationTest` class | mock only in slices; in integration tests, use the real beans (or one shared configuration of fakes) |
| `@TestPropertySource` or `properties = …` on one class | put test settings in one shared place |
| `@DirtiesContext` | clean up the data instead of throwing the context away |
| a test container per class (`@Container`) | one container for the whole JVM, as `TestcontainersConfiguration` does |

This module's tests start three contexts: the web slice of `OrderControllerTest`, the JPA slice of `OrderRepositoryTest`, and the whole application for `OrderApiTest`. Every further `@IntegrationTest` class would reuse the third one for free. The cache keeps up to 32 contexts; to see its hits and misses, set `logging.level.org.springframework.test.context.cache: debug`.

### 8. Test data builders

Every repository test needs a member, a book and orders in particular states. Copying that setup into every test hides what the test is about. A *test data builder* gives every value a sensible default, so that each test states only what matters to it:

```java
Order paid = anOrder(buyer, effectiveJava).placedAt("2026-06-02T10:00:00Z").status(OrderStatus.PAID).build();
entityManager.persist(paid);
```

[`OrderBuilder`](src/test/java/com/example/bookstore/order/OrderBuilder.java) builds orders through the entity's own methods (`Order.place`, `addLine`, `markPaid`, `ship`), so it can only build orders that the application could have built. By default an order is `PENDING`, has one copy of the book, and was placed on 2026-06-01, before every seeded order.

Test data must also be able to tell right from wrong:

- **Save rows in a different order than the one you expect back.** Saved oldest first, the orders come back "newest first" only if the query really sorts.
- **Put a row exactly on every boundary,** and one just inside it: an order placed exactly at the cutoff, and one a microsecond before.
- **Add the rows that must stay out:** another member's orders, an order in the wrong status.
- **Make the expected numbers unique:** a total of 108,000 won can only come from the right two orders.

### 9. Which test for which risk

| Risk | Test |
|---|---|
| a business rule: stock can't go negative, a coupon expires | a unit test of the entity or service |
| JSON names and formats, status codes, headers, validation, parameter binding, error responses | `@WebMvcTest` with the service mocked |
| a query's filter, order, paging, boundaries and `NULL` handling; an entity mapping | `@DataJpaTest` against MySQL |
| a use case across layers: transactions, rollbacks, side effects, N+1 queries | `@SpringBootTest` (`@IntegrationTest`) |
| locking and concurrent requests | `@SpringBootTest` with real commits (S08) |
| who may call what | a security test with a real filter chain (S12) |

Don't test the framework: Spring Data's `findById` works. Test what you wrote: your queries, your mappings, your contract.

### 10. Why the exercises use mutants

How do you know that your tests are good? Run them against code that is wrong. A *mutant* is a copy of the code with one realistic mistake; a test suite *catches* it when at least one test fails. This is *mutation testing*, and tools such as [PIT](https://pitest.org/) generate mutants automatically. The exercises use hand-written mutants instead: mistakes that people really make.

## TypeScript/NestJS → Spring

| NestJS / Jest | Spring | Watch out for |
|---|---|---|
| `Test.createTestingModule({ controllers: [OrdersController], providers: [{ provide: OrdersService, useValue: mock }] })` | `@WebMvcTest(OrderController.class)` + `@MockitoBean OrderService orderService` | Spring adds the real web layer: JSON, validation and the exception handler are part of the test. |
| supertest: `request(app.getHttpServer()).post('/orders').send(body).expect(201)` | `mvc.post().uri("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body)` + `hasStatus(HttpStatus.CREATED)` | MockMvc calls the controller without a network or a server. |
| `jest.fn().mockResolvedValue(order)` | `when(orderService.findOrder(201L)).thenReturn(order)` | An unstubbed method returns `null`, which usually ends in a `500`. |
| `expect(fn).toHaveBeenCalledWith(7, 'PAID')` | `verify(orderService).findMemberOrders(7L, OrderStatus.PAID, …)` | Mockito compares arguments with `equals`. |
| `expect(res.body).toMatchObject({ … })` | `bodyJson().isLenientlyEqualTo("…")` | |
| `expect(res.body).toEqual({ … })` | `bodyJson().isStrictlyEqualTo("…")` | |
| an e2e test with a real database (testcontainers for Node) | `@SpringBootTest` + Testcontainers, here `@IntegrationTest` | Each test configuration starts its own application context. |
| truncating tables in `afterEach` | a test transaction that rolls back | Rolling back hides flushes, lazy loading and after-commit events. |
| factories (fishery, `@faker-js/faker`) | test data builders such as `OrderBuilder` | Prefer fixed, meaningful values to random ones. |

## In real projects

- A test suite puts each risk into the cheapest test that can see it, uses the real database engine, and keeps the number of application contexts low. See [Testing Spring applications](../../docs/handbook/spring-testing.md).
- Test names state behavior, each test checks one behavior, and test data comes from small builders. See [Testing](../../docs/handbook/testing.md).
- Rolled-back tests have blind spots: flushing, lazy loading and after-commit listeners. See [Transactions](../../docs/handbook/transactions.md#testing-transactional-code).
- Error responses are part of the API contract, so tests check their status and `code`. See [REST API design](../../docs/handbook/rest-api-design.md#errors-problem-details).
- List endpoints have a test that counts SQL statements. See [JPA entities and repositories](../../docs/handbook/jpa-entities.md#the-n1-problem).
- Controllers can be tested with a mocked service because they only translate HTTP and call one service method. See [Spring layering](../../docs/handbook/spring-layering.md).

## Exercises

### S13-01 · Write tests: a controller slice

**Write tests** · 30 min · [`OrderController.java`](src/main/java/com/example/bookstore/order/OrderController.java) → tests in [`OrderControllerTest.java`](src/test/java/com/example/bookstore/order/OrderControllerTest.java)

**Ticket: pin down the order API.** Next sprint, the team moves the order API to a new error format and splits `OrderService`. The API itself must not change. Before anyone touches it, write fast tests that pin down its contract: a web slice with the service mocked.

The controller, its request and response records and `GlobalExceptionHandler` are finished and correct. The contract is in the Javadoc of `OrderController` and `PlaceOrderRequest`; the error format is in [`spring/BOOKSTORE.md`](../BOOKSTORE.md#api-conventions). The test class already starts the slice: `mvc` sends requests, and `orderService` is the mock.

**How you're scored:** as in J03-03. `./score s13-01` runs your tests against the real code, where every test must pass, and then against six broken versions of it. A broken version is *caught* when at least one of your tests fails on it. Uncaught ones come with a hint; caught ones reveal what the bug was.

**You'll practice:** `@WebMvcTest`, `@MockitoBean`, `MockMvcTester`, JSON assertions, stubbing and verifying a mock.

<details>
<summary>Hint 1</summary>

Start with one happy path per endpoint, and check everything a client sees: the status, the headers, the whole JSON body. Stub the service with the exact request you expect, so a binding mistake shows up as a failed test.
</details>

<details>
<summary>Hint 2</summary>

For each request parameter, ask three questions: does its value reach the service, what happens when it's missing, and what happens when it's invalid?
</details>

<details>
<summary>Hint 3</summary>

For invalid input, check the response's `code` and `errors`, and check with `verifyNoInteractions(orderService)` that nothing reached the service.
</details>

**Done?** Compare with the reference tests on the `solutions` branch. Which of your tests would fail if someone renamed a field of `OrderResponse`? Which of them would still pass if the service were never called?

### S13-02 · Write tests: a repository slice

**Write tests** · 30 min · [`OrderRepository.java`](src/main/java/com/example/bookstore/order/OrderRepository.java) → tests in [`OrderRepositoryTest.java`](src/test/java/com/example/bookstore/order/OrderRepositoryTest.java)

**Ticket: test the order queries before they change.** Three custom queries in `OrderRepository` feed a member's order history (`findMemberOrders`), the "total spent" badge on the profile page (`totalSpentBy`), and a nightly job that cancels abandoned orders (`findPendingOrderedBefore`). None of them has a test, and the database team wants to rewrite them for speed. Write repository tests against MySQL that would catch a rewrite that changes their results.

The queries are finished and correct; their Javadoc states the rules. The test class already starts the JPA slice: `orders` is the repository, and `entityManager` saves test data. [`OrderBuilder`](src/test/java/com/example/bookstore/order/OrderBuilder.java) builds orders.

**How you're scored:** as in S13-01, against five broken versions of `OrderRepository`.

**You'll practice:** `@DataJpaTest` with Testcontainers, `TestEntityManager`, a test data builder, and test data that finds wrong orders, wrong filters and wrong boundaries.

<details>
<summary>Hint 1</summary>

Give every test a member of its own: `entityManager.persist(Member.register("buyer@example.org", "Test Buyer", "{noop}secret"))`. For the book, `entityManager.find(Book.class, 1L)` loads Effective Java, which costs 36,000 won (`spring/BOOKSTORE.md`), so two copies total 72,000 won.
</details>

<details>
<summary>Hint 2</summary>

`findPendingOrderedBefore` looks at every member's orders. Use times before August 2026: every seeded order was placed in August or September 2026.
</details>

<details>
<summary>Hint 3</summary>

A test can only catch a wrong sort, a wrong count or a wrong boundary if its data makes the difference visible. Lesson section 8 lists four ways.
</details>

**Done?** Compare with the reference tests on the `solutions` branch. Which of your tests would still pass if the queries ignored the member completely? What does the "same moment" rule in the Javadoc of `findMemberOrders` protect against?

## Review questions

1. What does `@WebMvcTest(OrderController.class)` start, and what doesn't it start?
2. A test stubs `orderService.findMemberOrders(7L, OrderStatus.PAID, PageRequest.of(2, 5))`, but the controller calls the service with different arguments. What does the test see, and why?
3. Why does a `@DataJpaTest` in this course import `TestcontainersConfiguration`, `JpaAuditingConfiguration` and `ClockConfiguration`?
4. A test checks the `totalElements` of a page with 3 matching orders and a page size of 10. Why can't it catch a broken count query?
5. Two classes are annotated with `@IntegrationTest`; one of them also declares `@MockitoBean PaymentClient`. How many application contexts start, and why does that matter?
6. A rolled-back integration test places an order and then reads the stock with `JdbcClient`. Why can it read the old value, and how do you fix the test?
7. When is `isLenientlyEqualTo` the better choice, and when `isStrictlyEqualTo`?
8. Which kind of test would you write for each: (a) a coupon can't be used after it expires, (b) the JSON name of a response field, (c) a query must leave out cancelled orders, (d) a failed order must not change the stock?
9. Why does `OrderBuilder` create orders through `Order.place` and `markPaid` instead of setting their fields?
10. Your controller test uses member 1 and order 1. Which kind of bug can it never catch?

<details>
<summary>Answers</summary>

1. The web layer for that controller: the controller, `@RestControllerAdvice` classes (here `GlobalExceptionHandler`), Jackson, bean validation, converters, filters and Spring Data's web support. It starts no services, repositories, other `@Configuration` classes or database; the controller's collaborators must be mocked with `@MockitoBean`.
2. The mock doesn't match those arguments, so it returns `null`. The controller passes `null` on (here into `new PagedModel<>(…)`), which fails, and the global handler answers `500`. A `500` where you expected `200` often means that the controller called the service with different arguments than you expected.
3. A slice starts without the application's `@Configuration` classes. `TestcontainersConfiguration` provides the MySQL container (without it, there's no database). The auditing configuration and the clock fill `created_at` and `updated_at`; without them, saving an entity fails because those columns are `NOT NULL`.
4. Spring Data runs the count query only when it can't work out the total from the page. A first page with 3 of 10 possible rows is obviously the only page, so the total is 3 without counting. Ask for a page of 2 to make it count.
5. Two: the context cache's key includes the set of mocked beans, so the second class gets a context of its own. Each context costs seconds to start and holds its own connection pool; across a big suite, such one-off combinations make the tests slow.
6. The test's transaction never commits, so Hibernate keeps the stock change in memory until something flushes it. A JPQL query on the same table would flush first, plain SQL doesn't. Call `entityManager.flush()` before reading with `JdbcClient`.
7. Strict when the test pins down a contract, such as a whole response body: an extra or renamed field should fail it. Lenient when the test is about one part, such as the `code` and `errors` of a `400`, and shouldn't break because of unrelated fields.
8. (a) A unit test of `CouponIssue.use`. (b) `@WebMvcTest`. (c) `@DataJpaTest` against MySQL. (d) An integration test (`@SpringBootTest`), because the rollback happens in the service's transaction, across the service and two repositories.
9. The entity's methods enforce its rules, such as `PENDING → PAID → SHIPPED`. Built through them, test data can't be in a state that the application could never produce, and the builder breaks loudly if those rules change.
10. A mix-up of two ids, such as a `Location` header that uses the member's id instead of the order's: both are 1, so the wrong value looks right.
</details>

## Further reading

- Spring Boot reference: [Testing Spring Boot applications](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html), the list of [test slices](https://docs.spring.io/spring-boot/appendix/test-auto-configuration/slices.html), and [Testcontainers](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html)
- Spring Framework reference: [MockMvcTester](https://docs.spring.io/spring-framework/reference/testing/mockmvc/assertj.html), [context caching](https://docs.spring.io/spring-framework/reference/testing/testcontext-framework/ctx-management/caching.html), [`@MockitoBean`](https://docs.spring.io/spring-framework/reference/testing/testcontext-framework/bean-overriding.html)
- Ham Vocke: [The Practical Test Pyramid](https://martinfowler.com/articles/practical-test-pyramid.html)
- Nat Pryce: [Test Data Builders](http://www.natpryce.com/articles/000714.html)
- [PIT](https://pitest.org/), mutation testing for Java
