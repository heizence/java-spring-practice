# Logging and observability

When something goes wrong in production, logs, metrics and health checks are all you have. Write them for the person on call at 3 a.m., which may be you.

## Logging

Spring Boot uses SLF4J with Logback. With Lombok, `@Slf4j` on a class gives you a `log` field.

```java
log.info("Order {} paid, {} won", order.getId(), order.getTotalAmount());
log.warn("Payment provider slow: {} ms for order {}", elapsedMillis, orderId);
log.error("Could not publish outbox event {}", event.getId(), exception); // exception last: logs the stack trace
```

- **Use placeholders (`{}`), not string concatenation.** The message is only built if the level is enabled, and the code stays readable.
- **Choose the level by who must act:**

  | Level | Meaning | Example |
  |---|---|---|
  | `ERROR` | something failed and someone must look | a payment couldn't be recorded |
  | `WARN` | unusual, but handled | a retry succeeded on the third attempt |
  | `INFO` | important business events and lifecycle | order paid, application started |
  | `DEBUG` | details for diagnosing a problem | the SQL Hibernate runs (`logging.level.org.hibernate.SQL: debug`) |

- **Log an exception once**, with its stack trace, where it's handled (usually the global exception handler). Logging it at every layer floods the logs with the same failure.
- **Never log secrets or personal data:** passwords, tokens, full card numbers, and full email addresses or phone numbers where your privacy rules forbid it.
- **Log facts, not feelings:** "Coupon FLASH10000 sold out after 10 issues", not "Something weird happened".

## Request ids and MDC

One request produces many log lines, often interleaved with lines from other requests. Give every request an id and put it on every line:

- A servlet filter reads an incoming `X-Request-Id` header (or creates a UUID), stores it in the **MDC** (Mapped Diagnostic Context, a per-thread map that the logger adds to each line), returns it in the response header, and clears it when the request ends.
- **The MDC is per thread.** Work handed to another thread (`@Async`, executors) loses it unless you copy it, for example with a `TaskDecorator`.
- Distributed tracing (Micrometer Tracing with OpenTelemetry) generalizes this across services with trace and span ids.

## Structured logs

In production, write logs as JSON so log platforms can search and filter by field:

```yaml
logging:
  structured:
    format:
      console: ecs   # Elastic Common Schema; logstash and gelf are also built in
```

Keep human-readable text logs for local development.

## Health checks: Actuator

`spring-boot-starter-actuator` adds operational endpoints under `/actuator`:

- `/actuator/health`: aggregated status of the application and what it depends on (database, disk space, your own `HealthIndicator`s). Kubernetes uses the `liveness` and `readiness` groups to decide when to restart a container or send it traffic.
- `/actuator/info`, `/actuator/metrics`, `/actuator/prometheus` (with the Prometheus registry).
- **Expose only what you need** (`management.endpoints.web.exposure.include: health,info,metrics`), and protect everything except health.

## Metrics

Micrometer is to metrics what SLF4J is to logging: one API, many back ends (Prometheus, Datadog, CloudWatch).

```java
meterRegistry.counter("bookstore.orders.placed", "status", "paid").increment();
Timer.Sample sample = Timer.start(meterRegistry);
…
sample.stop(meterRegistry.timer("bookstore.payment.duration"));
```

- Count business events (orders placed, coupons issued) and time slow operations (payments, external calls).
- Keep tag values from a small, fixed set. A tag with a user id or an order id creates a new time series per value and can overload the metrics system.

## Graceful shutdown

When a new version is deployed, the old instance receives a stop signal. With graceful shutdown (the default since Spring Boot 3.4) the web server stops accepting new requests and lets running ones finish, within `spring.lifecycle.timeout-per-shutdown-phase`.
