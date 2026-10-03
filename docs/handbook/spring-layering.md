# Spring layering

Most Spring Boot services are organized the same way. Knowing the pattern lets you find your way around any codebase in minutes, and it tells you where new code belongs.

## Package by feature, layers inside

```
com.example.bookstore
├── order/
│   ├── OrderController.java      HTTP: requests in, responses out
│   ├── OrderService.java         use cases and transactions
│   ├── OrderRepository.java      database access
│   ├── Order.java, OrderLine.java, OrderStatus.java   the domain model (entities)
│   └── CreateOrderRequest.java, OrderResponse.java    request and response records
├── book/
└── common/                       code shared by several features
```

Group code by **feature** first: everything about orders lives in `order/`. Changing how ordering works usually touches one folder. Many older projects group by layer instead (`controller/`, `service/`, `repository/` at the top), which scatters one feature over the whole tree. You'll see both styles at work; prefer feature folders in new code.

## What each layer does

| Layer | Does | Doesn't |
|---|---|---|
| **Controller** (`@RestController`) | Maps a URL to a method, validates the request (`@Valid`), calls **one** service method, turns the result into an HTTP response | Contain business rules, call repositories, open transactions |
| **Service** (`@Service`) | Implements a use case ("place an order"), defines the transaction (`@Transactional`), coordinates entities and repositories, returns response records | Know about HTTP (no `HttpServletRequest`, no status codes) |
| **Repository** (`JpaRepository`, or a class with `JdbcClient`) | Loads and saves data | Contain business rules |
| **Entity** (`@Entity`) | Holds data *and* the rules that protect it: `order.cancel()`, `inventory.decrease(3)` | Know about services, repositories or HTTP |

Dependencies point one way: controller → service → repository, and everyone may use entities. A controller never calls a repository directly, and a repository never calls a service.

## Rules worth following

- **Put rules in the entity, not in the service.** `inventory.decrease(quantity)` checks the stock and throws if it's too low. A service that does `inventory.setQuantity(inventory.getQuantity() - quantity)` spreads the rule across every caller, and one of them will forget the check.
- **Never return entities from controllers.** Use response records:
  - Entities may expose fields the client must not see (`passwordHash`).
  - Lazy relationships fail or trigger extra queries during JSON serialization.
  - Bidirectional relationships can recurse forever.
  - Every entity change becomes an API change, breaking clients.
- **Never bind requests directly to entities.** A client could set fields you never meant to accept, such as `role` (a "mass assignment" vulnerability). Accept request records with exactly the allowed fields.
- **Map in the service, at the transaction boundary.** With `open-in-view` off (as it should be), lazy relationships only load inside the transaction. The service builds the response record while everything is still loadable.
- **Constructor injection only.** Declare dependencies as `private final` fields and let Lombok's `@RequiredArgsConstructor` write the constructor. The class can't exist without its dependencies, tests can pass fakes through the constructor, and the fields can't change. Field injection (`@Autowired` on a field) hides dependencies and breaks all three.
- **Keep beans stateless.** A Spring bean is a singleton shared by every request thread. Fields hold dependencies and configuration only, never per-request data. See [Concurrency](concurrency.md).
- **Watch for circular dependencies.** Spring Boot refuses `A → B → A` by default. A cycle usually means one class does two jobs; split it.

## Example

```java
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
class OrderController {

    private final OrderService orderService;

    @PostMapping
    ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request,
                                         @AuthenticationPrincipal Jwt jwt) {
        OrderResponse order = orderService.placeOrder(memberId(jwt), request);
        return ResponseEntity.created(URI.create("/api/orders/" + order.id())).body(order);
    }
}

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orders;
    private final InventoryRepository inventories;

    @Transactional
    public OrderResponse placeOrder(long memberId, CreateOrderRequest request) {
        // load entities, call their methods (they enforce the rules), save, map to a response record
    }
}
```

## Stereotype annotations

| Annotation | Use it on |
|---|---|
| `@RestController` | HTTP endpoints that return data (JSON) |
| `@Service` | use-case classes |
| `@Repository` | hand-written repository classes. It also translates SQL exceptions into Spring's `DataAccessException`. Spring Data interfaces don't need it. |
| `@Component` | any other bean |
| `@Configuration` + `@Bean` | creating beans you can't annotate, such as a `Clock` or a third-party client |

They all make a class a bean; the specific names document the role and enable small extras.
