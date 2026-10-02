# Configuration

Configuration is everything that changes between environments (your laptop, test, production) or that operations may tune without a code change: URLs, limits, timeouts, feature switches, credentials.

## Where values come from

Spring Boot merges many sources. Later ones in this list win:

1. `application.yml` inside the jar
2. `application-<profile>.yml` for each active profile
3. environment variables (`BOOKSTORE_SHIPPING_FREE_THRESHOLD=50000`)
4. Java system properties and command-line arguments (`--bookstore.shipping.free-threshold=50000`)

So the jar carries sensible defaults, and each environment overrides only what differs. Environment variables are the usual way containers and cloud platforms pass configuration.

**Relaxed binding** connects the spellings: `bookstore.shipping.free-threshold` in YAML, `BOOKSTORE_SHIPPING_FREE_THRESHOLD` as an environment variable and `freeThreshold` in Java all mean the same property. Use kebab-case in YAML.

## Type-safe properties

Bind a group of related settings to a record:

```java
@ConfigurationProperties(prefix = "bookstore.shipping")
@Validated
public record ShippingProperties(
        @PositiveOrZero long freeThreshold,
        @Positive long fee,
        @NotNull Duration cutoffTime) {
}
```

```yaml
bookstore:
  shipping:
    free-threshold: 50000
    fee: 3000
    cutoff-time: 14h
```

Register it with `@EnableConfigurationProperties(ShippingProperties.class)` or `@ConfigurationPropertiesScan`, and inject `ShippingProperties` like any bean.

- **Prefer `@ConfigurationProperties` over `@Value`.** You get one place per feature, types (`Duration`, `DataSize`, enums, lists), validation at startup, and IDE completion once `spring-boot-configuration-processor` is on the annotation-processor path.
- **Validate at startup** with `@Validated`. A typo in production should stop the application immediately, not cause a strange bug a week later.
- **Use the right types:** `Duration` (`30s`, `5m`), `DataSize` (`10MB`), enums and lists.
- **Name your own properties under your own prefix** (`bookstore.…`), never under `spring.…`.

## Profiles

A profile activates a group of settings and beans: `local`, `test`, `prod`.

- Activate with `spring.profiles.active=prod` (usually an environment variable: `SPRING_PROFILES_ACTIVE=prod`).
- `application-prod.yml` holds the production overrides.
- `@Profile("local")` on a bean registers it only in that profile. A fake mail sender for local development is a typical example.
- **Keep profiles few and environment-shaped.** A profile per feature switch quickly becomes unmanageable; use a property (`bookstore.recommendations.enabled: true`) with `@ConditionalOnProperty` instead.
- Production code shouldn't behave differently because of a profile in surprising ways. Profiles choose *infrastructure* (which database, which mail server), not business rules.

## Secrets

- **Never commit secrets** (passwords, API keys, signing keys) to git, not even in a "local" file. Git keeps history forever.
- Pass them as environment variables, or load them from a secret manager (AWS Secrets Manager, Vault) at startup.
- Local development uses harmless placeholder values, such as the Docker Compose database password.
- Don't log configuration that contains secrets.

## Checking what's active

- `/actuator/env` and `/actuator/configprops` (in secure, non-production setups) show the effective values and where each came from.
- Starting with `--debug` prints the auto-configuration report: which beans Spring Boot created, and why.
