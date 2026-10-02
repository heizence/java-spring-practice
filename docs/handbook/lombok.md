# Lombok

[Lombok](https://projectlombok.org/) generates boilerplate (getters, constructors, loggers) at compile time from annotations. Most Korean and many international Spring codebases use it, so you need to read it fluently. Used carelessly, it also generates bugs. These are common house rules.

## Allowed everywhere

| Annotation | Generates | Typical use |
|---|---|---|
| `@Getter` | a getter per field | entities, configuration classes |
| `@RequiredArgsConstructor` | a constructor for all `final` fields | constructor injection in Spring beans |
| `@Slf4j` | `private static final Logger log = …` | any class that logs |
| `@NoArgsConstructor(access = AccessLevel.PROTECTED)` | a protected no-argument constructor | JPA entities: JPA needs it, other code shouldn't call it |

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private final OrderRepository orders;      // injected through the generated constructor
    private final InventoryRepository inventories;
}
```

## Allowed with care

- **`@Builder`** for classes with many optional parameters, outside entities. Don't use it to skip validation: put the checks in the constructor the builder calls.
- **`@ToString`** only with `exclude` for lazy relationships and secrets. A generated `toString` that touches a lazy collection triggers a query, or a `LazyInitializationException`, just because something logged the object.

## Not allowed on entities

| Annotation | Why not |
|---|---|
| `@Setter` | anyone can change any field and bypass the entity's rules |
| `@Data` | setters, plus `equals`/`hashCode`/`toString` over every field, including lazy relationships: unexpected queries, infinite recursion between parent and child, and broken `HashSet`s when the id changes after saving |
| `@EqualsAndHashCode` | same problem; see [JPA entities](jpa-entities.md) for safe entity equality |
| `@AllArgsConstructor` | creates objects without validation, in a parameter order that silently changes when you reorder fields |

## DTOs: use records instead

Java records give you immutable data carriers with constructors, accessors, `equals`, `hashCode` and `toString`, without Lombok:

```java
public record OrderResponse(long id, String status, long totalAmount, List<OrderLineResponse> lines) {
}
```

## Reading Lombok code

- In IntelliJ, the Lombok plugin is bundled, and **Refactor → Delombok** shows the generated code.
- Lombok runs as an annotation processor during compilation. If generated methods "don't exist" in the IDE, enable annotation processing in the settings.
- Lombok works on Java 25, but each new JDK needs a matching Lombok release. That's one reason some teams prefer records and plain Java.
