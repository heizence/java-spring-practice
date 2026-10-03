# S11 · Calling other services

⏱ 3 sessions · week 12 · 3 exercises

The bookstore doesn't work alone. It asks a carrier what shipping costs, the central warehouse how many copies it has, and the publishers' catalog for book data. Every such call crosses a network: it can be slow, fail halfway, or get an error that means "try again later" or "your request is wrong". A backend that handles these cases badly fails in the worst way: one slow partner ties up all of its request threads, and the whole shop stops answering. This module teaches Spring's HTTP clients (`RestClient` and HTTP interface clients), how Spring Boot configures them, timeouts, turning remote errors into your own exceptions, and protecting both sides with retries, backoff and concurrency limits. It also shows two ways to test clients: a mock server for exact requests, and a real local server for timeouts and retries.

This module has no database, so its tests run without Docker and take about 20 seconds. Each exercise names the lesson sections to read first.

## Lesson

### 1. `RestClient`

`RestClient` (since Spring Framework 6.1) is Spring's HTTP client for normal, blocking code. Its fluent style is close to `fetch` or axios. You'll also meet two older or different clients in existing code: `RestTemplate`, the original client (new code uses `RestClient`; Spring plans to retire `RestTemplate`), and `WebClient`, the reactive client.

Build it from the `RestClient.Builder` that Spring Boot provides:

```java
@Component
public class ShippingClient {

    private final RestClient restClient;

    public ShippingClient(RestClient.Builder builder, ShippingProperties properties) {
        this.restClient = builder.baseUrl(properties.baseUrl()).build();
    }
}
```

Spring Boot's builder comes with the JSON converters (Jackson 3, with Boot's settings), the HTTP library and the timeouts from `spring.http.clients.*` (section 5), metrics when Actuator is present, and the hooks that tests use to replace the network (section 10). `RestClient.create()` gives you none of that. The builder is a *prototype* bean: every injection gets a fresh copy, so one client's settings never leak into another's.

A request:

```java
ShippingEstimate estimate = restClient.get()
        .uri(uri -> uri.path("/v1/estimates")
                .queryParam("postalCode", postalCode)
                .queryParam("weightGrams", weightGrams)
                .build())
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .body(ShippingEstimate.class);
```

- **URIs.** `uri("/v1/stock/{isbn}", isbn)` fills in a URI template, and the URI builder above adds query parameters. Both encode the values. Never build URLs by concatenating strings: a value with `&`, a space or a `/` would change the request.
- **Bodies.** `body(Type.class)` reads the body; `body(new ParameterizedTypeReference<List<Book>>() {})` reads a generic type; `toEntity(Type.class)` gives status and headers too; `toBodilessEntity()` ignores the body. To send one: `.post().contentType(MediaType.APPLICATION_JSON).body(request)`.
- **Headers.** `.header("X-Request-Id", id)` for one request, `builder.defaultHeader(…)` for every request of the client.

### 2. JSON to records

Jackson maps a JSON object to a record by the names of the record's components. Spring Boot's configuration ignores JSON properties that the record doesn't have, so a partner that adds a field doesn't break you. A property that is missing becomes `null`, or `0` for a primitive, without an error.

When the partner names things differently, map the name on the component:

```java
public record ShippingEstimate(String carrier, long fee, @JsonProperty("estimated_days") int estimatedDays) {
}
```

Jackson 3 moved to the package `tools.jackson`, but its annotations stay in `com.fasterxml.jackson.annotation`, so `@JsonProperty` looks as before. Dates such as `"2018-10-18"` become `LocalDate` without extra setup. Keep such records in the client's package: they describe the partner's API, not your domain.

### 3. Errors: `retrieve()`, status handlers and `exchange()`

| What happens | What `retrieve()` does |
|---|---|
| `2xx` | reads the body |
| `4xx` | throws `HttpClientErrorException`, or a subclass such as `HttpClientErrorException.NotFound` |
| `5xx` | throws `HttpServerErrorException`, or a subclass such as `HttpServerErrorException.ServiceUnavailable` |
| no answer: connection refused, connection reset, timeout | throws `ResourceAccessException`, which wraps the `IOException` |

Both HTTP exceptions extend `RestClientResponseException`, which has the status code and the response body (`getResponseBodyAsString()`).

`onStatus` replaces the default for the statuses it matches:

```java
.retrieve()
.onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
    throw new ShippingUnavailableException("The carrier answered %d.".formatted(response.getStatusCode().value()));
})
.body(ShippingEstimate.class);
```

For full control, `exchange` hands you the raw response and does no error handling at all:

```java
Optional<ShippingEstimate> estimate = restClient.get().uri(…).exchange((request, response) ->
        response.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)
                ? Optional.empty()
                : Optional.ofNullable(response.bodyTo(ShippingEstimate.class)));
```

### 4. Turning remote errors into your own exceptions

The rest of the application shouldn't know which HTTP library the client uses, or that the carrier answers `503` when it's overloaded. Decide at the client what each outcome means for your domain:

| The partner | It means | The client |
|---|---|---|
| `404` for a postal code it doesn't serve | a normal answer | returns `Optional.empty()` |
| `5xx`, or no answer | "can't tell right now" | throws `ShippingUnavailableException` |
| `400` | our request is wrong: a bug | lets `HttpClientErrorException` through, so the bug is loud |

A caller can then decide what to do: show "shipping is calculated at checkout", or answer its own client with `503`. At your API's edge, map the domain exception in `GlobalExceptionHandler`, with an error code of its own (this module has no endpoint that needs one yet):

```java
@ExceptionHandler(ShippingUnavailableException.class)
public ResponseEntity<ProblemDetail> handleShippingUnavailable(ShippingUnavailableException exception) {
    log.warn("Carrier unavailable: {}", exception.getMessage());
    return problem(ErrorCode.SHIPPING_UNAVAILABLE, "Shipping costs can't be calculated right now.", List.of());
}
```

Never pass a partner's error message on to your own clients unchanged: it may contain internals, and it's written for you, not for them.

### 5. Timeouts

Two timeouts protect every call:

- **Connect timeout:** how long to wait for a connection (the TCP and TLS handshakes). A healthy partner connects in milliseconds; a few seconds at most.
- **Read timeout:** how long to wait for the response once the request is sent. Set it a little above the partner's slow responses (for example its 99th percentile), and well below the time your own client waits for you.

Without them, a partner that hangs keeps your request thread waiting forever. The next requests wait too, the thread pool runs out, and your service stops answering although its own code is fine: a *cascading failure*. With virtual threads, threads don't run out, but memory and the partner's connections do.

Spring Boot applies these properties to every client built from its builders:

```yaml
spring:
  http:
    clients:
      connect-timeout: 2s
      read-timeout: 5s
```

Spring Boot picks the HTTP library itself, in this order, whichever is on the classpath: Apache HttpClient 5, Jetty's client, Reactor Netty, then the JDK's `java.net.http.HttpClient`. This module has none of the others, so it uses the JDK's (`spring.http.clients.imperative.factory` chooses explicitly).

A timeout leaves the outcome unknown: the partner may have done the work and only been slow to answer. For a `GET` that's harmless. For a payment, it's the reason for idempotency keys (S08-06).

### 6. HTTP interface clients

Instead of writing the calls, declare them, and let Spring generate the implementation:

```java
@HttpExchange(url = "/v1", accept = MediaType.APPLICATION_JSON_VALUE)
public interface PublisherCatalogClient {

    @GetExchange("/books/{isbn}")
    PublisherBook findBook(@PathVariable String isbn);

    @GetExchange("/books")
    List<PublisherBook> findBooksByAuthor(@RequestParam String author, @RequestParam int limit);
}
```

- The annotations are the ones you know from controllers, seen from the other side: `@GetExchange`, `@PostExchange`, `@PutExchange`, `@DeleteExchange`; parameters with `@PathVariable`, `@RequestParam`, `@RequestHeader`, `@RequestBody`.
- Return the body type, `ResponseEntity<T>` for status and headers, or `void`.
- Errors behave like `retrieve()`: a `404` throws `HttpClientErrorException.NotFound`.

Spring Framework 7 registers such interfaces as beans in **HTTP service groups**. All interfaces of a group share one `RestClient`:

```java
@Configuration(proxyBeanMethods = false)
@ImportHttpServices(group = "publisher-catalog", types = PublisherCatalogClient.class)
public class PublisherClientConfiguration {
}
```

Spring Boot configures each group's client from `spring.http.serviceclient.<group>`:

```yaml
spring:
  http:
    serviceclient:
      publisher-catalog:
        base-url: https://catalog.publisher.example
        connect-timeout: 1s
        read-timeout: 2s
        default-header:
          X-Api-Key: ${PUBLISHER_CATALOG_API_KEY:local-development-key}
```

The group's timeouts override the defaults of section 5. `${NAME:fallback}` reads an environment variable (any property source, really) and falls back to the value after the colon, so the real key never appears in git ([Configuration](../../docs/handbook/configuration.md)). For settings that properties don't cover, such as a status handler for the whole group, declare a `RestClientHttpServiceGroupConfigurer` bean.

In code written before Spring Framework 7 you'll see the manual way, a `@Bean` method per client:

```java
@Bean
PublisherCatalogClient publisherCatalogClient(RestClient.Builder builder) {
    RestClient restClient = builder.baseUrl("https://catalog.publisher.example").build();
    return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient)).build()
            .createClient(PublisherCatalogClient.class);
}
```

### 7. Retries with backoff

S08 introduced `@Retryable`, `@EnableResilientMethods` and backoff (S08 lesson section 4). For HTTP calls, two questions decide whether a retry is right.

**Can another attempt get a different answer?**

| Retry | Don't retry |
|---|---|
| `503 Service Unavailable`, `502`, `504`: overloaded or restarting | `400`, `401`, `403`, `404`, `409`, `422`: the same request gets the same answer |
| `429 Too Many Requests`, after the time in its `Retry-After` header | `500`: usually a bug that the same request triggers again |
| a connection that failed or timed out, for a request that is safe to repeat | |

**Is the request safe to repeat?** `GET`, `PUT` and `DELETE` are idempotent: repeating them has the same effect as sending them once. A `POST` that creates something isn't, unless the partner supports idempotency keys. A timeout on a `POST` is the dangerous case: the first attempt may have worked.

```java
@Retryable(includes = WarehouseUnavailableException.class, maxRetries = 3, delay = 100, multiplier = 2)
public int availableCopies(String isbn) { … }
```

- Throw a dedicated exception for the retryable case in `onStatus`, and retry only that. Without `includes`, `@Retryable` retries every exception.
- Bound the attempts and the total time: here 4 attempts, waiting 100, 200 and 400 ms in between. Add `jitter` in production, so that many clients that failed together don't retry together.
- Retry in one layer only. If three layers each try four times, one user request can become 4 × 4 × 4 = 64 calls to a partner that is already struggling.

### 8. Concurrency limits with `@ConcurrencyLimit`

```java
@ConcurrencyLimit(4)
public int availableCopies(String isbn) { … }
```

At most 4 calls of the method run at the same time; more calls wait until one finishes. It's like `p-limit(4)` in Node.js, or a `Semaphore` (J08) that Spring manages for you, per bean. It needs the same `@EnableResilientMethods`.

Why limit:

- **To protect the partner.** Partners limit how many requests a client may send at once. Above the limit they answer `429` or `503`, or slow down for everybody.
- **To protect yourself.** Without a limit, a slow partner can hold all of your request threads, and requests that don't need the partner wait too. A limit per partner keeps the damage in one place. This is called a *bulkhead*, after the walls that split a ship into separate compartments.

By default the waiting has no time limit, so combine it with timeouts. `@ConcurrencyLimit(limit = 4, policy = ConcurrencyLimit.ThrottlePolicy.REJECT)` fails at once with an `InvocationRejectedException` instead of waiting. `limitString = "${bookstore.warehouse.max-concurrent-calls}"` reads the limit from configuration.

### 9. Circuit breakers

Retries help when failures are short. When a partner is down for minutes, every request still waits for its timeouts and retries, and the retries add load to a service that is trying to recover. A **circuit breaker** watches the failures: after too many it "opens" and fails every call at once, without calling the partner; after a while it lets a few test calls through ("half-open"), and "closes" again when they succeed. Spring Framework has no circuit breaker of its own. Teams use the Resilience4j library (directly, or through Spring Cloud Circuit Breaker), which also has rate limiters and bulkheads.

### 10. Testing HTTP clients

Two tools, for different questions:

- **`MockRestServiceServer`** checks exactly what the client sends and how it handles each answer, without a network. `@RestClientTest(ShippingClient.class)` starts only that client (plus Jackson) and binds a mock server to the `RestClient.Builder`:

  ```java
  carrier.expect(requestTo("https://api.carrier.example/v1/estimates?postalCode=06236&weightGrams=800"))
          .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
          .andRespond(withSuccess("""
                  { "carrier": "CJ Logistics", "fee": 3000, "estimated_days": 2 }""", MediaType.APPLICATION_JSON));
  ```

  `withStatus(…)`, `withResourceNotFound()` and `withException(new SocketTimeoutException())` simulate the other answers; `verify()` fails if an expected request never came.
- **A real HTTP server on this computer** is the only way to test timeouts, the waits between retries and how many requests arrive at the same time, because those happen in the HTTP library. This module's `support/StubServer` uses the JDK's own `com.sun.net.httpserver.HttpServer`; WireMock is a popular library for the same job. `@DynamicPropertySource` points the client at the server's random port:

  ```java
  @DynamicPropertySource
  static void pointAtTheStubServer(DynamicPropertyRegistry registry) {
      registry.add("bookstore.warehouse.base-url", WAREHOUSE::baseUrl);
  }
  ```

## TypeScript/NestJS → Spring

| TypeScript / NestJS | Spring | Watch out for |
|---|---|---|
| `fetch(url)`, axios | `RestClient` | `retrieve()` throws for `4xx` and `5xx`; `fetch` doesn't |
| axios `baseURL`, `timeout` | `builder.baseUrl(…)`, `spring.http.clients.read-timeout` | without a configured timeout, a call can wait forever |
| NestJS `HttpModule` | the auto-configured `RestClient.Builder` | inject the builder; don't create clients with `create()` |
| a typed client generated from OpenAPI, or ts-rest | an HTTP interface with `@ImportHttpServices` | one group per partner, configured with `spring.http.serviceclient.<group>` |
| `axios-retry`, `p-retry` | `@Retryable` | only for transient errors and requests that are safe to repeat |
| `p-limit(4)` | `@ConcurrencyLimit(4)` | waiting callers block a thread, with no time limit |
| `nock`, `msw` | `MockRestServiceServer`; a stub server for timeouts | a mock can't show timeouts or concurrency |

## In real projects

- Give every remote call a connect and a read timeout, chosen from the partner's real response times. [Configuration](../../docs/handbook/configuration.md)
- Keep base URLs, timeouts and API keys in configuration, and the keys themselves in environment variables or a secret store. [Configuration](../../docs/handbook/configuration.md)
- Translate remote errors at the client into your own exceptions, and decide at your API's edge what your clients see. [Exceptions](../../docs/handbook/exceptions.md) · [REST API design](../../docs/handbook/rest-api-design.md)
- Keep clients and the partner's records in their own package; the rest of the code depends on your exceptions and types, not on the partner's. [Spring layering](../../docs/handbook/spring-layering.md)
- Retry only transient failures of idempotent requests, a bounded number of times, with backoff and jitter, in one layer. Never call a partner inside a database transaction. [Transactions](../../docs/handbook/transactions.md)
- Limit concurrent calls per partner, and add a circuit breaker for partners that fail for minutes at a time.
- Log failed calls with the status, the time they took and the partner's name, never with API keys. Spring Boot records the timer `http.client.requests` for clients built from its builders. [Logging and observability](../../docs/handbook/logging-and-observability.md)
- Test the mapping with `MockRestServiceServer`, and timeouts, retries and limits against a real local server. [Testing Spring applications](../../docs/handbook/spring-testing.md)

## Exercises

### S11-01 · Calling an API with RestClient

**Build** · 30 min · [`ShippingClient.java`](src/main/java/com/example/bookstore/shipping/ShippingClient.java) · [`ShippingEstimate.java`](src/main/java/com/example/bookstore/shipping/ShippingEstimate.java)

**Before you start:** lesson sections 1 to 4.

The checkout page should show what shipping costs before the member pays. The carrier, CJ Logistics, offers an API: `GET /v1/estimates?postalCode=06236&weightGrams=800` answers `{ "carrier": "CJ Logistics", "fee": 3000, "currency": "KRW", "estimated_days": 2 }`, a `404` for postal codes it doesn't serve, and `5xx` when it has problems.

**Ticket:** implement `ShippingClient.estimate`, and make `ShippingEstimate` read the carrier's `estimated_days`. The constructor and the configuration (`bookstore.shipping.base-url`) are ready.

**Acceptance criteria** (the tests in `shipping/ShippingClientTest`):

- The request is `GET /v1/estimates` with the postal code and the weight as query parameters and `Accept: application/json`, and the JSON becomes a `ShippingEstimate`, whatever other properties it has.
- A `404` gives an empty result. A `5xx` throws `ShippingUnavailableException` with the status in its message. No answer at all throws `ShippingUnavailableException` with the network error as its cause. A `400` passes through as `HttpClientErrorException.BadRequest`.
- A postal code that isn't 5 digits, or a weight outside 1 to 30,000 grams, throws `IllegalArgumentException` before any request is sent.
- All rules are in the Javadoc of `estimate`.

**You'll practice:** `RestClient` requests with a URI builder, mapping JSON to a record, `onStatus`, catching `HttpClientErrorException.NotFound` and `ResourceAccessException`, `MockRestServiceServer`.

<details>
<summary>Hint 1</summary>

Check the arguments first, with `Assert.isTrue` or plain `if`s. Then write the call of lesson section 1, and run the tests: the first ones pass before you have handled any error.
</details>

<details>
<summary>Hint 2</summary>

`onStatus(HttpStatusCode::is5xxServerError, …)` covers the carrier's problems. Wrap the whole call in a `try` with two `catch` blocks: `HttpClientErrorException.NotFound` (return `Optional.empty()`) and `ResourceAccessException` (throw `ShippingUnavailableException` with it as the cause).
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Why is a `404` a normal answer here, but a `400` a bug? What would the checkout page show for each outcome?

### S11-02 · HTTP interface clients

**Build** · 30 min · [`PublisherCatalogClient.java`](src/main/java/com/example/bookstore/publisher/PublisherCatalogClient.java) · [`PublisherClientConfiguration.java`](src/main/java/com/example/bookstore/publisher/PublisherClientConfiguration.java) · [`application.yml`](src/main/resources/application.yml)

**Before you start:** lesson sections 5 and 6.

Before the bookstore lists a new book, the catalog team checks the publisher's data in the publishers' shared catalog: `GET /v1/books/{isbn}` and `GET /v1/books?author=…&limit=…`. The catalog wants an API key in the header `X-Api-Key`, and it is known to hang now and then.

**Ticket:** turn `PublisherCatalogClient` into an HTTP interface client, register it in the HTTP service group `publisher-catalog`, and configure the group in `application.yml`: base URL `https://catalog.publisher.example`, connect timeout 1 second, read timeout 2 seconds, and the API key from the environment variable `PUBLISHER_CATALOG_API_KEY` (falling back to `local-development-key`).

**Acceptance criteria** (the tests in `publisher/PublisherCatalogClientTest`, against a real HTTP server on your computer):

- `findBook` sends `GET /v1/books/{isbn}` and maps the JSON to a `PublisherBook`. `findBooksByAuthor` sends `?author=…&limit=…`; an empty array is an empty list.
- Every request has `X-Api-Key` with the value of `PUBLISHER_CATALOG_API_KEY`, and `Accept: application/json`.
- An unknown ISBN throws `HttpClientErrorException.NotFound`.
- A catalog that doesn't answer fails with `ResourceAccessException` after the read timeout of 2 seconds. The connect timeout is 1 second.
- All rules are in the Javadoc of `PublisherCatalogClient`.

**You'll practice:** `@HttpExchange`, `@GetExchange`, `@PathVariable`, `@RequestParam`, `@ImportHttpServices` and HTTP service groups, `spring.http.serviceclient.*`, secrets from environment variables.

<details>
<summary>Hint 1</summary>

Put what both methods share on the interface: `@HttpExchange(url = "/v1", accept = MediaType.APPLICATION_JSON_VALUE)`. Then each method needs one annotation for the request and one per parameter.
</details>

<details>
<summary>Hint 2</summary>

The group name connects the three files: `@ImportHttpServices(group = "publisher-catalog", …)` in the configuration class, and `spring.http.serviceclient.publisher-catalog` in `application.yml`. The test overrides the base URL to reach its server, so a typo in the group name shows up as a request to the real catalog.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Where would you add a second interface for the same catalog? What changes when the catalog's timeouts differ per endpoint?

### S11-03 · Retries and concurrency limits

**Build** · 30 min · [`WarehouseClient.java`](src/main/java/com/example/bookstore/warehouse/WarehouseClient.java) · [`ResilienceConfiguration.java`](src/main/java/com/example/bookstore/common/ResilienceConfiguration.java)

**Before you start:** lesson sections 7 and 8.

The central warehouse's stock API, `GET /v1/stock/{isbn}`, is shared by every shop of the company. When it's overloaded it answers `503` for a moment, and it allows each client at most 4 requests at the same time. Last month the bookstore's nightly stock check sent 200 requests at once, got `503` for most of them, and gave up.

**Ticket:** implement `WarehouseClient.availableCopies`: retry a `503` a bounded number of times with growing waits, never retry other errors, and send at most 4 requests at the same time.

**Acceptance criteria** (the tests in `warehouse/WarehouseClientTest`, against a real HTTP server on your computer that counts the requests):

- `GET /v1/stock/{isbn}` with `Accept: application/json` returns the `available` copies.
- `503, 503, 200` returns the copies after 3 requests; `503, 503, 503, 200` after 4. The waits between the attempts are at least 100, 200 and 400 ms.
- A warehouse that always answers `503`: `WarehouseUnavailableException` after exactly 4 requests, within 2 seconds.
- A `4xx` (`400`, `404`, `429`) throws `HttpClientErrorException` and a `500` throws `HttpServerErrorException`, each after one request.
- 10 calls at the same moment all succeed, and the warehouse never sees more than 4 requests at the same time (and does see 4).
- All rules are in the Javadoc of `availableCopies`.

**You'll practice:** `@Retryable` with `includes`, `maxRetries`, `delay` and `multiplier`, `@ConcurrencyLimit`, `@EnableResilientMethods`, measuring retries and concurrency at a real server.

<details>
<summary>Hint 1</summary>

The call itself is short: `uri("/v1/stock/{isbn}", isbn)`, `retrieve()`, an `onStatus` that throws `WarehouseUnavailableException` for `503` only, and `body(StockLevel.class)`. Then the annotations. If nothing is retried or limited, look at `ResilienceConfiguration`.
</details>

<details>
<summary>Hint 2</summary>

`HttpStatusCode` has `isSameCodeAs(HttpStatus.SERVICE_UNAVAILABLE)`. Retry only `WarehouseUnavailableException`: everything else must reach the caller after one attempt.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: What would happen to the warehouse if 50 bookstore servers each retried 10 times without waiting? Why can't `MockRestServiceServer` test the concurrency limit?

## Review questions

1. Why build a `RestClient` from Spring Boot's `RestClient.Builder` instead of `RestClient.create()`?
2. What does `retrieve()` do with a `404`, a `503`, and a connection that times out?
3. When is `exchange()` a better choice than `retrieve()`?
4. A partner's JSON has `estimated_days`, your record `estimatedDays`. What happens without a mapping, and how do you map it?
5. What is the difference between a connect timeout and a read timeout? What goes wrong in a busy service when a partner hangs and there is no read timeout?
6. Why should a client turn a partner's `5xx` into its own exception, but let a `400` through?
7. How does Spring Boot know the base URL and the timeouts of an HTTP interface client registered with `@ImportHttpServices`?
8. Which failures are worth retrying, and which requests are safe to retry?
9. Three layers of a system each retry a failed call 3 times (4 attempts). How many calls can one user request cause at the bottom layer? What do you do about it?
10. What does `@ConcurrencyLimit(4)` do with the fifth concurrent call? Why is a limit per partner useful even when the partner never complains?
11. What can a test with a real local HTTP server check that a test with `MockRestServiceServer` can't?

<details>
<summary>Answers</summary>

1. Spring Boot's builder brings the configured JSON converters, the HTTP library with the timeouts from `spring.http.clients.*`, metrics, and the test support (`MockRestServiceServer` binds to it). `RestClient.create()` uses plain defaults, without timeouts from your configuration.
2. A `404` throws `HttpClientErrorException.NotFound`, a `503` throws `HttpServerErrorException.ServiceUnavailable`, and a timeout throws `ResourceAccessException` with the `IOException` as its cause.
3. When you want to decide everything yourself from the status, the headers and the body: for example to turn a `404` into `Optional.empty()` without exceptions, or to read an error body in a special format. `exchange()` applies no status handlers at all.
4. Jackson ignores `estimated_days` (Spring Boot ignores unknown properties), and `estimatedDays` stays `0`, silently. Map it with `@JsonProperty("estimated_days")` on the record component.
5. The connect timeout limits the wait for a connection; the read timeout limits the wait for the response once the request is sent. Without a read timeout, each request to the hanging partner holds a thread forever; new requests pile up until the thread pool (or memory, or the connection pool) runs out, and the whole service stops answering.
6. The rest of the application should depend on your domain's meaning ("shipping can't be calculated now"), not on HTTP details of one partner. A `400` means that your own request is wrong, a bug that should fail loudly so that someone fixes it, not be hidden as "partner unavailable".
7. Each `@ImportHttpServices` group has a name, and Spring Boot binds `spring.http.serviceclient.<group>.*` (base URL, timeouts, default headers) to that group's `RestClient`. Settings it doesn't define come from `spring.http.clients.*`.
8. Failures that another attempt can fix: `503`, `502`, `504`, `429` (after `Retry-After`), and connection problems. Not `4xx` errors such as `400` or `404`, and usually not `500`. Only requests that are safe to repeat: `GET`, `PUT`, `DELETE`, or a `POST` with an idempotency key.
9. 4 × 4 × 4 = 64 calls. Retry in one layer only (usually the one closest to the partner), bound the total time, and add backoff with jitter.
10. It waits until one of the 4 running calls finishes (or fails at once with `policy = REJECT`). Even a partner that never complains can become slow; without a limit, calls to it can take all of your threads, so requests that have nothing to do with the partner fail too. The limit keeps the damage in one place (a bulkhead).
11. Behavior that happens in the HTTP library or over time: read and connect timeouts, the waits between retries, how many requests arrive at the same moment, and real connection errors.
</details>

## Further reading

- Spring Framework reference, REST Clients (`RestClient`, HTTP service clients and groups): https://docs.spring.io/spring-framework/reference/integration/rest-clients.html
- Spring Framework reference, Resilience Features (`@Retryable`, `@ConcurrencyLimit`): https://docs.spring.io/spring-framework/reference/core/resilience.html
- Spring Boot reference, Calling REST Services (the builder, HTTP service clients, `spring.http.clients`): https://docs.spring.io/spring-boot/reference/io/rest-client.html
- Spring Framework reference, testing client applications (`MockRestServiceServer`): https://docs.spring.io/spring-framework/reference/testing/spring-mvc-test-client.html
- Marc Brooker, "Timeouts, retries, and backoff with jitter", Amazon Builders' Library: https://aws.amazon.com/builders-library/timeouts-retries-and-backoff-with-jitter/
- Resilience4j documentation (circuit breakers, rate limiters, bulkheads): https://resilience4j.readme.io/
