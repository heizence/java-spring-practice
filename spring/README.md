# Part 2 · Spring Boot

Part 2 teaches Spring Boot's core on one application: an online bookstore with MySQL. It uses Spring Boot 4.1 on Java 25, with MySQL 9.7 in Docker. Read [BOOKSTORE.md](BOOKSTORE.md) first: it describes the application, its database and the seed data your tests use.

## How it works

- **One application throughout.** Every module folder holds a complete copy of the bookstore at the point where that module starts (a *checkpoint*). A mistake in an earlier module never blocks you.
- **Exercises are tickets**, like at work: "Implement `POST /api/orders`…", "Bug: stock goes negative during flash sales". Acceptance tests define when a ticket is done.
- **Tests live next to the code** in the application's own packages, as in real projects. `./score s06` finds them by their display name (`S06-05 · …`).
- **Docker must be running.** Modules with a database start MySQL in Docker for the tests (Testcontainers) and for `bootRun` (Docker Compose).
- **S01's first exercise creates the application from scratch**, so you know how a Spring Boot project is put together before you use the prepared bookstore.

## Modules

| Week | Module | Topics |
|---|---|---|
| 7 | [S01 · Spring Boot fundamentals, IoC and DI](s01-boot-fundamentals/README.md) | starters, auto-configuration, beans, constructor injection, scopes, conditional beans, Lombok |
| 7 | [S02 · Configuration and profiles](s02-configuration/README.md) | `application.yml`, `@ConfigurationProperties`, profiles, secrets |
| 7–8 | [S03 · AOP](s03-aop/README.md) | aspects, pointcuts, proxies, the self-invocation trap |
| 8–9 | [S04 · REST APIs](s04-rest-apis/README.md) | controllers, DTOs, validation, `ProblemDetail` errors, Jackson 3, filters vs interceptors, paging |
| 9 | [S05 · Database foundations](s05-database-foundations/README.md) | MySQL in Docker, Flyway migrations, `JdbcClient`, SQL injection |
| 9–10 | [S06 · Spring Data JPA](s06-spring-data-jpa/README.md) | entities, repositories, relationships, the persistence context, N+1 queries |
| 10–11 | [S07 · Transactions and ACID](s07-transactions/README.md) | rollback rules, propagation, isolation levels in MySQL, short transactions |
| 11 | [S08 · Concurrency and data integrity](s08-concurrency-integrity/README.md) | lost updates, optimistic and pessimistic locking, deadlocks, idempotency |
| 11–12 | [S09 · Events, async and scheduling](s09-events-async/README.md) | after-commit events, `@Async`, scheduled jobs, the outbox pattern |
| 12 | [S10 · Caching](s10-caching/README.md) | Spring Cache with Caffeine, invalidation, stale data |
| 12 | [S11 · Calling other services](s11-http-clients/README.md) | `RestClient`, HTTP interface clients, timeouts, retries |
| 12–13 | [S12 · Security essentials](s12-security/README.md) | filter chain, password hashing, JWT, authorization rules |
| 13 | [S13 · Testing strategy](s13-testing-strategy/README.md) | controller and repository slice tests |
| 13 | [S14 · Production readiness](s14-production-readiness/README.md) | request-id logging, Actuator health and metrics, graceful shutdown, packaging |
| after 13 | [Capstone ★](capstone/README.md) | build a small API from an empty folder, with your own tests |

Later electives may cover Redis, Kafka, RabbitMQ, WebSocket and more.
