# REST API design

An API is a contract with every client: web front ends, mobile apps, other services. Changing it is expensive, so it pays to design it consistently from the start. These conventions match what most companies expect.

## Resources and methods

- **Plural nouns for collections, ids for items:** `/api/books`, `/api/books/42`, `/api/members/me/orders`.
- **Methods carry the action**, not the path: `POST /api/orders`, not `/api/createOrder`.
- **Lowercase, hyphenated paths:** `/api/order-lines`. JSON properties use `camelCase`.
- For actions that aren't simple create/update/delete, use a sub-resource that names the result: `POST /api/orders/42/cancellation`, or `POST /api/orders/42/cancel` if your team prefers verbs for actions.

| Method | Meaning | Safe | Idempotent | Typical success status |
|---|---|---|---|---|
| `GET` | read | yes | yes | `200 OK` |
| `POST` | create, or start an action | no | no | `201 Created` + `Location` header |
| `PUT` | replace the whole resource | no | yes | `200 OK` (or `204`) |
| `PATCH` | change some fields | no | not necessarily | `200 OK` |
| `DELETE` | remove | no | yes | `204 No Content` |

*Safe* means the request changes nothing. *Idempotent* means repeating the request has the same effect as sending it once, which matters because clients retry on timeouts. `POST` isn't idempotent, so retried payment requests need an **idempotency key**: a unique id the client sends with the request. The server stores it and answers a repeat with the original result instead of charging twice (S08-06).

## Status codes

| Status | When |
|---|---|
| `400 Bad Request` | The request is malformed or invalid: missing field, wrong format, failed validation |
| `401 Unauthorized` | Not authenticated: no credentials, or invalid ones |
| `403 Forbidden` | Authenticated, but not allowed to do this |
| `404 Not Found` | The resource doesn't exist, or the caller isn't allowed to know it does |
| `409 Conflict` | The request conflicts with the current state: out of stock, duplicate email, invalid status transition |
| `422 Unprocessable Content` | Some teams use it for business-rule violations instead of 409. Pick one convention and stick to it. |
| `500 Internal Server Error` | A bug. Never send a stack trace to the client. |

## Errors: problem details

Spring supports RFC 9457 *problem details* out of the box (`ProblemDetail`). Add a stable, machine-readable `code`, so clients can react without parsing English messages:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "The request has 2 invalid fields.",
  "instance": "/api/orders",
  "code": "INVALID_INPUT",
  "errors": [
    { "field": "lines[0].quantity", "message": "must be greater than 0" },
    { "field": "couponCode", "message": "size must be between 0 and 30" }
  ]
}
```

- Produce every error response in **one place**, a `@RestControllerAdvice`, so the format is identical everywhere.
- `detail` is for humans: specific, with the bad value where it helps. It never contains stack traces, SQL or secrets.

## Validation

- Validate request records with Bean Validation (`@NotBlank`, `@Positive`, `@Size`, `@Email`, `@Valid` on nested objects) and `@Valid` on the controller parameter.
- Validation checks the *shape* of input. Business rules that need the database ("is this coupon still available?") belong in the service and the entities.

## Lists, paging and filtering

- **Always page lists that can grow:** `?page=0&size=20`, and cap the size (for example at 100) so nobody can request a million rows.
- **Sorting:** `?sort=price,desc`. Accept only a known list of sortable properties.
- **Filtering:** query parameters named after the property: `?categoryId=3&minPrice=10000`.
- Return a page envelope: the items plus `page`, `size`, `totalElements` and `totalPages`.

## JSON

- Times are ISO-8601 strings in UTC (`"2026-09-14T08:15:00Z"`). Dates without time are `"2026-09-14"`.
- Enums are their names (`"PAID"`).
- Money in this course is a whole number of won. With currencies that have cents, send a string or a number with a fixed scale, never a float.
- Tolerate unknown properties in requests (Spring's default), so old clients keep working when you add fields.

## Versioning

You'll need to change an API without breaking existing clients. Additive changes, such as new optional fields or new endpoints, need no new version. Breaking changes do. Spring Framework 7 has built-in API versioning: you choose where the version lives (a request header, a path segment, a query parameter or the media type) and declare it on mappings with `@GetMapping(path = "/api/books", version = "2")`.

## Documentation

Generate an OpenAPI description from the code (springdoc-openapi) and keep it in sync automatically. Clients and front-end developers read it, and tools generate clients from it.
