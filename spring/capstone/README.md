# Capstone ★ · A to-do API from an empty folder

⏱ about 6 sessions · after week 13 · optional

Every exercise so far gave you a skeleton, a spec and tests. Real work doesn't. In this capstone you build a small but complete service on your own: you create the project, design the database, write the code **and the tests**. Nothing here is scored by `./score`. You decide when it's done, using the checklist at the end.

Use this README as the "product requirements" you'd get from a product manager. When something is unclear, make a decision and write it down in your project's README, as you would at work.

## The product

A to-do list API for several users. Each user has their own lists and tasks.

### Requirements

1. **Sign-up and login.** A user signs up with email, name and password, and logs in to get a token (JWT). Email is unique and case-insensitive. Passwords are hashed.
2. **Lists.** A user can create, rename and delete their own to-do lists. List names are unique per user.
3. **Tasks.** In a list, a user can add a task with a title (required, at most 200 characters), an optional description and an optional due date. They can edit it, mark it done, mark it not done again, and delete it.
4. **Viewing tasks.** A user can page through the tasks of a list, filter by done/not done and by "due before a date", and sort by due date or creation time.
5. **Overdue tasks.** A user can get all their overdue tasks across lists: not done, with a due date in the past. "Now" comes from an injected `Clock`.
6. **Ownership.** Nobody can see or change another user's lists or tasks. Asking for someone else's list returns `404`.
7. **Errors.** Every error is a problem-detail response with a `code`, as in the bookstore.

### Non-functional requirements

- Spring Boot 4.1, Java 25, MySQL 9.7 with Flyway migrations; `ddl-auto: validate`.
- Runs locally with `./gradlew :spring:capstone:todo-api:bootRun` and Docker Compose.
- The handbook's conventions: package by feature, records for DTOs, no entities in responses, constructor injection, `@Transactional` services, `open-in-view` off.
- No N+1 queries on the list endpoints.
- All configuration through `@ConfigurationProperties`; no secrets in git.

## Steps

| Session | Goal |
|---|---|
| 1 | Create the Gradle module `spring/capstone/todo-api` with the Spring Boot plugin (copy a `build.gradle.kts` from a Part 2 module and trim it), register it in `settings.gradle.kts`, write `TodoApiApplication`, and make `./gradlew :spring:capstone:todo-api:test` run one empty context test against MySQL in Testcontainers. |
| 2 | Design the schema (users, lists, tasks) and write `V1__create_schema.sql`. Map the entities with their rules (`task.complete()`, `list.rename(…)`). Write unit tests for those rules. |
| 3 | Lists and tasks API without security: controllers, request/response records, validation, problem-detail errors. Integration tests with `MockMvcTester` for the happy paths *and* the edge cases. |
| 4 | Paging, filtering, sorting, and the overdue query with a fixed `Clock` in tests. Count SQL statements in a test to prove there's no N+1. |
| 5 | Security: sign-up, login with JWT, ownership checks. Tests that anonymous requests get 401 and that one user can't touch another user's data. |
| 6 | Production touches: request-id logging, Actuator health, graceful shutdown, a README with how to run it. Review your code against the [handbook](../../docs/handbook/README.md). |

## Done checklist

- [ ] `./gradlew :spring:capstone:todo-api:test` passes, and every requirement above has at least one test, including its edge cases and errors.
- [ ] Tests cover: validation errors (400), missing resources (404), conflicts (409), anonymous access (401), and another user's data (404).
- [ ] There's a test that would fail if a list endpoint ran one query per task.
- [ ] The schema is created only by Flyway; Hibernate only validates it.
- [ ] No entity appears in an API response; no request record is bound to an entity.
- [ ] No secret is committed; the JWT key comes from configuration.
- [ ] The README explains how to run the service and which decisions you made.

**Done?** Ask an AI assistant, or a colleague, to review your code against the handbook pages. Compare their findings with your own review.
