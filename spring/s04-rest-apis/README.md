# S04 · REST APIs

⏱ 8 sessions + 3 optional · weeks 8–9 · 10 exercises

Most Spring Boot code at work sits behind an HTTP API, and most API bugs are web-layer bugs: a status code that lies, an error body that every endpoint formats differently, a time zone that drifts, a lost update when two people edit the same record. In this module you build the bookstore's catalog API ticket by ticket: controllers, request validation, problem-detail errors, JSON rules, filters and interceptors, paging, updates with optimistic concurrency, API versioning, OpenAPI documentation, file uploads and CORS. Along the way you learn what Spring MVC does with a request between Tomcat and your method, so that its behavior stops being magic.

**This module has no database.** The catalog, the stock and the members live in memory, in repositories that behave like the database tables you'll meet in S05 (they even start with the same rows). Restarting the application resets the data. To try the API yourself:

```bash
./gradlew :spring:s04-rest-apis:bootRun          # no Docker needed; stop it with Ctrl+C
curl -i http://localhost:8080/api/books/1         # after S04-01
```

The OpenAPI document is at `http://localhost:8080/v3/api-docs`, and the Swagger UI at `http://localhost:8080/swagger-ui.html`. Read [BOOKSTORE.md](../BOOKSTORE.md) for the API conventions that every ticket follows.

## Lesson

### 1. A request's journey through Spring MVC

A Spring Boot web application runs inside an embedded **Tomcat** server. Tomcat keeps a pool of threads (200 at most by default, `server.tomcat.threads.max`) and serves each request on one of them, from the first byte to the last. While your method runs, that thread runs it and nothing else. With `spring.threads.virtual.enabled=true`, Tomcat uses a virtual thread per request instead (J08-07); the model stays the same.

Here is what happens to `GET /api/books/1`:

```
HTTP request
 │  Tomcat takes a thread from its pool
 ▼
Servlet filters ................ RequestIdFilter (S04-05), later Spring Security (S12)
 ▼
DispatcherServlet .............. Spring MVC's single entry point, the "front controller"
 ├─ HandlerMapping ............. finds the method for GET /api/books/{id}: BookController.getBook
 ├─ HandlerInterceptor.preHandle  AdminTokenInterceptor (S04-05), for /api/admin/** only
 ├─ HandlerAdapter ............. builds each argument with an argument resolver:
 │                               @PathVariable, @RequestParam, @RequestBody (JSON → record, then @Valid),
 │                               @CurrentMember (your own resolver, S04-05)
 ├─ your method runs
 ├─ return value handler ....... a record or ResponseEntity → an HttpMessageConverter writes JSON (Jackson)
 ├─ HandlerInterceptor.postHandle  (only if nothing threw)
 ├─ if anything threw: HandlerExceptionResolvers → your @RestControllerAdvice (S04-03)
 └─ HandlerInterceptor.afterCompletion  (always)
 ▼
Servlet filters see the response on its way out
 ▼
Tomcat sends the response; the thread goes back to the pool
```

Three consequences matter every day:

- **Your controllers are shared by all threads.** Spring creates one `BookController` and every request thread calls it at the same time. A field that holds per-request data ("the current page") mixes up the requests of different users. Beans hold dependencies and configuration only (the [concurrency handbook page](../../docs/handbook/concurrency.md) explains why).
- **Most of the work happens outside your method.** Converting `"1"` to a `long`, parsing JSON, validating, writing JSON and turning exceptions into error responses are Spring's jobs. When something goes wrong before your method runs, for example `GET /api/books/abc`, Spring answers `400` without calling it.
- **Every step has a hook.** Filters, interceptors, argument resolvers, message converters and exception handlers are the places where you add behavior for many endpoints at once. Section 7 compares them.

### 2. Controllers and request mappings

A controller is a bean whose methods handle HTTP requests:

```java
@RestController                       // @Controller + @ResponseBody: return values become the response body
@RequestMapping("/api/books")         // the common path prefix of every method below
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @GetMapping                       // GET /api/books?page=0&size=20
    public PageResponse<BookResponse> getBooks(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return bookService.findBooks(page, size, BookSort.DEFAULT);
    }

    @GetMapping("/{id}")              // GET /api/books/1
    public BookResponse getBook(@PathVariable long id) { ... }
}
```

- **Mapping annotations:** `@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping` and `@DeleteMapping` are shortcuts for `@RequestMapping(method = ...)`. Spring answers `HEAD` and `OPTIONS` requests by itself.
- **`@PathVariable`** reads a part of the path. **`@RequestParam`** reads a query parameter (`?page=2`). **`@RequestHeader`** reads a header. Spring finds them by the parameter's name, which the compiler keeps because this repository compiles with `-parameters`; you can also name them: `@RequestParam("page")`.
- **Types are converted for you.** `long id` with the path `/api/books/abc` fails before your method runs, and Spring answers `400 Bad Request`.
- **A `@RequestParam` without `defaultValue` is required:** a request without it gets a `400`. With `required = false`, a missing parameter is `null`. An empty value (`?sort=`) also takes the default.
- **The return value** becomes the response body, converted to JSON by Jackson (section 6), with `200 OK`.

### 3. ResponseEntity and status codes

To choose the status code or add headers, return a `ResponseEntity`:

```java
return ResponseEntity.ok(book);                                     // 200 with a body
return ResponseEntity.created(URI.create("/api/books/" + id)).body(book);   // 201 + Location header
return ResponseEntity.noContent().build();                          // 204, no body
return ResponseEntity.of(optionalBook);                             // 200 with the body, or 404 with none
return ResponseEntity.ok().eTag("3").body(book);                    // 200 + ETag: "3"
return ResponseEntity.status(HttpStatus.ACCEPTED).body(job);        // any status
```

The bookstore's conventions ([BOOKSTORE.md](../BOOKSTORE.md)): `200` for reads and updates, `201` with a `Location` header for creates, `204` for deletes, `400` for invalid input, `404` for a missing resource, `409` for a broken business rule. The `Location` header may be a path (`/api/books/61`) or a full URL; `ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(id).toUri()` builds the full one.

`ResponseEntity.of(optional)` is fine for a first version, but its `404` has an empty body: a client can't tell "no such book" from "no such path". Section 5 fixes that.

### 4. Request bodies and Bean Validation

`@RequestBody` turns the JSON body into a Java object with Jackson. `@Valid` then checks the object's **Bean Validation** constraints (Jakarta Validation, implemented by Hibernate Validator) before your method runs:

```java
@PostMapping
public ResponseEntity<BookResponse> createBook(@Valid @RequestBody CreateBookRequest request) { ... }

public record CreateBookRequest(
        @NotNull @Pattern(regexp = "\\d{13}", message = "must be 13 digits") String isbn,
        @NotBlank @Size(max = 200) String title,
        @NotNull @Positive Long price) {
}
```

If a constraint fails, Spring throws `MethodArgumentNotValidException` and answers `400`; your method never runs. Without `@Valid`, nothing is checked.

| Constraint | Valid | Default message |
|---|---|---|
| `@NotNull` | anything but `null` | `must not be null` |
| `@NotBlank` | a string with at least one non-whitespace character | `must not be blank` |
| `@NotEmpty` | a non-empty string or collection (spaces count!) | `must not be empty` |
| `@Size(min, max)` | a string or collection of that length | `size must be between 0 and 200` |
| `@Positive`, `@PositiveOrZero` | a number above 0, or 0 and above | `must be greater than 0` |
| `@Min(1)`, `@Max(100)` | a number in range | `must be greater than or equal to 1` |
| `@Pattern(regexp)` | a string that matches the whole expression | `must match "\d{13}"` |
| `@Email`, `@Past`, `@Future` | what the name says | … |

- **Every constraint except `@NotNull`, `@NotBlank` and `@NotEmpty` accepts `null`.** A required field needs one of those three plus the constraints for its value.
- **Use wrapper types in requests:** `Long price`, not `long price`. A missing `price` then becomes `null`, which `@NotNull` reports by name. Jackson 3 can't put a missing value into a `long`, so it rejects the whole body with a vague "Failed to read request".
- **`message = "..."`** replaces the default message. The bookstore's messages are always English: `spring.web.locale-resolver: fixed` in `application.yml`.
- **Nested objects need `@Valid` too:** `List<@Valid OrderLine> lines`.
- **Request parameters can be validated as well:** `@RequestParam @Max(100) int size`. Since Spring Framework 6.1 this works without extra setup, and a failure throws `HandlerMethodValidationException`, which also answers `400`.
- **Your own constraint** is an annotation marked `@Constraint(validatedBy = ...)` plus a small `ConstraintValidator`. The bookstore has one: [`NullOrNotBlank`](src/main/java/com/example/bookstore/common/validation/NullOrNotBlank.java).
- **Validation checks the shape of input.** Rules that need data, such as "the ISBN is not taken yet", belong in the service ([REST API design](../../docs/handbook/rest-api-design.md#validation)).

You can test constraints without Spring or HTTP: `Validation.buildDefaultValidatorFactory().getValidator().validate(request)` returns the broken rules. `CreateBookRequestTest` does that.

### 5. Errors as problem details

RFC 9457 defines a standard JSON shape for errors, a *problem detail*: `type`, `title`, `status`, `detail`, `instance`, plus any properties you add. Spring has a class for it, `ProblemDetail`, and writes it with the media type `application/problem+json`. The bookstore adds a machine-readable `code` and, for invalid input, `errors`:

```json
{ "title": "Conflict", "status": 409, "detail": "A book with ISBN 9791115881394 already exists.",
  "instance": "/api/books", "code": "DUPLICATE_ISBN" }
```

**`@RestControllerAdvice`** marks a bean whose **`@ExceptionHandler`** methods turn exceptions from every controller into responses:

```java
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusinessException(BusinessException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setProperty("code", "DUPLICATE_ISBN");          // extra properties appear at the top level
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }
}
```

- Spring fills in `title` (the status's reason phrase, such as "Conflict") and `instance` (the request path) when it writes the response. Spring Framework 7 leaves out `type` when it's `about:blank`, which RFC 9457 defines as "the status says it all".
- For each exception, Spring picks the `@ExceptionHandler` with the closest matching exception type. A handler for `Exception.class` catches everything else: those are bugs. Log them with the stack trace, and tell the client only that something went wrong. An exception message can contain SQL, internal addresses or personal data.
- **`ResponseEntityExceptionHandler`** is a base class that already handles Spring MVC's own exceptions (malformed JSON, a missing parameter, an unknown path, a wrong method, validation failures) and turns them into problem details. Override its methods to change one case, such as `handleMethodArgumentNotValid` for validation errors. Every response it builds passes through `createResponseEntity`, the place to add something to all of them.
- If an `@ExceptionHandler` method itself throws, Spring logs a warning and falls back to its defaults: the right status for its own exceptions, but an empty body. An exception that nothing handles leaves Spring MVC; the servlet container then calls Spring Boot's fallback `/error` endpoint, which answers `500` in its own JSON format, not as a problem detail.
- **Exceptions from filters never reach `@RestControllerAdvice`**: filters run outside Spring MVC (section 7).

The bookstore's [`ErrorCode`](src/main/java/com/example/bookstore/common/error/ErrorCode.java) lists every code with its status. Services throw a [`BusinessException`](src/main/java/com/example/bookstore/common/error/BusinessException.java) with one, and the handler maps it. A code is part of the API: clients compare `code` instead of parsing English, so never rename one.

### 6. JSON with Jackson 3

Jackson converts between JSON and Java objects; Spring MVC calls it through `JacksonJsonHttpMessageConverter`. Spring Boot 4 uses **Jackson 3**, which differs from the Jackson 2 in older tutorials:

- **New packages:** `tools.jackson.databind.json.JsonMapper` instead of `com.fasterxml.jackson.databind.ObjectMapper`. The annotations stay where they were: `com.fasterxml.jackson.annotation.JsonIgnore`.
- **`JsonMapper` is immutable.** You configure it once with a builder; Spring Boot builds the application's mapper and makes it a bean.
- **Exceptions are unchecked:** `JacksonException` extends `RuntimeException`.
- **`java.time` and `Optional` work out of the box**, without registering modules.
- **New defaults.** What Spring Boot 4.1 with Jackson 3 does when you configure nothing:

| Situation | Default | Fine? |
|---|---|---|
| `Instant` | `"2026-10-15T00:00:00Z"`, always UTC | yes |
| `LocalDate` | `"2018-10-18"` | yes |
| `ZonedDateTime`, `OffsetDateTime` | keep their offset: `"2026-10-15T14:00:00+09:00"` | not for a UTC API |
| `LocalDateTime` | no zone at all: `"2026-10-15T14:00:00"` | avoid in APIs: which zone? |
| enum | its name: `"IN_STOCK"`; requests may also send its position as a number | names yes, numbers risky |
| `null` value | written: `"orderBy": null` | your API's choice |
| unknown property in a request | ignored | yes: old clients keep working |
| `null` or missing value for a primitive (`long`) | error | yes, but use wrapper types (section 4) |
| `32000.5` for a `long` | cut to `32000`, silently | not for money |
| `"32000"` (a string) for a `long` | converted to 32000 | lenient |
| extra content after the JSON value | error | yes |

Configure the application's mapper **the Spring Boot way**, in `application.yml`, so that a rule holds for every endpoint. Spring Boot builds the `JsonMapper` from these properties:

```yaml
spring:
  jackson:
    serialization:
      indent-output: true                    # pretty-printed JSON: handy on your computer, wasteful in production
    datatype:
      enum:
        fail-on-numbers-for-enums: true      # a request that sends 2 for an enum gets a 400
```

- **Features by name.** Jackson's on/off switches are enums, such as `SerializationFeature.INDENT_OUTPUT`. Spring Boot maps each enum to a group of properties, and the feature's name, in lower case with hyphens, to a key: `spring.jackson.serialization.*`, `spring.jackson.deserialization.*`, `spring.jackson.mapper.*`, and for Jackson 3's new feature sets `spring.jackson.datatype.datetime.*` and `spring.jackson.datatype.enum.*`. Your IDE completes the names in `application.yml`.
- **Settings that aren't on/off switches** have their own properties directly under `spring.jackson`, for example the time zone, the locale, the naming strategy, and which properties to write (`default-property-inclusion`): `always`, `non_null`, `non_absent` (also leaves out an empty `Optional`), `non_empty` (also empty strings and collections) or `non_default` (also values such as `0` and `false`).
- **When a property isn't enough**, a `JsonMapperBuilderCustomizer` bean changes the builder in code, and `@JacksonComponent` registers a custom serializer.
- **Per class or field**, annotations decide: `@JsonIgnore` (leave it out), `@JsonInclude(Include.NON_NULL)`, `@JsonProperty("name")` and `@JsonFormat`.
- **`spring.jackson.use-jackson2-defaults: true`** brings back Spring Boot 3's defaults, which helps when you migrate an older application.

Prefer `Instant` for points in time in your records: it can't carry a wrong zone. A global time-zone setting is the safety net for code that uses zoned types anyway. And remember that `"price": null` and a missing `price` look the same in a record; section 9 comes back to that.

### 7. Filters, interceptors and argument resolvers

Spring offers several places to add behavior to many requests at once. They run at different moments and see different things:

| | Servlet filter | Handler interceptor | Argument resolver | `@RestControllerAdvice` | AOP aspect (S03) |
|---|---|---|---|---|---|
| Belongs to | the Servlet API | Spring MVC | Spring MVC | Spring MVC | Spring, any bean |
| Runs | for every request, before Spring MVC | after Spring MVC chose the handler, for the path patterns you register | for each controller parameter it supports | when a handler or interceptor throws | around calls to bean methods |
| Sees | the raw request and response | request, response and the chosen handler | the parameter and the request | the exception | the method call |
| Can stop the request | yes: don't call `chain.doFilter` | yes: return `false` or throw | yes: throw | — | yes: throw |
| Its exceptions reach `@RestControllerAdvice` | **no** | yes | yes | — | yes, from controllers |
| Typical uses | request ids, logging, compression, security (S12) | checks for a group of URLs, timing | the current user, a tenant, a paging object | error responses | transactions, metrics |
| Register with | a `FilterRegistrationBean` or `@Component` | `WebMvcConfigurer.addInterceptors` | `WebMvcConfigurer.addArgumentResolvers` | the annotation | an `@Aspect` bean |

```java
public class RequestIdFilter extends OncePerRequestFilter {           // a filter that runs once per request
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader("X-Request-Id", ...);
        chain.doFilter(request, response);                               // the rest: other filters, Spring MVC
    }
}

public class AdminTokenInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        ...                                                              // throw, or return true to continue
    }
}

@Configuration
public class WebConfiguration implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminTokenInterceptor).addPathPatterns("/api/admin/**");
    }
}
```

- A filter that is a `@Component` runs for every URL. A `FilterRegistrationBean` also sets its position in the chain (`setOrder`) and its URL patterns.
- An interceptor also runs for paths that end in a 404: Spring MVC hands unknown paths to its static-resource handler, which is a handler too.
- `postHandle` runs after a REST method returned, but by then the JSON body is usually written and the response *committed* (on its way to the client): changing headers there has no effect. Set response headers before the body is written, as the filter above does.
- An argument resolver lets controllers declare what they need, such as `@CurrentMember Member member`, instead of parsing headers in every method. Spring Security's `@AuthenticationPrincipal` (S12) is one.

### 8. Paging and sorting

A list endpoint must never return "everything": the catalog grows, and one request for a million rows can take the service down. The bookstore's convention:

```
GET /api/books?page=0&size=20&sort=price,desc
```

- **`page`** counts from 0, **`size`** has a default (20) and a **maximum** (100). Out-of-range values are invalid input, `400`.
- **`sort`** is a property and an optional direction. Allow only an **allow list** of properties. Sorting by any field a client names would expose internal fields, sort by columns without a database index (slow), and in S05, where the sort goes into SQL, invite injection.
- **End every sort with a unique tie-breaker**, usually the id. Without it, books with equal prices come in any order, and between two page requests a book can move across the page boundary: it appears on two pages, or on none.
- **The page envelope** tells the client where it is: `{"content": [...], "page": {"size": 20, "number": 0, "totalElements": 60, "totalPages": 3}}`. A page after the last one is empty, with the totals.

From S06 on, Spring Data does this for you: a `Pageable` parameter reads `page`, `size` and `sort`, and `PagedModel` produces the same JSON. You build it by hand here, so you know what it does. Offset paging gets slow for deep pages (the database still reads the skipped rows); feeds and infinite scrolling use *keyset* paging instead: "the next 20 after id 1040".

### 9. PUT, PATCH, DELETE, idempotency and ETags

| Method | Meaning | Safe | Idempotent |
|---|---|---|---|
| `GET` | read | yes | yes |
| `POST` | create, or start an action | no | no |
| `PUT` | replace the whole resource | no | yes |
| `PATCH` | change some fields | no | not necessarily |
| `DELETE` | remove | no | yes |

*Idempotent* means that sending the request twice has the same effect as sending it once. That matters because clients retry when a response doesn't arrive.

- **`PUT` replaces.** The body is the complete new state, so every field is required. A missing field is an error, not "keep the old value".
- **`PATCH` changes some fields.** With JSON Merge Patch (RFC 7396), a field that is missing stays as it is, and a field set to `null` is removed. A Java record can't tell "missing" from `null`, so the bookstore, whose book fields are all required, treats both as "keep". Validate only the fields that are present: `@NotBlank` would reject a missing title, although a `PATCH` without a title is fine. That's what [`NullOrNotBlank`](src/main/java/com/example/bookstore/common/validation/NullOrNotBlank.java) is for.
- **`DELETE` is idempotent**, but the status code of a repeat is your choice: `404` ("it isn't there") or `204` ("it's gone either way"). The bookstore answers `204`, so that a client can retry a `DELETE` whose response it never received without seeing an error.

**The lost update.** Two admins open book 7. Anna changes its price and saves. Ben, who still sees the old page, fixes a typo in the title and saves: his `PUT` contains the old price, and Anna's change is gone. Nobody gets an error.

**ETags and conditional requests** prevent that. An *ETag* (entity tag) is a version label of a representation, sent in a response header: `ETag: "3"`. The bookstore uses the book's version number, which increases with every change.

```
GET /api/books/7                     → 200, ETag: "3"
PUT /api/books/7, If-Match: "3"      → 200, ETag: "4"        Anna's change
PUT /api/books/7, If-Match: "3"      → 412 Precondition Failed   Ben read version 3; it's 4 now
```

- **`If-Match`** makes an update conditional: "only if the book still has this ETag". A mismatch answers `412 Precondition Failed`; Ben reloads the book and repeats his change on the new version. This is *optimistic concurrency*: nobody locks anything, and conflicts are detected instead of prevented. S08 does the same in the database with JPA's `@Version`.
- **`428 Precondition Required`** (RFC 6585) answers an update without `If-Match`, so that a client can't skip the check by accident.
- **Strong and weak ETags.** `"3"` is strong: the representation is exactly this one. `W/"3"` is weak: "equivalent". `If-Match` compares strongly, so a weak ETag never matches. `org.springframework.http.ETag.parse(header)` parses a header into ETags.
- **`If-None-Match`** works the other way, for caching: a client that still has version 3 sends `If-None-Match: "3"` and gets `304 Not Modified` without a body. For a `GET` that returns a `ResponseEntity` with an ETag, Spring does this for you.

### 10. API versioning in Spring Framework 7

An API is a contract with clients you don't control: an app version from last year still calls it. **Additive changes** (a new optional field in a response, a new endpoint) need no new version, because clients ignore what they don't know. **Breaking changes** do: removing or renaming a field, changing a field's type, changing what a status code means.

A client can send the version in a request header (`API-Version: 2`), the path (`/api/v2/books`), a query parameter (`?version=2`) or the media type (`Accept: application/json;version=2`). Spring Framework 7 supports all four. Spring Boot configures it with properties:

```yaml
spring:
  mvc:
    apiversion:
      use:
        header: API-Version      # or: path-segment: 1, query-parameter: version, media-type-parameter: ...
      default: 1                 # the version of a request that doesn't name one
```

```java
@GetMapping("/{id}")                        // no version: matches every version
public BookResponse getBook(@PathVariable long id) { ... }

@GetMapping(path = "/{id}", version = "2")  // only for version 2
public BookV2Response getBookV2(@PathVariable long id) { ... }
```

- A mapping without `version` matches every version; for a request with `API-Version: 2`, the mapping for version 2 wins. `version = "1.2+"` means "1.2 and later".
- Versions are compared as numbers: `2`, `2.0` and `v2` are the same version.
- The supported versions are the ones your mappings declare, plus the default; `spring.mvc.apiversion.supported` adds more. A request for another version gets `400` (`InvalidApiVersionException`). With `required: true`, a request without a version gets `400` too.
- `WebMvcConfigurer.configureApiVersioning(ApiVersionConfigurer)` does the same in code, and can also mark versions as deprecated with response headers.
- **A `version` attribute without configured versioning stops the application from starting:** "API version specified, but no ApiVersionStrategy configured".

Header versioning keeps URLs stable; path versioning is easier to see in logs and to try in a browser. Teams pick one and keep it. Whatever you pick, supporting a version costs work for as long as clients use it, so change versions rarely.

### 11. OpenAPI documentation with springdoc-openapi

OpenAPI is a standard, machine-readable description of an HTTP API: paths, parameters, bodies, responses. Front-end teams read it, and tools generate clients from it (for example TypeScript clients with `openapi-generator`). **springdoc-openapi** generates it from your Spring code at runtime. Use version 3.x with Spring Boot 4 (2.x is for Spring Boot 3); it isn't in Spring Boot's BOM, so `build.gradle.kts` names the version:

```kotlin
implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1")
```

- The document is at `/v3/api-docs` (JSON) and `/v3/api-docs.yaml`; the Swagger UI, a web page to read it and try requests, is at `/swagger-ui.html`.
- **springdoc infers a lot:** paths, parameters with their defaults, request and response schemas from your records, and Bean Validation rules (`@NotBlank` → `required`, `@Size(max = 200)` → `maxLength`, `@Pattern` → `pattern`).
- **It can't infer** a status code that a `ResponseEntity` chooses at runtime (it documents `200`), the error responses, descriptions, or that `@CurrentMember` isn't a parameter that clients send.
- **Annotations fill the gaps** (package `io.swagger.v3.oas.annotations`): `@Tag(name = "Books")` on a controller, `@Operation(summary = "Get a book")` on a method, `@ApiResponse(responseCode = "404", description = ..., content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))`, `@Parameter(hidden = true)`, `@Hidden`.
- **An `OpenAPI` bean** sets the document's title, version and description.
- **Properties** set the rest: `springdoc.paths-to-exclude`, `springdoc.default-produces-media-type`, and `springdoc.api-docs.enabled=false` where the document must not be public.

Generating the document from code keeps it in sync with the code. The alternative, *contract first*, writes the OpenAPI file first and generates the controllers' interfaces from it; it suits APIs that several teams design together.

### 12. File uploads and CORS

**Uploads.** A browser form or a client sends files as `multipart/form-data`: a body with several named parts. Spring hands each file part to you as a `MultipartFile`:

```java
@PutMapping(path = "/{id}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<Void> uploadCover(@PathVariable long id, @RequestParam("file") MultipartFile file) { ... }
```

- `consumes` limits the endpoint to multipart requests; others get `415 Unsupported Media Type`. A request without the part gets `400`.
- `MultipartFile` offers `getSize()`, `getBytes()`, `getInputStream()`, `isEmpty()`, `getOriginalFilename()` and `getContentType()`.
- **Never trust what the client says about the file.** The content type and the file name are chosen by the client: anyone can send a script named `cover.png` with `Content-Type: image/png`. Check the file's first bytes (its *magic number*), and never use the file name in a path on disk.
- **Limits:** Tomcat rejects a file larger than `spring.servlet.multipart.max-file-size` (1MB by default) or a request larger than `max-request-size` (10MB) before your code runs, with `413 Content Too Large`. Check your own, stricter limit in code too: it gives a clear error, and MockMvc tests don't go through Tomcat's multipart parsing, so they never hit the server's limit.
- Real applications store files in object storage, such as Amazon S3, and keep only the address in the database.

**CORS.** Browsers follow the *same-origin policy*: JavaScript on `https://shop.example.com` may only read responses from that same *origin* (scheme, host and port). To call the API at another origin, the API must allow it with **CORS** (cross-origin resource sharing) response headers:

- The browser sends an `Origin` header with the request. If the response's `Access-Control-Allow-Origin` names that origin, the page's JavaScript gets the response.
- Before a request that isn't "simple" (a `PUT`, `PATCH` or `DELETE`, a JSON body, custom headers such as `If-Match`), the browser asks first with a **preflight** request: `OPTIONS` with `Access-Control-Request-Method` and `Access-Control-Request-Headers`. The answer lists what is allowed, and the browser caches it for `Access-Control-Max-Age` seconds.
- JavaScript can read only a few basic response headers unless the server lists more in `Access-Control-Expose-Headers`, such as `ETag` and `Location`.
- Spring MVC configures it in `WebMvcConfigurer.addCorsMappings` with a `CorsRegistry`, or per controller with `@CrossOrigin`. With Spring Security (S12), configure CORS in the security chain, which runs first.
- **CORS protects your users' browsers, not your API.** It stops a malicious page from reading your API's responses with your users' cookies. A script with `curl` ignores it completely; authentication (S12) protects the API.
- A preflight request carries no credentials or custom headers. An interceptor that demands a token would reject it, and the browser would block the real request. Let `OPTIONS` preflight requests through such checks.

### 13. Testing the web layer

`MockMvcTester` sends requests through the whole Spring MVC stack (filters, interceptors, argument resolvers, validation, exception handlers, JSON) without a network, and checks the responses with AssertJ:

```java
@IntegrationTest
class BookCatalogApiTest {

    @Autowired
    private MockMvcTester mvc;

    @Test
    @DisplayName("GET /api/books/1 answers 200 with the book as JSON")
    void returnsABook() {
        assertThat(mvc.get().uri("/api/books/1"))
                .hasStatusOk()
                .bodyJson().isStrictlyEqualTo("""
                        { "id": 1, "title": "Effective Java", ... }""");
    }
}
```

- `bodyJson().isStrictlyEqualTo(json)` compares JSON structurally: property order doesn't matter, but every property must match and no extra one may exist. `isLenientlyEqualTo` allows extra properties. `extractingPath("$.page.totalElements")` and `hasPathSatisfying(...)` check one value with a JSONPath expression.
- **Full context or slice?** `@SpringBootTest` with `@AutoConfigureMockMvc` starts the whole application. `@WebMvcTest(BookController.class)` starts only the web layer, and you replace the services with mocks (`@MockitoBean`). A slice is faster and isolates the controller, but its mocks can't catch a wrong assumption about the service. This module has no database, so the full context starts in about a second, and the tests use it: `@IntegrationTest` (in `src/test/.../support`) adds a fixed clock and puts the in-memory data back after every test. S13 practices slices.
- Not everything needs HTTP: a validation rule is a unit test with a `Validator`, a filter is a unit test with `MockHttpServletRequest` and a lambda `FilterChain`.

## TypeScript / NestJS → Spring

| Express / NestJS | Spring MVC | Watch out for |
|---|---|---|
| `@Controller('api/books')` + `@Get(':id')`, or `app.get('/api/books/:id', …)` | `@RestController` + `@RequestMapping("/api/books")` + `@GetMapping("/{id}")` | Path variables are written `{id}`, not `:id`. |
| `@Param('id')`, `req.params.id` | `@PathVariable long id` | Spring converts the type; `abc` gets a 400 before your code runs. |
| `@Query('page')`, `req.query.page` | `@RequestParam(defaultValue = "0") int page` | A parameter without a default is required. |
| `@Body() dto: CreateBookDto` | `@Valid @RequestBody CreateBookRequest request` | Without `@Valid`, nothing is validated. |
| `class-validator` decorators + `ValidationPipe` | Bean Validation annotations + `@Valid` | Most constraints accept `null`: add `@NotNull`. |
| `res.status(201).location(url).json(book)` | `ResponseEntity.created(uri).body(book)` | Returning the record alone means `200 OK`. |
| Exception filters (`@Catch`), `HttpException` | `@RestControllerAdvice` + `@ExceptionHandler`, `ProblemDetail` | Spring MVC's own errors go through `ResponseEntityExceptionHandler`. |
| Middleware | a servlet `Filter` (`OncePerRequestFilter`) | It runs before Spring MVC knows the controller; its exceptions skip the advice. |
| Guards | a `HandlerInterceptor`'s `preHandle`, or Spring Security (S12) | Return `false` or throw to stop the request. |
| Interceptors (wrap the handler, map the result) | `HandlerInterceptor`, or an AOP `@Around` aspect (S03) | `postHandle` comes after the JSON body is written. |
| `createParamDecorator` (`@User() user`) | `HandlerMethodArgumentResolver` + your annotation | Register it in a `WebMvcConfigurer`. |
| `class-transformer`, `JSON.stringify` | Jackson 3, configured with `spring.jackson.*` | No `undefined` in Java: a missing property and `null` look the same in a record. |
| `@nestjs/swagger` | springdoc-openapi | Not in Spring Boot's BOM: you choose the version. |
| `multer` | `MultipartFile` | Size limits are `spring.servlet.multipart.*`. |
| `app.enableCors({ origin })` | `CorsRegistry` in a `WebMvcConfigurer` | With Spring Security, configure it in the security chain. |
| Node's event loop: one thread serves every request | Tomcat: a thread per request | Beans are shared by all threads: keep them stateless. |

## In real projects

- **Thin controllers.** A controller reads the request, calls one service method and builds the response. Rules live in services and entities. See [Spring layering](../../docs/handbook/spring-layering.md).
- **Records for requests and responses, never entities.** A request record accepts exactly the fields a client may set; a response record shows exactly what clients may see. See [REST API design](../../docs/handbook/rest-api-design.md).
- **One error format, produced in one place.** A `@RestControllerAdvice` turns every exception into the same problem-detail shape with a stable `code`. Log unexpected exceptions once, with the stack trace, and never send their messages to clients. See [Logging and observability](../../docs/handbook/logging-and-observability.md).
- **Validate the shape at the edge, the rules in the service.** Bean Validation for formats and ranges, `BusinessException` for rules that need data.
- **Page every list that can grow**, cap the size, allow-list the sort properties, and end every sort with the id.
- **Protect editable resources against lost updates** with ETags and `If-Match`, or with a version field in the body. S08 adds the database side.
- **Configure JSON once**, in `application.yml`, and use `Instant` for points in time. See [Configuration](../../docs/handbook/configuration.md).
- **Give every request an id** and return it in a header, so that support can find a customer's request in the logs (S14 adds it to every log line).
- **A header like `X-Member-Id` isn't authentication**, CORS isn't access control, and secret tokens are compared in constant time. See [Security essentials](../../docs/handbook/security.md).
- **Version only for breaking changes**, and document the API with an OpenAPI document generated from the code.
- **Test the API through MockMvc**, and unit-test the rules underneath it. See [Testing Spring applications](../../docs/handbook/spring-testing.md).
- **Constructor injection with `@RequiredArgsConstructor`, records for DTOs.** See [Lombok](../../docs/handbook/lombok.md).

## Exercises

All exercises work on the same application, and each builds on the ones before it: do them in order. The tests of an exercise check its ticket's acceptance criteria. `./score s04` shows your progress, and `./score s04-03` lists every failing test of one exercise.

Each method you implement throws `UnsupportedOperationException("TODO: S04-xx")`. A test that reaches such a method fails with a `500`, or with "Request failed unexpectedly"; the log shows which TODO it hit. Methods that Spring calls while the application starts, such as those of a `WebMvcConfigurer`, are empty instead: a stub that threw there would stop the whole application, and every test with it. Until you finish S04-03, the log also shows warnings with `TODO: S04-03` whenever a request fails: that is the unfinished error handler, and Spring falls back to answering with the right status but an empty body.

### S04-01 · Your first REST controller

**Build** · 30 min · [`BookController.java`](src/main/java/com/example/bookstore/book/BookController.java)

> **Ticket: the catalog's read API.** The shop's front end needs to show books. Implement `GET /api/books` and `GET /api/books/{id}`. `BookService` already does the work; your controller translates between HTTP and the service.

**Before you start:** lesson sections 1–3. Read [`BookService`](src/main/java/com/example/bookstore/book/BookService.java) (`findBooks`, `findBook`), [`BookResponse`](src/main/java/com/example/bookstore/book/BookResponse.java) and [`PageResponse`](src/main/java/com/example/bookstore/common/web/PageResponse.java).

**You'll practice:** `@RestController`, `@RequestMapping`, `@GetMapping`, `@PathVariable`, `@RequestParam` with defaults, `ResponseEntity`.

**Acceptance criteria**

- `GET /api/books/1` answers `200` with the book as JSON, exactly as in [BOOKSTORE.md](../BOOKSTORE.md).
- `GET /api/books` answers `200` with the first page of 20 books in id order, in the page envelope. `?page=2&size=7` answers books 15 to 21. A page after the last one is empty, with the totals.
- `GET /api/books/9999` answers `404`. Its body is empty for now; S04-03 adds a problem detail.
- `GET /api/books/abc` and `GET /api/books?page=first` answer `400`. Spring does that for you once the parameters are typed.

Implement `getBooks` and `getBook`. The other methods of `BookController` belong to later exercises; leave them as they are.

<details>
<summary>Hint 1</summary>

The class needs two annotations of its own: one that makes it a REST controller, and one for the common path `/api/books`. Each of the two methods needs a mapping annotation, and each parameter an annotation that says where its value comes from.
</details>

<details>
<summary>Hint 2</summary>

`BookService.findBook` returns an `Optional<BookResponse>`. Which static method of `ResponseEntity` turns an `Optional` into `200` or `404`? For the list, pass `BookSort.DEFAULT` to `findBooks`.
</details>

**Try it:** `./gradlew :spring:s04-rest-apis:bootRun`, then `curl -i http://localhost:8080/api/books/1` and `curl -i http://localhost:8080/api/books/abc`.

**Done?** Compare with the reference on the `solutions` branch (it shows the controller after all ten exercises), then ask yourself: Which component answered `GET /api/books/abc` with `400`, and when did it decide? What would go wrong if `BookController` stored the requested page in a field?

### S04-02 · Request bodies and validation

**Build** · 30 min · [`CreateBookRequest.java`](src/main/java/com/example/bookstore/book/CreateBookRequest.java) · [`BookController.java`](src/main/java/com/example/bookstore/book/BookController.java)

> **Ticket: add books through the API.** The store's staff wants to add books with `POST /api/books`. Invalid input must never reach `BookService`.

**Before you start:** lesson section 4.

**You'll practice:** `@RequestBody` and `@Valid`, Bean Validation constraints on a record, custom messages, `201 Created` with a `Location` header.

**Acceptance criteria**

- A valid book answers `201 Created` with the new book as JSON (its category's name, not its id) and its address in the `Location` header, `/api/books/{id}`. The book can then be read at that address, and the catalog has 61 books.

  ```json
  { "isbn": "9788966261208", "title": "Effective Kotlin", "author": "Marcin Moskala",
    "categoryId": 1, "price": 32000, "publishedDate": "2022-03-07" }
  ```

- Every rule in the Javadoc of `CreateBookRequest` holds, with exactly the message it names. `CreateBookRequestTest` checks each rule directly with a `Validator`.
- An invalid book, a missing body or malformed JSON answers `400`, and nothing is stored. A body that isn't JSON (`Content-Type: text/plain`) answers `415`. These responses have no body yet: S04-03.
- Korean text arrives intact, and spaces around the title are removed (`Book` does that).

<details>
<summary>Hint 1</summary>

Which constraint rejects `"   "` as a title: `@NotNull`, `@NotEmpty` or `@NotBlank`? And which one does a required `Long` need? The ISBN rule needs a custom message: every constraint has a `message` attribute.
</details>

<details>
<summary>Hint 2</summary>

`@Pattern` must match the whole value, so `"\\d{13}"` needs no `^` and `$`. For the response: `ResponseEntity.created(URI.create(...)).body(...)`.
</details>

**Done?** Compare with the reference, then ask yourself: Why does the ISBN check for "already taken" live in `BookService` and not in a constraint? What would a client see if the record used `long price` and the body had no price?

### S04-03 · Error responses with ProblemDetail

**Build** · 30 min · [`GlobalExceptionHandler.java`](src/main/java/com/example/bookstore/common/error/GlobalExceptionHandler.java) · [`BookController.java`](src/main/java/com/example/bookstore/book/BookController.java)

> **Ticket: clients can't handle our errors.** The mobile team reports that a `400` has no body, so they can't tell the user which field is wrong. A second book with the same ISBN crashes the request instead of answering `409`. And a missing book is an empty `404`, which looks the same as a wrong URL. Make every error response the problem detail of [BOOKSTORE.md](../BOOKSTORE.md).

**Before you start:** lesson section 5. Read the parts of `GlobalExceptionHandler` that are already there: `handleHandlerMethodValidationException` and the helper methods at the end.

**You'll practice:** `@RestControllerAdvice`, `@ExceptionHandler`, `ProblemDetail` with custom properties, the hooks of `ResponseEntityExceptionHandler`, logging an unexpected exception.

**Acceptance criteria** (every response has the media type `application/problem+json`)

| Request | Status | `code` | `detail` |
|---|---|---|---|
| `GET /api/books/9999` | 404 | `BOOK_NOT_FOUND` | `Book 9999 was not found.` |
| `POST /api/books` with the ISBN of book 1 | 409 | `DUPLICATE_ISBN` | `A book with ISBN 9791115881394 already exists.` |
| `POST /api/books` with `"categoryId": 99` | 404 | `CATEGORY_NOT_FOUND` | `Category 99 was not found.` |
| `POST /api/books` with invalid fields | 400 | `INVALID_INPUT` | `Invalid request content.`, plus `errors` |
| malformed JSON | 400 | `INVALID_INPUT` | `Failed to read request` |
| `GET /api/books/abc` | 400 | `INVALID_INPUT` | `Failed to convert 'id' with value: 'abc'` |
| `GET /api/bookz` | 404 | `NOT_FOUND` | `No endpoint GET /api/bookz.` |
| `DELETE /api/books` | 405 | `METHOD_NOT_ALLOWED` | `Method 'DELETE' is not supported.` |
| a body that isn't JSON | 415 | `UNSUPPORTED_MEDIA_TYPE` | Spring's |
| any unexpected exception | 500 | `INTERNAL_ERROR` | `An unexpected error occurred.` |

- `errors` lists every broken rule as `{ "field": ..., "message": ... }`, sorted by field, then by message, so a title of 201 spaces appears twice.
- An unexpected exception is logged at `ERROR` level with the request (`GET /api/books`) and its stack trace. Its message never appears in the response.
- `GET /api/books/{id}` reports a missing book with a `BusinessException` now, instead of the empty `404` of S04-01.

<details>
<summary>Hint 1</summary>

`ProblemDetail.forStatusAndDetail(status, detail)` creates the problem detail, and `setProperty("code", ...)` adds the code. Return it in a `ResponseEntity` with the same status. A small private method can build it for both `@ExceptionHandler` methods.
</details>

<details>
<summary>Hint 2</summary>

`handleMethodArgumentNotValid` is the twin of the finished `handleHandlerMethodValidationException`: the exception's `getBindingResult()` has the field errors and the errors of the whole object, and the helpers `fieldViolation`, `objectViolation` and `withErrors` do the rest.
</details>

<details>
<summary>Hint 3</summary>

`createResponseEntity` receives the body that the base class built. If it is a `ProblemDetail` without a `code`, add the generic code for the status (`genericCodeFor`), then call `super.createResponseEntity(...)`. For the controller: `bookService.findBook(id).orElseThrow(() -> new BusinessException(ErrorCode.BOOK_NOT_FOUND, ...))`.
</details>

**Done?** Compare with the reference, then ask yourself: Why does the `500` response hide the exception's message, and where can you still find it? Why is the duplicate ISBN a `BusinessException` and not a validation error?

### S04-04 · JSON with Jackson

**Build** · 30 min · [`application.yml`](src/main/resources/application.yml)

> **Ticket: the partner's parser rejects our JSON.** A price-comparison site reads `GET /api/books/{id}/availability` and adds books through `POST /api/books`. Their parser follows [BOOKSTORE.md](../BOOKSTORE.md) strictly. Configure Jackson, the Spring Boot way, so that every endpoint follows the rules below. The availability endpoint already exists: read [`AvailabilityService`](src/main/java/com/example/bookstore/inventory/AvailabilityService.java) and [`AvailabilityResponse`](src/main/java/com/example/bookstore/inventory/AvailabilityResponse.java). The warehouse works in Korean time, so `orderBy` is a `ZonedDateTime` in `Asia/Seoul`.

**Before you start:** lesson section 6.

**You'll practice:** Jackson 3's defaults, `spring.jackson.*` properties, null inclusion, time zones, strict number handling.

**Acceptance criteria**

- Times are ISO-8601 strings in UTC, even when the code computed them in Seoul time. At 09:00 in Seoul (the tests' clock, `2026-10-15T00:00:00Z`), book 1 answers:

  ```json
  { "bookId": 1, "status": "IN_STOCK", "quantity": 10, "orderBy": "2026-10-15T05:00:00Z",
    "estimatedDelivery": "2026-10-16", "checkedAt": "2026-10-15T00:00:00Z" }
  ```

- Dates are ISO dates (`"2026-10-16"`), and enums are written by name (`"LOW_STOCK"`).
- Properties whose value is `null` are left out: book 59, which is out of stock, has no `orderBy` and no `estimatedDelivery`.
- A price with a fraction, `32000.5` or even `32000.0`, is rejected with `400` instead of being cut to whole won.
- Properties that a request doesn't know are ignored, so `"subtitle"` in a new book doesn't stop it.
- The rules hold for every response: a test serializes its own record with the application's `JsonMapper`.

Don't change `AvailabilityService` or `AvailabilityResponse`: the rules belong to the whole API.

<details>
<summary>Hint 1</summary>

Look at the table in lesson section 6: which rows aren't fine yet? Then look for the matching properties under `spring.jackson` (your IDE completes them in `application.yml`), or in the [Spring Boot reference](https://docs.spring.io/spring-boot/appendix/application-properties/index.html#appendix.application-properties.json).
</details>

<details>
<summary>Hint 2</summary>

Jackson converts zoned times to *its* time zone only when you set one explicitly. The rule that cuts `32000.5` is a `deserialization` feature whose name says "accept float as int".
</details>

**Done?** Compare with the reference, then ask yourself: What would `non_empty` or `non_default` leave out that `non_null` keeps, such as `"quantity": 0`? The ticket could also have been solved by changing `orderBy` to an `Instant`: why is the configuration still worth having?

### S04-05 · Filters, interceptors and argument resolvers

**Build** · 30 min · [`RequestIdFilter.java`](src/main/java/com/example/bookstore/common/web/RequestIdFilter.java) · [`AdminTokenInterceptor.java`](src/main/java/com/example/bookstore/common/web/AdminTokenInterceptor.java) · [`CurrentMemberArgumentResolver.java`](src/main/java/com/example/bookstore/member/CurrentMemberArgumentResolver.java) · [`WebConfiguration.java`](src/main/java/com/example/bookstore/common/web/WebConfiguration.java)

> **Ticket: three things every request needs.** (1) Support asks customers for a request id, so every response must carry one in `X-Request-Id`. (2) The admin API under `/api/admin/` must check the admin token in `X-Admin-Token`. (3) `GET /api/members/me` and future endpoints need the current member, named by the `X-Member-Id` header, without parsing the header themselves. (Real authentication arrives in S12; until then the gateway in front of us sets `X-Member-Id`.)

**Before you start:** lesson section 7. Read [`MemberController`](src/main/java/com/example/bookstore/member/MemberController.java), [`BookAdminController`](src/main/java/com/example/bookstore/book/BookAdminController.java) and [`AdminProperties`](src/main/java/com/example/bookstore/common/web/AdminProperties.java).

**You'll practice:** `OncePerRequestFilter` and `FilterRegistrationBean`, `HandlerInterceptor` with path patterns, `HandlerMethodArgumentResolver`, `WebMvcConfigurer`, constant-time comparison.

**Acceptance criteria** (the Javadoc of each class has the details)

- **Filter:** every response has an `X-Request-Id` header: the request's own id if it is valid, otherwise a new random UUID. An invalid id (too long, spaces, line breaks, other characters) is never echoed. The header is set before the rest of the chain runs. The filter runs at the front of the filter chain, also for paths that end in `404` and for requests the interceptor rejects.
- **Interceptor:** `GET /api/admin/books/stats` with the right token answers `{ "books": 60, "categories": 6, "outOfStock": 2 }`. Without the token, or with any other value (another case, a prefix, an extra character), it answers `401` with code `UNAUTHORIZED` and the detail `Send the admin token in the X-Admin-Token header.`, and the controller never runs. The rest of the API needs no token.
- **Argument resolver:** `GET /api/members/me` with `X-Member-Id: 1` answers `{ "id": 1, "email": "minji@example.com", "name": "Minji Kim", "role": "USER" }`. A missing header, an id that isn't 1 to 18 digits, or an unknown member answers `401` with code `UNAUTHORIZED` and the detail `Send the id of an existing member in the X-Member-Id header.`

<details>
<summary>Hint 1</summary>

The filter reads the header, checks it against a regular expression, and sets the response header before calling `chain.doFilter(request, response)`. `UUID.randomUUID()` creates an id.
</details>

<details>
<summary>Hint 2</summary>

In `WebConfiguration`: a `@Bean` method that returns a `FilterRegistrationBean<RequestIdFilter>` with the order `Ordered.HIGHEST_PRECEDENCE`; `registry.addInterceptor(...).addPathPatterns(...)`; `resolvers.add(...)`. Inject the interceptor and the resolver through the constructor: both are beans already.
</details>

<details>
<summary>Hint 3</summary>

`MessageDigest.isEqual(a, b)` compares two byte arrays in constant time; `String.getBytes(StandardCharsets.UTF_8)` gives the bytes. In the resolver, `parameter.hasParameterAnnotation(CurrentMember.class)` and `parameter.getParameterType()` decide `supportsParameter`, and `webRequest.getHeader(...)` reads the header.
</details>

**Done?** Compare with the reference, then ask yourself: Why can the interceptor's exception become a problem detail, but a filter's couldn't? Why does the resolver give the same message for "no header" and "unknown member"?

### S04-06 · Pagination and sorting

**Build** · 30 min · [`BookSort.java`](src/main/java/com/example/bookstore/book/BookSort.java) · [`BookController.java`](src/main/java/com/example/bookstore/book/BookController.java)

> **Ticket: sorting, and a limit.** The shop wants to list the catalog by price and by publication date. And last week someone requested `?size=1000000`: limit the page size to 100.

**Before you start:** lesson section 8, and the section "Lists, paging and filtering" of [REST API design](../../docs/handbook/rest-api-design.md#lists-paging-and-filtering).

**You'll practice:** validating request parameters, parsing a parameter into a value object with clear errors, allow lists, stable sort orders.

**Acceptance criteria**

- `GET /api/books` accepts `sort`: a property and an optional direction, such as `price` or `price,desc`. The rules and the exact error messages are in the Javadoc of `BookSort.parse`. Without `sort`, or with an empty one, the order is by id.
- Books with equal values come in id order, so every book is on exactly one page. (`BookSort.comparator()` already does that: read it.)
- `page` must be 0 or more (message `must be greater than or equal to 0`), and `size` 1 to 100 (`must be greater than or equal to 1`, `must be less than or equal to 100`). Other values answer `400` with code `INVALID_INPUT` and an `errors` entry for each invalid parameter:

  ```json
  { "title": "Bad Request", "status": 400, "detail": "Validation failure", "instance": "/api/books",
    "code": "INVALID_INPUT", "errors": [ { "field": "size", "message": "must be less than or equal to 100" } ] }
  ```

- An invalid `sort` answers `400` with code `INVALID_INPUT` and the message from `BookSort.parse` as `detail`.

Add the `sort` parameter to `getBooks` in `BookController`, check the parameters there, and implement `BookSort.parse`.

<details>
<summary>Hint 1</summary>

Constraints work on `@RequestParam` parameters too: `@PositiveOrZero`, `@Min(1)`, `@Max(100)`. Mind the messages: `@Positive` and `@Min(1)` say different things.
</details>

<details>
<summary>Hint 2</summary>

`"price,".split(",")` returns `["price"]`: `String.split` drops empty strings at the end. `split(",", -1)` keeps them. `Property.values()` and its `parameterName()` give you the allow list; `Collectors.joining(", ")` builds the list for the message.
</details>

**Done?** Compare with the reference, then ask yourself: Why does the API refuse `sort=isbn`, although books have an ISBN? What would happen to page 2 without the id tie-breaker if two books with the same price sat on the page boundary?

### S04-07 · Updating and deleting resources

**Build** · 60 min · [`BookController.java`](src/main/java/com/example/bookstore/book/BookController.java) · [`ReplaceBookRequest.java`](src/main/java/com/example/bookstore/book/ReplaceBookRequest.java) · [`PatchBookRequest.java`](src/main/java/com/example/bookstore/book/PatchBookRequest.java) · [`BookService.java`](src/main/java/com/example/bookstore/book/BookService.java)

> **Ticket: edit and remove books, without lost updates.** The store's staff edits books in the admin tool: sometimes the whole book (`PUT`), usually one field (`PATCH`), and sometimes it removes one (`DELETE`). Last month two staff members edited the same book at the same time, and one change silently disappeared. Every update must now say which version of the book it changes.

**Before you start:** lesson section 9. Read `BookService.replaceBook`, and how [`BookRepository.save`](src/main/java/com/example/bookstore/book/BookRepository.java) counts versions.

**You'll practice:** `PUT` vs `PATCH` semantics, validating only present fields, `ETag` and `If-Match`, `412` and `428`, idempotent `DELETE`, conditional `GET`.

**Acceptance criteria**

- `GET /api/books/{id}` sends the book's version as its ETag: `ETag: "0"` for every seeded book. A `GET` with `If-None-Match: "0"` answers `304 Not Modified` without a body.
- `PUT /api/books/{id}` with `If-Match: "0"` and a valid [`ReplaceBookRequest`](src/main/java/com/example/bookstore/book/ReplaceBookRequest.java) replaces every field but the ISBN and answers `200` with the book and its new ETag, `"1"`.
- `PATCH /api/books/{id}` with `If-Match` and a [`PatchBookRequest`](src/main/java/com/example/bookstore/book/PatchBookRequest.java) changes only the fields the body contains; the fields that are present must be valid. Implement `BookService.patchBook`.
- A change that changes nothing (an empty `PATCH`, a `PUT` with the current values) keeps the version and the ETag.
- Without `If-Match`: `428` with code `PRECONDITION_REQUIRED`. With an old version, a weak ETag (`W/"0"`) or something that isn't a version: `412` with code `PRECONDITION_FAILED`, and the book is unchanged.
- An invalid body answers `400` with the `errors` of S04-03; an unknown book or category answers `404`.
- `DELETE /api/books/{id}` answers `204` and the book is gone, from the list too. Deleting it again, or deleting a book that never existed, answers `204` as well.

<details>
<summary>Hint 1</summary>

`ResponseEntity.ok().eTag(Long.toString(version)).body(book)` sends `ETag: "3"`: Spring adds the quotes. `BookResponse.version()` has the version; `@JsonIgnore` keeps it out of the JSON.
</details>

<details>
<summary>Hint 2</summary>

Read `If-Match` with `@RequestHeader(name = HttpHeaders.IF_MATCH, required = false)`: a required header that is missing would make Spring answer `400` before your `428` check. `ETag.parse(ifMatch)` returns a list of `ETag`s with `tag()` and `weak()`. One private method can turn the header into the expected version for both `PUT` and `PATCH`.
</details>

<details>
<summary>Hint 3</summary>

In `patchBook`, start like `replaceBook`, then keep the book's current value for every field the request doesn't contain: `Objects.requireNonNullElse(request.title(), book.getTitle())`. `BookRepository.save` decides whether anything changed.
</details>

**Done?** Compare with the reference, then ask yourself: Why must a `PUT` contain every field? Why is `428` better than letting an update without `If-Match` through? What does a `PATCH` with `"title": null` do here, and what would JSON Merge Patch say it should do?

### S04-08 · API versioning ★

**Build** · 30 min · [`BookV2Response.java`](src/main/java/com/example/bookstore/book/BookV2Response.java) · [`BookController.java`](src/main/java/com/example/bookstore/book/BookController.java) · [`application.yml`](src/main/resources/application.yml)

> **Ticket: version 2 of a book.** Version 3.0 of the mobile app links each book to its category page and is getting ready for other currencies, so it needs the category's id and the price's currency. Changing `category` and `price` to objects would break every older app. Add version 2 of `GET /api/books/{id}`, chosen with the `API-Version` header; everything else stays as it is.

**Before you start:** lesson section 10.

**You'll practice:** Spring Framework 7's API versioning, `spring.mvc.apiversion.*`, the `version` attribute of request mappings.

**Acceptance criteria**

- `API-Version: 2` answers [`BookV2Response`](src/main/java/com/example/bookstore/book/BookV2Response.java) (its Javadoc shows the JSON). `2.0` and `v2` mean the same version.
- A request without `API-Version`, or with `API-Version: 1`, answers version 1, exactly as before.
- Other endpoints answer as before, whatever supported version the request names.
- A version the API doesn't have (`3`, `0`, `abc`) answers `400` with code `INVALID_INPUT`, for every endpoint.
- Version 2 of a book that doesn't exist answers `404` with code `BOOK_NOT_FOUND`.

<details>
<summary>Hint 1</summary>

Configure versioning first, in `application.yml` under `spring.mvc.apiversion`: where the version comes from, and the default for requests without one. A mapping with a `version` attribute stops the application from starting until then.
</details>

<details>
<summary>Hint 2</summary>

`getBookV2` is a second `@GetMapping("/{id}")`, with `version = "2"`. `BookService.findBookV2` is ready; it calls your `BookV2Response.from`.
</details>

**Done?** Compare with the reference, then ask yourself: Which of these changes would need a new version: a new response field `subtitle`, renaming `author` to `authors`, a new optional request field? What would you change in the tests to version the API by path (`/api/v2/books/1`) instead?

### S04-09 · OpenAPI documentation ★

**Build** · 30 min · [`OpenApiConfiguration.java`](src/main/java/com/example/bookstore/common/OpenApiConfiguration.java) · [`BookController.java`](src/main/java/com/example/bookstore/book/BookController.java) · [`MemberController.java`](src/main/java/com/example/bookstore/member/MemberController.java) · [`application.yml`](src/main/resources/application.yml)

> **Ticket: an API description the front end can trust.** The front-end team generates its TypeScript client from `/v3/api-docs`. Today the document calls itself "OpenAPI definition", promises `200` where the API answers `201` or `204`, documents no errors, publishes the admin API, and asks for a `member` parameter that nobody sends. Fix the document.

**Before you start:** lesson section 11. Open `http://localhost:8080/swagger-ui.html` with `bootRun` before and after.

**You'll practice:** springdoc-openapi: the `OpenAPI` bean, `@Tag`, `@Operation`, `@ApiResponse`, `@Parameter`, springdoc properties.

**Acceptance criteria**

- The document's title is `Bookstore API`, its version `1`.
- The book operations have the tag `Books`. `GET /api/books/{id}` has the summary `Get a book`, `POST /api/books` `Add a book`, and `DELETE /api/books/{id}` `Remove a book`.
- `POST /api/books` documents exactly `201` (with `BookResponse`), `400` and `409`; `DELETE /api/books/{id}` exactly `204`. `GET /api/books/{id}` documents its `404`. Each error response has the media type `application/problem+json` and the `ProblemDetail` schema.
- `GET /api/members/me` documents the required header `X-Member-Id` and no other parameter.
- Successful responses are documented as `application/json`, not as `*/*` (any media type).
- Nothing under `/api/admin` is in the document.
- The request schemas show the validation rules (from S04-02), and the Swagger UI is reachable.

<details>
<summary>Hint 1</summary>

An `@Bean` method that returns `new OpenAPI().info(new Info().title(...).version(...))`. Two springdoc properties in `application.yml` cover the admin API and the media type: `springdoc.paths-to-exclude` and `springdoc.default-produces-media-type`.
</details>

<details>
<summary>Hint 2</summary>

`@ApiResponse(responseCode = "201", description = "...", useReturnTypeSchema = true)` documents a success with the method's return type. Once a method declares any `@ApiResponse` for a success, springdoc stops adding its default `200`. On `MemberController`: `@Parameter(hidden = true)` on the parameter, and a method-level `@Parameter(name = "X-Member-Id", in = ParameterIn.HEADER, required = true)`.
</details>

**Done?** Compare with the reference, then ask yourself: The `ProblemDetail` schema shows a `properties` object, while the real JSON has `code` at the top level. How could the document describe the real shape? Would you publish `/v3/api-docs` of an internal API on the internet?

### S04-10 · File upload and CORS ★

**Build** · 30 min · [`BookCoverController.java`](src/main/java/com/example/bookstore/book/BookCoverController.java) · [`CoverService.java`](src/main/java/com/example/bookstore/book/CoverService.java) · [`ApiCorsConfiguration.java`](src/main/java/com/example/bookstore/common/web/ApiCorsConfiguration.java)

> **Ticket: covers, and the new shop front end.** (1) The admin tool uploads a cover image per book: `PUT /api/books/{id}/cover`, a JPEG or PNG of at most 1 MB (`bookstore.covers.max-size`), kept in memory for now. (2) The new shop front end at `https://shop.example.com` calls the API from the browser; allow exactly that origin (`bookstore.cors.allowed-origins`).

**Before you start:** lesson section 12. `GET /api/books/{id}/cover` already works; read it, [`CoverStorage`](src/main/java/com/example/bookstore/book/CoverStorage.java) and [`ImageFormat`](src/main/java/com/example/bookstore/book/ImageFormat.java).

**You'll practice:** multipart uploads with `MultipartFile`, checking a file's size and real type, binary responses, CORS with `CorsRegistry`.

**Acceptance criteria**

- `PUT /api/books/{id}/cover` with a multipart part `file` that holds a PNG or JPEG answers `204`. Afterwards `GET` answers the same bytes with `Content-Type: image/png` or `image/jpeg`. A second upload replaces the first.
- The bytes decide the type: a PNG sent as `application/octet-stream` is stored as `image/png`; a GIF that claims to be `image/png` answers `415` with code `UNSUPPORTED_MEDIA_TYPE`.
- A cover of exactly 1 MB (1,048,576 bytes) is accepted; one byte more answers `413` with code `COVER_TOO_LARGE`, and nothing is stored. The rules, their order and their messages are in the Javadoc of `CoverService.store`.
- An empty file or a request without the part `file` answers `400` with code `INVALID_INPUT`; a request that isn't `multipart/form-data` answers `415`; an unknown book `404`.
- CORS, for every path under `/api/`: the rules are in the Javadoc of `ApiCorsConfiguration`. A preflight from `https://shop.example.com` for a `PATCH` with `If-Match` is allowed; one from any other origin, or for the header `X-Admin-Token`, or for another method, answers `403`. Requests without an `Origin` header get no CORS headers.

<details>
<summary>Hint 1</summary>

`@PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)` and `@RequestParam("file") MultipartFile file`. Check `file.getSize()` before you read the bytes with `getBytes()`; `coverProperties.maxSize().toBytes()` is the limit.
</details>

<details>
<summary>Hint 2</summary>

`registry.addMapping("/api/**").allowedOrigins(...).allowedMethods(...).allowedHeaders(...).exposedHeaders(...).maxAge(...)`. `CorsProperties` is a bean: inject it into `ApiCorsConfiguration`.
</details>

**Done?** Compare with the reference, then ask yourself: Why does the test for a 1 MB + 1 byte cover reach your code, although Tomcat would reject such a file? Which header would the front end miss if `ETag` weren't exposed, and which feature would break? Why doesn't CORS stop `curl -X DELETE`?

## Review questions

1. List, in order, the components of Spring MVC that a request for `GET /api/books/1` passes through, and what each does.
2. Why must a controller be stateless? What goes wrong if it keeps the current request's data in a field?
3. A client sends `{"price": null}` to an endpoint with `@RequestBody CreateBookRequest request`, and nothing complains. Name two possible causes.
4. Why does every error response of the bookstore have a `code`, when the status code already says what kind of error it is?
5. What happens to an exception that a servlet filter throws, and to one that a handler interceptor throws?
6. A client sends `"price": 32000.5`. What does Jackson 3 do by default, and how do you change it for the whole application?
7. Give two reasons to allow only a list of known properties in a `sort` parameter.
8. Two admins edit book 7 at the same time. Explain, request by request, how `ETag` and `If-Match` keep both changes from silently overwriting each other.
9. Is `PUT` idempotent? Is `PATCH`? What should a repeated `DELETE` answer?
10. Which changes to an API need a new version, and which don't?
11. Why can't you trust the `Content-Type` of an uploaded file, and what do you check instead?
12. A colleague says: "CORS allows only our front end, so nobody else can call the API." What's wrong with that?
13. When do you test a controller with `@WebMvcTest`, and when with `@SpringBootTest` and `@AutoConfigureMockMvc`?

<details>
<summary>Answers</summary>

1. Tomcat gives the request a thread. The servlet filters run (the request-id filter). The `DispatcherServlet` asks its handler mappings which method handles the path: `BookController.getBook`. Interceptors registered for the path run their `preHandle`. The handler adapter builds the arguments with argument resolvers (`@PathVariable` converts `"1"` to `1`) and calls the method. A return value handler writes the result as JSON with an `HttpMessageConverter` (Jackson). Interceptors finish, and if anything threw, the exception resolvers, including the `@RestControllerAdvice`, build the error response. The filters see the response on its way out.
2. Spring creates one instance and every request thread uses it at the same time. A field shared by all requests mixes up users: one request reads the page that another request stored a moment before. Keep per-request data in parameters and local variables.
3. `@Valid` is missing, so the constraints aren't checked at all; or the field has no `@NotNull` (or `@NotBlank`), because most constraints accept `null`.
4. One status covers many errors: `409` can mean a duplicate ISBN, an empty stock or a used coupon, and `404` an unknown book or an unknown path. Clients need to react differently (show "this ISBN exists", or retry later), and they shouldn't parse English `detail` texts, which may change.
5. A filter runs outside Spring MVC, so `@RestControllerAdvice` never sees its exception. It reaches the servlet container, and Spring Boot's fallback `/error` endpoint answers `500` in its own format, not as a problem detail. An exception from an interceptor goes through Spring MVC's exception resolvers, so the advice turns it into a problem detail.
6. It cuts the value to `32000`, silently. `spring.jackson.deserialization.accept-float-as-int: false` in `application.yml` makes it fail, and the client gets a `400`.
7. Sorting by any field would expose internal fields; sorting by a column without a database index makes every request slow; and in SQL, a sort property copied from the request invites SQL injection (S05). An allow list also keeps the API stable when the internal model changes.
8. Both `GET` the book and receive `ETag: "3"`. Anna sends `PUT` with `If-Match: "3"`; the version still is 3, so her change is saved and the book gets version 4 and `ETag: "4"`. Ben sends `PUT` with `If-Match: "3"`; the book now has version 4, so the server answers `412` and changes nothing. Ben reloads the book, sees Anna's change, and sends his change again with `If-Match: "4"`.
9. `PUT` is idempotent: sending the same complete state twice leaves the same state. `PATCH` isn't necessarily: "add 1 to the stock" changes the state again each time, although a merge patch that sets fields is idempotent. A repeated `DELETE` leaves the same state as the first, so it is idempotent too; it answers `404` or `204`, by the API's choice. The bookstore answers `204`, so that a client's retry doesn't look like a failure.
10. Breaking changes need one: removing or renaming a field, changing a field's type or meaning, making an optional request field required, changing a status code's meaning. Additive changes don't: a new optional request field, a new response field, a new endpoint.
11. The client chooses the content type (and the file name): anyone can send a script as `image/png`. Check the file's first bytes, its magic number, and use the detected type. Also check the size, and never build a path on disk from the file name.
12. CORS is enforced by browsers only. It stops a page from another origin from reading responses in a user's browser; `curl`, scripts and other servers ignore it. Authentication and authorization protect the API (S12).
13. `@WebMvcTest` starts only the web layer, with the services replaced by mocks: fast and focused on HTTP mapping, validation and error handling of one controller. `@SpringBootTest` with `@AutoConfigureMockMvc` starts the whole application: slower, but it tests the real wiring, the services and the database. Use slices for many detailed web cases, and full tests for the important paths through all layers.
</details>

## Further reading

- Spring Framework reference: [Spring Web MVC](https://docs.spring.io/spring-framework/reference/web/webmvc.html), [the DispatcherServlet](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-servlet.html), [annotated controllers](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller.html), [validation](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html), [error responses](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html), [interceptors](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-servlet/handlermapping-interceptor.html), [HTTP caching and ETags](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-caching.html), [API versioning](https://docs.spring.io/spring-framework/reference/web/webmvc-versioning.html), [CORS](https://docs.spring.io/spring-framework/reference/web/webmvc-cors.html), [multipart](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-servlet/multipart.html), [MockMvcTester](https://docs.spring.io/spring-framework/reference/testing/mockmvc/assertj.html).
- Spring Boot reference: [servlet web applications](https://docs.spring.io/spring-boot/reference/web/servlet.html), [JSON](https://docs.spring.io/spring-boot/reference/features/json.html), [Spring MVC how-to guides](https://docs.spring.io/spring-boot/how-to/spring-mvc.html).
- Jackson 3: [Introducing Jackson 3 support in Spring](https://spring.io/blog/2025/10/07/introducing-jackson-3-support-in-spring) and the [migration guide](https://github.com/FasterXML/jackson/blob/main/jackson3/MIGRATING_TO_JACKSON_3.md), with every changed default.
- Bean Validation: [the specification](https://beanvalidation.org/) and the [Hibernate Validator reference](https://docs.jboss.org/hibernate/validator/9.1/reference/en-US/html_single/), including custom constraints.
- [springdoc-openapi](https://springdoc.org/) and the [OpenAPI specification](https://swagger.io/specification/).
- The standards: [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) (problem details), [RFC 9110](https://www.rfc-editor.org/rfc/rfc9110) (HTTP semantics: methods, status codes, conditional requests), [RFC 7396](https://www.rfc-editor.org/rfc/rfc7396) (JSON Merge Patch), [RFC 6585](https://www.rfc-editor.org/rfc/rfc6585) (`428 Precondition Required`).
- [MDN: Cross-Origin Resource Sharing](https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/CORS), the clearest explanation of preflight requests.
- The [NestJS documentation](https://docs.nestjs.com/), to compare middleware, guards, interceptors and pipes with this module.
