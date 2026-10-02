# Handbook: how good Java teams write code

These pages collect the conventions that well-run Java and Spring teams follow. They are short on purpose: each rule comes with the reason behind it, so you can apply it with judgment instead of by rote.

The reference solutions on the `solutions` branch follow these rules. When you compare your solution with the reference, use these pages as your review checklist. The module lessons link here from their "In real projects" sections.

## Java (Part 1)

| Page | What it covers |
|---|---|
| [Java style](java-style.md) | Naming, file layout, methods, comments, `var`, formatting |
| [Immutability](immutability.md) | Final fields, records, defensive copies, value objects, `equals`/`hashCode` |
| [Null and Optional](null-and-optional.md) | Where `null` is allowed, when to return `Optional`, validating arguments |
| [Exceptions](exceptions.md) | Checked vs unchecked, which exception to throw, messages, anti-patterns |
| [Collections and generics](collections-and-generics.md) | Choosing a collection, immutable collections, wildcards |
| [Functional style](functional-style.md) | Streams vs loops, lambdas, side effects |
| [Dates and money](dates-and-money.md) | `java.time`, time zones, `Clock`, `BigDecimal` |
| [Concurrency](concurrency.md) | Thread-safety strategies, executors, locks, virtual threads |
| [Testing](testing.md) | Test structure, naming, AssertJ, edge cases, mocks |
| [Git workflow](git-workflow.md) | This repository's branches, commits, comparing with the solutions |

## Spring Boot (Part 2)

| Page | What it covers |
|---|---|
| [Spring layering](spring-layering.md) | Package by feature, controllers vs services vs repositories, DTOs vs entities, constructor injection |
| [JPA entities and repositories](jpa-entities.md) | Entity rules, relationships, N+1, the persistence context, open-session-in-view |
| [Transactions](transactions.md) | Where `@Transactional` goes, rollback rules, propagation, isolation, short transactions |
| [REST API design](rest-api-design.md) | Paths, methods, status codes, problem-detail errors, validation, paging, versioning |
| [Configuration](configuration.md) | Property sources, `@ConfigurationProperties`, profiles, secrets |
| [Logging and observability](logging-and-observability.md) | Log levels, request ids, structured logs, Actuator, metrics |
| [Lombok](lombok.md) | Which annotations to use, and which never to put on entities |
| [Testing Spring applications](spring-testing.md) | Unit vs slice vs integration tests, Testcontainers, context caching, data isolation |
| [Security essentials](security.md) | Authentication vs authorization, passwords, JWT, ownership checks, CSRF and CORS |

## A note on style guides

Most companies start from the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html) and adjust it. What matters most is that a team agrees on one style and lets a tool enforce it (IntelliJ's formatter, Spotless, Checkstyle). Don't spend code review time on formatting.
