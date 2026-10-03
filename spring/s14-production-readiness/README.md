# S14 · Production readiness

⏱ 3 sessions · week 13 · 3 exercises

Code that passes its tests still has to run in production: for months, on several machines at once, deployed many times a week, with nobody watching most of the time. This module adds what an application needs there. Request ids tie the log lines of one request together. Health checks tell Kubernetes when to restart an instance and when to send it traffic. Metrics count business events and time slow operations. Graceful shutdown lets a deployment stop old instances without failing requests. A container image packages it all. When something breaks at 3 a.m., these are the tools you have. Read lesson sections 1–4 before S14-01, sections 5–7 before S14-02, and sections 8–10 before S14-03.

## Lesson

### 1. Logging: rules and levels

Spring Boot logs through SLF4J with Logback behind it. Lombok's `@Slf4j` gives a class a `log` field:

```java
log.info("Order {} placed by member {}: {} won", order.getId(), member.getId(), order.getTotalAmount());
log.warn("Payment provider slow: {} ms for order {}", elapsedMillis, orderId);
log.error("Could not send the confirmation for order {}", orderId, exception); // the exception last: stack trace
```

- **Placeholders, not string concatenation.** `{}` is filled in only if the level is switched on, and the call stays readable.
- **Choose the level by who must act:** `ERROR` when someone must look (a payment couldn't be recorded), `WARN` for something unusual that was handled (a retry succeeded), `INFO` for business events and the application's life cycle (order placed, application started), `DEBUG` for details you need while you hunt a bug.
- **Levels per logger:** `logging.level.com.example.bookstore: debug` in `application.yml`, or `LOGGING_LEVEL_COM_EXAMPLE_BOOKSTORE=debug` in the environment.
- **Log an exception once,** where it's handled, with its stack trace: the bookstore's `GlobalExceptionHandler` does that for bugs.
- **Never log secrets or personal data:** passwords, tokens, card numbers, and email addresses where your privacy rules forbid them. Ids are fine.

### 2. Request ids and the MDC

A busy server writes the lines of many requests at the same time, mixed together. To find the lines of one request, give each request an id and print it on every line.

The **MDC** (*mapped diagnostic context*) is a small map of strings that SLF4J keeps for each thread. A log pattern can print its entries: `%X{requestId}`. Like `AsyncLocalStorage` in Node.js, it carries request data through every method call without passing it as a parameter. A servlet filter is the right place to fill it, because it runs before everything else that handles the request:

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // first, so that other filters' log lines have the id too
public class TenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        MDC.put("tenant", tenantOf(request));
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove("tenant"); // the thread serves another request next
        }
    }
}
```

- **Clean up in `finally`.** Server threads come from a pool. An entry that stays in the MDC shows up in the log lines of the next request that the thread serves, also when this request failed.
- **Reuse an incoming id.** When a proxy or another service sends `X-Request-Id`, use it, and the same id appears in every service's logs. Return it in the response too, so that a customer can quote it in a bug report.
- **Never trust it blindly.** A header comes from the client. A line break in it can forge whole log lines (*log injection*), and a megabyte of text fills your logs. Accept a short, safe set of characters, and create a new id for anything else.
- **Print it:** Spring Boot's console pattern has a slot for this, `logging.pattern.correlation`. With `"[%X{requestId:-}] "`, every line shows `[3f1c…]` between the thread and the logger name; `:-` prints nothing when the entry is missing.

Larger systems use *distributed tracing* (Micrometer Tracing with OpenTelemetry): each request gets a trace id that every service passes on, and Spring Boot prints it in the same correlation slot. A request id of your own is still common as the id that customers and support staff see.

### 3. Taking the MDC to other threads

The MDC belongs to one thread. Work that moves to another thread, such as an `@Async` method or a task for an executor, starts with an empty MDC, and its log lines lose the request id.

Spring's answer is a `TaskDecorator`: it wraps each task when it's handed to an executor, on the submitting thread, before a pool thread runs it.

```java
Runnable decorated = taskDecorator.decorate(task);  // on the request's thread: take what you need now
// …later, a pool thread runs decorated.run(): set it up, run the task, clean up
```

Spring Boot configures the executor behind `@Async` methods (`applicationTaskExecutor`, threads named `task-1`, `task-2`, …) and gives it the application's `TaskDecorator` bean. A decorator for the MDC copies the MDC in `decorate`, sets the copy on the pool thread while the task runs, and afterwards gives the pool thread back the MDC it had before, also when the task throws. For an executor that you create yourself, set the decorator yourself: `executor.setTaskDecorator(decorator)`. Spring Framework's `ContextPropagatingTaskDecorator` does the same for everything that Micrometer's context-propagation library knows, such as the trace context.

### 4. Structured logs

On your laptop you read logs. In production a log platform (Elasticsearch, Loki, CloudWatch) searches millions of lines, and it works much better with JSON, where every value is a field:

```yaml
logging:
  structured:
    format:
      console: ecs   # Elastic Common Schema; logstash and gelf are built in too
```

A line then looks like this (shortened):

```json
{"@timestamp":"2026-10-15T00:00:00.000Z","log":{"level":"INFO","logger":"com.example.bookstore.order.OrderService"},"process":{"pid":1,"thread":{"name":"http-nio-8080-exec-1"}},"service":{"name":"bookstore"},"message":"Order 201 placed by member 1: 36000 won","requestId":"3f1c9a2e-…","ecs":{"version":"8.11"}}
```

MDC entries become fields of their own, so "every line of request `3f1c…`" is a query, not a text search. Keep the readable text format for local development, and switch to JSON in production, for example with `application-prod.yml`.

### 5. Actuator: endpoints, exposure and safety

`spring-boot-starter-actuator` adds operational endpoints under `/actuator`: `health`, `info`, `metrics`, `loggers` (change log levels while the application runs), `env` (every configuration value), `heapdump` (all of the memory, secrets included), `threaddump`, `beans`, `mappings`, `shutdown`, and more.

- **Only `health` is on the web by default.** Turn on what operations need, and nothing else: `management.endpoints.web.exposure.include: health, info, metrics`. `env` and `heapdump` reveal secrets, and `shutdown` stops the application.
- **Keep them away from the internet.** A common setup moves them to another port (`management.server.port: 8081`) that only the cluster can reach. With Spring Security, add a rule for `/actuator/**`; S12 showed how. Kubernetes' probes need `health` without a token.
- **Health details** (disk space, database version) stay hidden unless you show them: `management.endpoint.health.show-details` and `show-components` take `never`, `when-authorized` or `always`. Showing which checks exist, and their status, is safe; showing their details on a public port isn't.

### 6. Health checks: liveness, readiness and your own indicator

Kubernetes asks every container two questions, through *probes*:

| Probe | Question | When it fails | Endpoint |
|---|---|---|---|
| liveness | Is the application broken beyond repair? | Kubernetes restarts the container. | `/actuator/health/liveness` |
| readiness | May this instance get traffic now? | Kubernetes stops sending it requests, without a restart. | `/actuator/health/readiness` |

Spring Boot keeps the answers as *availability states*: `LivenessState` (`CORRECT` or `BROKEN`) and `ReadinessState` (`ACCEPTING_TRAFFIC` or `REFUSING_TRAFFIC`). The readiness state becomes `ACCEPTING_TRAFFIC` once the application has started, and `REFUSING_TRAFFIC` when it shuts down. Spring Boot 4 provides both endpoints by default, as *health groups*: named sets of health checks.

A *health indicator* is one check. Spring Boot has many (database, disk space), and you can add your own:

```java
@Component // listed as "paymentProvider": the bean name without "HealthIndicator"
public class PaymentProviderHealthIndicator extends AbstractHealthIndicator {

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        builder.up().withDetail("latencyMillis", paymentProvider.ping().toMillis());
    }
}
```

`AbstractHealthIndicator` turns an exception from `doHealthCheck` into `DOWN`, with the exception in the detail `error`. The statuses are `UP`, `DOWN`, `OUT_OF_SERVICE` and `UNKNOWN`. A group has the worst status of its members, and `DOWN` and `OUT_OF_SERVICE` answer with HTTP `503`. Groups are configuration:

```yaml
management:
  endpoint:
    health:
      group:
        readiness:
          include: readinessState, paymentProvider
```

- **Never put a dependency into liveness.** When the database is down, a restart doesn't help, and restarting every instance at once makes the outage worse.
- **Put into readiness only what this instance can't work without,** and think about what happens when every instance fails the same check at the same moment: all of them stop taking traffic, and users get errors from the load balancer instead of from you.
- If you list the members of a group yourself, list them for both groups: a group that Spring Boot creates for you doesn't show its members.

### 7. Metrics with Micrometer

Logs tell you what happened to one request. Metrics tell you how often and how fast, over time. *Micrometer* is to metrics what SLF4J is to logging: one API, many back ends (Prometheus, Datadog, CloudWatch). Its `MeterRegistry` creates and holds the *meters*:

```java
Counter cancelled = Counter.builder("bookstore.orders.cancelled")
        .description("Orders cancelled")
        .tag("reason", "out_of_stock")
        .register(meterRegistry);
cancelled.increment();

Timer.Sample sample = Timer.start(meterRegistry);
paymentProvider.charge(order);
sample.stop(meterRegistry.timer("bookstore.payment.duration", "method", "card"));
```

- **A counter** counts events: orders placed, coupons issued. **A timer** measures how long something takes, and counts it too. **A gauge** reports a current value, such as the size of a queue.
- **Names** are lowercase words separated by dots; each back end turns them into its own style (`bookstore_orders_placed_total` in Prometheus).
- **Tags** split a meter into *time series*: one series for each combination of tag values. Use tag values from a small, fixed set: an outcome, a status, a category. A member id or an order id as a tag creates a new series for every member or order, and the metrics system has to keep all of them. Monitoring systems have run out of memory that way.
- **Register meters at startup.** A counter that appears only with the first event shows "no data" instead of 0 until then, and an alert on it can't tell the difference.
- Spring Boot already measures a lot: `http.server.requests` (every endpoint's timing and status), `jvm.*`, `hikaricp.*`. `GET /actuator/metrics` lists the names, `GET /actuator/metrics/bookstore.orders.placed` shows one meter. In production, a back end collects them, for example Prometheus with `micrometer-registry-prometheus` and `/actuator/prometheus`.

### 8. Graceful shutdown

Every deployment stops the old instances. When Kubernetes stops a container, this happens:

1. Kubernetes sends `SIGTERM` to the container's main process, and starts removing the instance from the load balancer. Both happen at about the same time, so a few requests may still arrive.
2. Spring Boot closes the application context. First it publishes `REFUSING_TRAFFIC`, so the readiness probe fails.
3. With `server.shutdown: graceful`, Spring Boot's default, the web server stops accepting new connections and waits for running requests to finish.
4. The other parts stop in *phases*, for example the task executor of `@Async` methods. Each phase waits at most `spring.lifecycle.timeout-per-shutdown-phase` (default 30 s). With `spring.task.execution.shutdown.await-termination: true`, the executor lets running tasks finish instead of interrupting them.
5. If the process is still running after `terminationGracePeriodSeconds` (default 30 s), Kubernetes kills it with `SIGKILL`.

So keep the phase timeouts well below Kubernetes' grace period. Many teams also add a short `preStop` sleep (5–10 s) to the pod, so that the load balancer has removed the instance before step 2 begins.

Availability states are events, so you can publish and watch them yourself:

```java
AvailabilityChangeEvent.publish(applicationContext, ReadinessState.REFUSING_TRAFFIC); // for example, during a cache warm-up

@EventListener
void onReadinessChange(AvailabilityChangeEvent<ReadinessState> event) {
    log.info("Readiness is now {}", event.getState());
}
```

### 9. The executable jar and its layers

`./gradlew :spring:s14-production-readiness:bootJar` builds `build/libs/bookstore.jar`: an *executable jar* with the application's classes, every dependency and a small launcher inside. `java -jar bookstore.jar` starts it. For containers, the jar is also *layered*. Spring Boot sorts its contents into four groups, from the one that changes least to the one that changes most:

```bash
$ java -Djarmode=tools -jar build/libs/bookstore.jar list-layers
dependencies
spring-boot-loader
snapshot-dependencies
application
```

`java -Djarmode=tools -jar bookstore.jar extract --layers --destination extracted` writes one folder per layer: the dependencies' jars, and a small `bookstore.jar` with only your code, which `java -jar` starts.

### 10. Container images: a Dockerfile, or Buildpacks

An image is a stack of layers, and Docker rebuilds, uploads and downloads only the layers that changed. One `COPY` per jar layer, in the order above, means that a change to your code moves a few hundred kilobytes, not the 60 MB of dependencies. A production Dockerfile has two *stages*:

```dockerfile
# Stage 1: unpack the jar. This stage is thrown away after the build.
FROM eclipse-temurin:25-jre AS extract
WORKDIR /build
COPY build/libs/bookstore.jar bookstore.jar
RUN java -Djarmode=tools -jar bookstore.jar extract --layers --destination extracted

# Stage 2: the image that ships.
FROM eclipse-temurin:25-jre
# …create a user, copy the layers from the first stage, switch to the user…
ENTRYPOINT ["java", "-jar", "bookstore.jar"]
```

A comment in a Dockerfile must have a line of its own: Docker would read a `#` after an instruction as part of it.

- **A JRE, not a JDK:** a production image needs no compiler; a smaller image has fewer things to patch.
- **Not root:** create a user and switch to it with `USER`. Prefer a numeric id: Kubernetes can then verify that the container doesn't run as root.
- **Exec form:** `ENTRYPOINT ["java", …]` makes Java process 1, so that it receives `SIGTERM` and shuts down gracefully. The shell form, `ENTRYPOINT java -jar …`, starts a shell that doesn't pass the signal on, and Kubernetes kills the application 30 seconds later.
- **Memory:** the JVM sees the container's memory limit, but by default uses only 25% of it for the heap. `-XX:MaxRAMPercentage=75` gives the heap three quarters and leaves the rest for threads, metaspace and buffers.
- **A `.dockerignore`** that sends only the jar to the Docker daemon keeps the build fast and keeps sources and local files out of the image.

**The alternative: Buildpacks.** `./gradlew :spring:s14-production-readiness:bootBuildImage --imageName=bookstore:latest` builds an image without a Dockerfile, with Cloud Native Buildpacks (Paketo). The image is layered, runs as a non-root user, and calculates the JVM's memory settings from the container's limit when it starts. You give up some control; you gain an image built the same way for every service, with base images that the buildpacks keep up to date. Both are good choices; many companies standardize on one of them.

**Configuration stays outside the image** (the *twelve-factor* rules): the same image runs in every environment, and environment variables supply what differs, such as `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_PASSWORD` or `SPRING_PROFILES_ACTIVE=prod`. The other twelve-factor rules match this module too: logs go to standard output as a stream, a process starts fast and stops gracefully (*disposability*), and the application binds its own port.

## TypeScript/Node.js → Spring Boot

| Node.js / NestJS | Spring Boot | Watch out for |
|---|---|---|
| `pino`, `winston` | SLF4J with Logback, `@Slf4j` | Placeholders `{}`, not template strings. |
| `AsyncLocalStorage` | the MDC | It belongs to one thread and doesn't follow work to other threads by itself. |
| `express-request-id`, `cls-rtracer` | a `OncePerRequestFilter` that fills the MDC | Clear it in `finally`; check incoming ids. |
| context that follows promises | a `TaskDecorator` | Give the pool thread its old MDC back afterwards. |
| `pino` JSON output | `logging.structured.format.console: ecs` | Readable text for local development. |
| `@nestjs/terminus` health checks | Actuator `HealthIndicator`s and health groups | Dependencies never go into liveness. |
| `prom-client` | Micrometer `Counter`, `Timer`, `Gauge` | Tag values from a small, fixed set. |
| `process.on("SIGTERM", …)` and `server.close()` | `server.shutdown: graceful`, shutdown phases | `ENTRYPOINT` in exec form, or the signal never arrives. |
| multi-stage `node:22` Dockerfile with `USER node` | multi-stage JRE Dockerfile with the jar's layers, or `bootBuildImage` | Copy the layers in order. |
| `.env` files and `process.env` | environment variables with relaxed binding | Never bake configuration into the image. |

## In real projects

- Give every request an id in the MDC, print it on every line, and carry it to `@Async` work with a `TaskDecorator`; switch to structured logs in production. See [Logging and observability](../../docs/handbook/logging-and-observability.md).
- Expose only the Actuator endpoints you need, keep them off the public port or behind Spring Security, and never show health details publicly. See [Logging and observability](../../docs/handbook/logging-and-observability.md) and [Security essentials](../../docs/handbook/security.md).
- Count business events and time slow calls in Micrometer, with tags from small, fixed sets. See [Logging and observability](../../docs/handbook/logging-and-observability.md).
- Keep dependencies out of liveness; put into readiness only what this instance can't work without. See [Logging and observability](../../docs/handbook/logging-and-observability.md).
- Ship one image for every environment, and configure it with environment variables and secrets from a secret manager. See [Configuration](../../docs/handbook/configuration.md).
- Test logging with `OutputCaptureExtension`, metrics through the `MeterRegistry`, and health through `/actuator/health`. See [Testing Spring applications](../../docs/handbook/spring-testing.md).

## Exercises

This module has no Spring Security, so `POST /api/orders` names its member in the request body: `{"memberId": 1, "lines": [{"bookId": 1, "quantity": 1}]}`. Placing an order writes the log line `Order 201 placed by member 1: 36000 won`, and after the commit an `@Async` method confirms the order on another thread: `Sent the confirmation for order 201`.

### S14-01 · Logging with request IDs

**Build** · 30 min · [`RequestIdFilter.java`](src/main/java/com/example/bookstore/common/logging/RequestIdFilter.java), [`MdcTaskDecorator.java`](src/main/java/com/example/bookstore/common/logging/MdcTaskDecorator.java) and [`application.yml`](src/main/resources/application.yml)

> **Ticket:** A customer says that their order failed at 14:03. Support can't find the right lines among the thousands from that minute. Give every request an id, print it on every log line, and send it back to the client, so that support can ask for it.

**Acceptance criteria** (the Javadoc of both classes has the details):

- Every request gets an id: the `X-Request-Id` header if it is 1 to 64 safe characters (letters, digits, `-`, `_`, `.`), otherwise a new random UUID. The response has the id in its `X-Request-Id` header.
- Every log line written while the request runs shows the id. Add the MDC entry `requestId` to Spring Boot's console pattern in `application.yml`; the format around it is up to you.
- After the request, the MDC is empty again, also when the request failed.
- Log lines that `@Async` methods write on other threads show the id of the request that started them. The decorator restores the pool thread's own MDC afterwards, also when the task throws.

**Before you start:** lesson sections 1–4.

**You'll practice:** `OncePerRequestFilter`, `@Order`, the MDC, `try`/`finally` cleanup, validating input that goes into logs, `TaskDecorator`, `logging.pattern.correlation`, testing log output with `OutputCaptureExtension`.

<details>
<summary>Hint 1</summary>

`Pattern.compile("[A-Za-z0-9._-]{1,64}")` describes a safe id; `UUID.randomUUID().toString()` makes a new one. Set the response header before calling `chain.doFilter`: once the controller has written the body, headers can't change anymore.
</details>

<details>
<summary>Hint 2</summary>

`MDC.getCopyOfContextMap()` copies the current thread's MDC, and returns `null` when it's empty. `MDC.setContextMap(map)` replaces a thread's MDC; `MDC.clear()` empties it. Which of them runs in `decorate`, and which inside the `Runnable` you return?
</details>

<details>
<summary>Hint 3</summary>

`logging.pattern.correlation: "[%X{requestId:-}] "` prints the id in the slot that Spring Boot keeps for it.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Which lines of a request don't show its id, and why? What would a request id like `abc\nINFO Admin password changed` have done to your logs?

### S14-02 · Health checks and metrics

**Build** · 30 min · [`InventoryHealthIndicator.java`](src/main/java/com/example/bookstore/inventory/InventoryHealthIndicator.java), [`OrderMetrics.java`](src/main/java/com/example/bookstore/order/OrderMetrics.java), [`OrderService.java`](src/main/java/com/example/bookstore/order/OrderService.java) and [`application.yml`](src/main/resources/application.yml)

> **Ticket:** The bookstore moves to Kubernetes, and the operations team asks for three things: probes they can trust, no Actuator endpoint that reveals internals, and two business metrics for their dashboard, orders placed and how long placing one takes.

**Acceptance criteria:**

- `InventoryHealthIndicator` (listed as `inventory`) is `UP` with the details `booksInStock` and `booksOutOfStock`, and `DOWN` with an `error` detail when the stock can't be read. Its Javadoc has the details.
- The liveness group checks only `livenessState`. The readiness group checks `readinessState` and `inventory`. Define both groups, and let health responses list each check's status, but none of their details.
- On the web, Actuator exposes `health`, `info` and `metrics`, and nothing else.
- `OrderMetrics` records the counter `bookstore.orders.placed` and the timer `bookstore.orders.placement` with the tag `outcome` (`placed` or `rejected`), as its Javadoc says. `OrderService.placeOrder` places every order through `OrderMetrics.recordPlacement`.

**Before you start:** lesson sections 5–7.

**You'll practice:** `HealthIndicator` and `AbstractHealthIndicator`, health groups, `management.*` settings, Micrometer's `MeterRegistry`, `Counter`, `Timer` and `Timer.Sample`, tag cardinality, testing through `/actuator` and the `MeterRegistry`.

<details>
<summary>Hint 1</summary>

The settings live under `management.endpoints.web.exposure.include`, `management.endpoint.health.show-components` and `management.endpoint.health.group.<name>.include`. Lesson sections 5 and 6 show them.
</details>

<details>
<summary>Hint 2</summary>

Register the counter in `OrderMetrics`' constructor, so that it exists before the first order: replace Lombok's `@RequiredArgsConstructor` with a constructor of your own that takes the `MeterRegistry`. Time with `Timer.start(registry)` and `sample.stop(…)` in a `finally` block, so that a rejected order is timed too.
</details>

<details>
<summary>Hint 3</summary>

In `OrderService`, move the body of `placeOrder` into a private method, and let `placeOrder` call `orderMetrics.recordPlacement(() -> place(request))`. Add `OrderMetrics` as a constructor dependency.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: If the database is down for every instance, what do your probes do, and what do customers see? Which tag would you add to `bookstore.orders.placed` to split it by category, and why is that tag safe while a member id isn't?

### S14-03 · Graceful shutdown and packaging

**Build** · 30 min · [`application.yml`](src/main/resources/application.yml) and [`Dockerfile`](Dockerfile)

> **Ticket:** Deployments fail a few requests every time, and the security scanner complains about the image: it runs as root, ships a whole JDK, and every release re-uploads all 60 MB of dependencies. Make shutdowns graceful, and give the bookstore a production-quality image.

**Acceptance criteria:**

- The web server shuts down gracefully (`server.shutdown: graceful`; write it down, even though it's Spring Boot's default). Each shutdown phase gets at most 20 seconds, and running `@Async` tasks get up to 10 seconds to finish.
- When the application shuts down, readiness turns to `REFUSING_TRAFFIC` before the web server stops; while it refuses traffic, the readiness probe answers `503 OUT_OF_SERVICE` and liveness stays `UP`. Spring Boot does this for you; the tests prove that your settings keep it working.
- The `Dockerfile` has two stages: a named stage that unpacks `build/libs/bookstore.jar` with `java -Djarmode=tools … extract --layers`, and a final stage on a Java 25 JRE that copies the four layers one by one, in order. It runs as a user that isn't root, documents port 8080, sets `-XX:MaxRAMPercentage`, and starts `java` with an `ENTRYPOINT` in exec form.

The tests read the `Dockerfile` as text; they never build an image. If you want to build and run it yourself (Docker Desktop must be running):

```bash
./gradlew :spring:s14-production-readiness:bootJar
docker build -t bookstore spring/s14-production-readiness
```

The image needs a database. Pass the connection as environment variables: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD`. Lesson section 10 shows the Buildpacks alternative, `bootBuildImage`.

**Before you start:** lesson sections 8–10.

**You'll practice:** graceful shutdown and its phases, availability states, multi-stage Dockerfiles, Spring Boot's jar layers, running as a non-root user, exec form and signals, container memory settings.

<details>
<summary>Hint 1</summary>

The settings are `server.shutdown`, `spring.lifecycle.timeout-per-shutdown-phase`, and `spring.task.execution.shutdown.await-termination` and `await-termination-period`. Durations are written like `20s`.
</details>

<details>
<summary>Hint 2</summary>

Lesson section 10 shows both stages. In the final stage: `RUN groupadd --system --gid 10001 bookstore && useradd --system --uid 10001 --gid bookstore bookstore`, then `COPY --from=<stage> /build/extracted/<layer>/ ./` for each layer, then `USER 10001:10001`.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Your pod has `terminationGracePeriodSeconds: 30` and a `preStop` sleep of 10 seconds. How long may a request take, at most, and still finish during a deployment? What changes in the image when you change one line of `OrderService`?

## Review questions

1. Why does a filter that puts the request id into the MDC have to remove it in a `finally` block?
2. Why can't the filter put any `X-Request-Id` header it receives into the MDC?
3. An `@Async` method's log lines have no request id. Why, and what does a `TaskDecorator` change?
4. Which Actuator endpoints should never be reachable from the internet, and why?
5. What is the difference between liveness and readiness? Why must the database never be part of liveness?
6. Why is a member id a bad tag for a counter, while an outcome such as `placed` or `rejected` is a good one?
7. Why should the bookstore register its counter at startup instead of creating it with the first order?
8. What happens, in order, when Kubernetes stops a pod of the bookstore?
9. A Dockerfile ends with `ENTRYPOINT java -jar app.jar`. What goes wrong at every deployment?
10. Why does the Dockerfile copy the jar's layers one by one instead of copying the jar?
11. When would you choose `bootBuildImage` over a Dockerfile?

<details>
<summary>Answers</summary>

1. Server threads come from a pool and serve one request after another. An entry left in the MDC appears in the log lines of the thread's next request. `finally` runs whether the request succeeded or threw.
2. The client controls the header. A line break in it can forge log lines, and a long value fills the logs. Accept only short ids of safe characters, and create a new id for anything else.
3. The MDC belongs to one thread, and the `@Async` method runs on a pool thread whose MDC doesn't have the id. A `TaskDecorator` wraps each task on the request's thread, copies the MDC there, sets the copy on the pool thread while the task runs, and restores the pool thread's MDC afterwards.
4. Above all `env` and `configprops` (configuration values, secrets included), `heapdump` (all of the memory, with passwords and tokens in it), `loggers` (anyone could change log levels), `shutdown` (stops the application), and also `beans`, `mappings` and `threaddump`, which show how the application works inside. Expose only what operations need, and keep it off the public port.
5. Liveness asks whether the application is broken so that only a restart helps; readiness asks whether this instance may get traffic now. A database outage isn't fixed by restarts: with the database in liveness, Kubernetes restarts every instance at once, and the instances stay down even after the database recovers, until each one has started again.
6. Every distinct combination of tag values is a time series that the metrics system stores. Member ids create a series per member, without limit; an outcome has two values.
7. Until the first order, a counter created on demand doesn't exist, so dashboards show "no data" and alerts can't tell "no orders" from "no metrics". A counter registered at startup shows 0.
8. Kubernetes sends `SIGTERM` and removes the pod from the load balancer. Spring Boot publishes `REFUSING_TRAFFIC` (readiness fails), the web server stops taking new connections and waits for running requests, the other parts stop phase by phase (the task executor lets running tasks finish), and the process exits. If it takes longer than the grace period, Kubernetes kills it.
9. The shell form starts `/bin/sh -c "java -jar app.jar"`: the shell is process 1 and doesn't pass `SIGTERM` on to Java. Spring Boot never shuts down gracefully; Kubernetes kills it after the grace period, and the requests that were running fail.
10. Docker reuses unchanged layers. With one `COPY` per jar layer, a code change rebuilds and moves only the small `application` layer; with a single jar, every release moves all the dependencies again.
11. When you want images without writing or maintaining a Dockerfile: the buildpacks choose the base image and keep it patched, run as non-root, and size the JVM's memory for you, the same way for every service. A Dockerfile gives you full control, for example over the base image or extra tools in the image.
</details>

## Further reading

- Spring Boot reference: [Logging](https://docs.spring.io/spring-boot/reference/features/logging.html), including structured logging and the correlation pattern
- Spring Boot reference: [Production-ready Features](https://docs.spring.io/spring-boot/reference/actuator/index.html): [Endpoints](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html), health groups and [Kubernetes probes](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html#actuator.endpoints.kubernetes-probes), [Metrics](https://docs.spring.io/spring-boot/reference/actuator/metrics.html)
- Spring Boot reference: [Graceful Shutdown](https://docs.spring.io/spring-boot/reference/web/graceful-shutdown.html) and [Application Availability](https://docs.spring.io/spring-boot/reference/features/spring-application.html#features.spring-application.application-availability)
- Spring Boot reference: [Efficient Container Images](https://docs.spring.io/spring-boot/reference/packaging/container-images/efficient-images.html), [Dockerfiles](https://docs.spring.io/spring-boot/reference/packaging/container-images/dockerfiles.html) and [Cloud Native Buildpacks](https://docs.spring.io/spring-boot/reference/packaging/container-images/cloud-native-buildpacks.html)
- Micrometer: [Concepts](https://docs.micrometer.io/micrometer/reference/concepts.html), on meters, naming and tags
- Kubernetes: [Liveness, Readiness and Startup Probes](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/) and [Termination of Pods](https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/#pod-termination)
- [The Twelve-Factor App](https://12factor.net/)
- OWASP: [Logging Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html), on log injection and what never to log
