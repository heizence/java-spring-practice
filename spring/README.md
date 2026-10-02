# Part 2 · Spring Boot

Part 2 arrives in two stages, before you finish Part 1. It uses Spring Boot 4.1 on Java 25, with MySQL 9.7 in Docker.

## How it works

- **One application throughout:** an online bookstore with members, books, inventory, orders, wallets, coupons and reviews. The schema and seed data are provided, so you spend your time on Spring, not on setup.
- **Exercises are tickets**, like at work: "Implement `POST /api/orders`…" or "Bug: stock goes negative during flash sales". Acceptance tests define when a ticket is done.
- **Each module starts from a complete checkpoint**, so a mistake in an earlier module never blocks you.
- **The first exercise creates a Spring Boot project from scratch**, so you know how one is bootstrapped before using the prepared bookstore.

## Modules

| Week | Module | Topics |
|---|---|---|
| 7 | S01 · Spring Boot fundamentals, IoC and DI | starters, auto-configuration, beans, constructor injection, scopes, conditional beans, Lombok |
| 7 | S02 · Configuration and profiles | `application.yml`, profiles, `@ConfigurationProperties`, secrets |
| 7–8 | S03 · AOP | proxies, `@Aspect`, pointcuts, the self-invocation trap |
| 8 | S04 · REST APIs | request lifecycle, DTOs, validation, `ProblemDetail` errors, Jackson 3, filters vs interceptors, paging |
| 9 | S05 · Database foundations | MySQL in Docker, HikariCP, Flyway migrations, `JdbcClient` |
| 9–10 | S06 · Spring Data JPA | entity rules, repositories, relationships, persistence context, N+1 queries |
| 10 | S07 · Transactions and ACID | propagation, rollback rules, isolation anomalies reproduced in MySQL |
| 11 | S08 · Concurrency and data integrity | lost updates, optimistic and pessimistic locking, deadlocks, idempotency |
| 11–12 | S09 · Events, async and scheduling | after-commit listeners, `@Async`, virtual threads, the outbox pattern |
| 12 | S10 · Caching | Spring Cache with Caffeine, invalidation |
| 12 | S11 · Calling other services | `RestClient`, HTTP interfaces, timeouts, retries |
| 12–13 | S12 · Security essentials | filter chain, password hashing, JWT, method security |
| 13 | S13 · Testing strategy | unit vs slice vs full tests, Testcontainers |
| 13 | S14 · Production readiness | logging, Actuator, graceful shutdown, Docker image |

Later electives may cover Redis, Kafka, RabbitMQ, WebSocket and more.
