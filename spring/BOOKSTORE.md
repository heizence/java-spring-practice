# The bookstore application

Every Part 2 module works on the same application: an online bookstore. Members browse books, order them, pay from a wallet, use coupons and write reviews. Each module folder contains a complete copy of the application at the point where that module starts (a *checkpoint*), so a mistake in an earlier module never blocks you.

This page describes the domain, the database, the seed data your tests can rely on, and the conventions the code follows.

## The domain

```
Member ──< Order ──< OrderLine >── Book >── Category
  │          │                      │
  │          └──< Payment           ├── Inventory (stock, 1:1)
  │                                 └──< Review >── Member
  ├── Wallet ──< WalletTransaction
  └──< CouponIssue >── Coupon
```

`A ──< B` means "one A has many B". For example, one order has many order lines.

| Entity | Table | What it is |
|---|---|---|
| `Member` | `members` | A customer (role `USER`) or an administrator (`ADMIN`). Email is unique. |
| `Category` | `categories` | A book category, such as Programming or Fiction. |
| `Book` | `books` | A book with ISBN, title, author, category and price. |
| `Inventory` | `inventories` | How many copies of a book are in stock. Separate from `books`, so locking stock doesn't lock the book. |
| `Order` | `orders` | A member's order: status `PENDING` → `PAID` → `SHIPPED`, or `CANCELLED`. |
| `OrderLine` | `order_lines` | One book in an order: quantity and the unit price at the time of ordering. |
| `Payment` | `payments` | A payment for an order. Its `idempotency_key` is unique, so the same payment request can't be processed twice. |
| `Wallet` | `wallets` | A member's prepaid balance, one per member. |
| `WalletTransaction` | `wallet_transactions` | One change to a wallet balance: `CHARGE`, `PAYMENT`, `REFUND`, `TRANSFER_IN`, `TRANSFER_OUT`. |
| `Coupon` | `coupons` | A discount code with a limited quantity and a validity period. |
| `CouponIssue` | `coupon_issues` | A coupon given to a member. Each member can get each coupon at most once. |
| `Review` | `reviews` | A member's rating (1–5) and text for a book. |

## Database conventions

- MySQL 9.7 with InnoDB and `utf8mb4`.
- Plural `snake_case` table names; `BIGINT AUTO_INCREMENT` ids.
- **Money is a `BIGINT` number of won.** The Korean won has no smaller unit, so whole numbers are exact. In Java: `long`.
- **Timestamps are `DATETIME(6)` in UTC.** In Java: `Instant`. Hibernate converts with `hibernate.jdbc.time_zone=UTC`.
- Enums are stored as their names (`VARCHAR`), never as numbers, so reordering an enum can't corrupt data.
- Columns named `version` are used for optimistic locking (S08).
- The schema is created by Flyway migrations in `src/main/resources/db/migration`: `V1__create_schema.sql` and `V2__seed_data.sql`. Modules that change the schema add `V3__…` and later files. Hibernate only *validates* the schema (`ddl-auto: validate`); it never changes it.

## Seed data your tests can rely on

`V2__seed_data.sql` loads the same data every time:

| Data | Facts |
|---|---|
| Categories | 6: Programming (1), Databases (2), Software Architecture (3), Fiction (4), Science (5), Self-Help (6) |
| Books | 60. Book 1 is *Effective Java* by Joshua Bloch, 36,000 won, 10 in stock. Books 59 (*Range*) and 60 (*Ultralearning*) are out of stock. Books 41 and 42 have Korean titles (*진달래꽃*, *운수 좋은 날*). |
| Members | 20, all with the password `bookstore123`. Member 1 is Minji Kim (`minji@example.com`). Member 20 is the administrator (`admin@example.com`, role `ADMIN`). |
| Wallets | One per member. Minji's balance is 100,000 won; the admin's is 0. Each wallet's history adds up to its balance: a `CHARGE` when the member joined, then a `PAYMENT` for each approved payment. |
| Coupons | `WELCOME5000` (1,000 total, issued to members 1–15; members 2 and 3 used theirs) · `FLASH10000` (only 10, none issued yet, valid in October 2026) · `AUTUMN3000` (500 total, issued to members 1–10; Minji used hers) · `EXPIRED2000` (valid in July and August 2026 only; members 2–6 received it and never used it) |
| Orders | 200 orders with 357 lines from August and September 2026: 108 `PAID`, 48 `SHIPPED`, 33 `PENDING`, 11 `CANCELLED`. Minji has 14 of them. |
| Payments | One `APPROVED` payment for every paid or shipped order (156). Its amount is the order's total, minus the discount for the three orders paid with a coupon. |
| Reviews | 300, at most one per member and book. Book 1 has 15. |

Tests run with the application's clock stopped at **2026-10-15T00:00:00Z**, so "now" is the same whenever you run them: `WELCOME5000`, `FLASH10000` and `AUTUMN3000` are valid, `EXPIRED2000` has expired, and every seeded order lies in the past. A test can move the clock.

## Package layout

The code is organized **by feature**, with the usual layers inside each feature:

```
com.example.bookstore
├── BookstoreApplication.java
├── common/          shared building blocks: BaseTimeEntity, error handling, configuration
├── book/            Book, Category, BookRepository, BookService, BookController, request/response records
├── inventory/
├── member/
├── order/
├── payment/
├── wallet/
├── coupon/
└── review/
```

- **Controllers** handle HTTP only: read the request, call one service method, return a response.
- **Services** hold the use cases and define the transaction boundaries (`@Transactional`).
- **Repositories** talk to the database.
- **Entities** protect their own rules with methods such as `inventory.decrease(quantity)` or `order.cancel()`. They have no public setters.
- **Requests and responses are records** (`CreateOrderRequest`, `OrderResponse`). Controllers never return entities.

The handbook explains each of these rules: [Spring layering](../docs/handbook/spring-layering.md).

## API conventions

| Rule | Example |
|---|---|
| Paths start with `/api` and use plural nouns | `GET /api/books`, `GET /api/books/1`, `POST /api/orders` |
| `200 OK` for reads and updates, `201 Created` with a `Location` header for creates, `204 No Content` for deletes | `Location: /api/orders/201` |
| Invalid input is `400 Bad Request`; a broken business rule (out of stock, duplicate) is `409 Conflict`; a missing resource is `404 Not Found` | |
| Errors use RFC 9457 *problem details*, with a machine-readable `code` | see below |
| JSON property names are `camelCase`; times are ISO-8601 strings in UTC | `"orderedAt": "2026-09-14T08:15:00Z"` |
| Lists are paged with `page` (from 0) and `size` | `GET /api/books?page=0&size=20&sort=price,desc` |

A page of a list:

```json
{
  "content": [
    { "id": 1, "isbn": "9791115881394", "title": "Effective Java", "author": "Joshua Bloch", "category": "Programming", "price": 36000, "publishedDate": "2018-10-18" }
  ],
  "page": { "size": 20, "number": 0, "totalElements": 60, "totalPages": 3 }
}
```

An error response:

```json
{
  "title": "Conflict",
  "status": 409,
  "detail": "Only 3 copies of book 7 are left, but 5 were requested.",
  "instance": "/api/orders",
  "code": "OUT_OF_STOCK"
}
```

There is no `type` property: RFC 9457 reads a missing `type` as `about:blank`, which means "the status code says it all". When the input is invalid, `errors` lists every invalid value:

```json
{
  "title": "Bad Request",
  "status": 400,
  "detail": "Invalid request content.",
  "instance": "/api/orders",
  "code": "INVALID_INPUT",
  "errors": [
    { "field": "lines[0].quantity", "message": "must be greater than or equal to 1" }
  ]
}
```

## Running a module

You need Docker Desktop running.

```bash
./gradlew :spring:s06-spring-data-jpa:bootRun
```

Spring Boot's Docker Compose support reads the module's `compose.yaml`, starts MySQL, and connects the application to it. Then try `http://localhost:8080/api/books`. When you stop the application, the database container stops too. Modules without a database (S01–S04) start without Docker.

The tests start their own MySQL container with [Testcontainers](https://testcontainers.com/), so they also need Docker, but no other setup.
