# S02 · Configuration and profiles

⏱ 2 sessions · week 7 · 2 exercises

The same jar runs on your laptop, in the CI pipeline, in staging and in production. What differs between those places is *configuration*: database URLs, fees and limits, which mail server to use, and secrets such as API keys. Spring Boot reads configuration from many places, merges it in a fixed order, converts it to typed Java objects and checks it at startup. This module teaches you to use that well: one validated record per feature instead of strings spread through the code, profiles that switch infrastructure per environment, and secrets that never reach git. Done right, a typo in production configuration stops the deployment in its first second, instead of turning into a strange bug a week later.

## Lesson

### 1. Where configuration comes from

Spring Boot collects configuration from many *property sources* into one `Environment`. When two sources set the same property, the later one in this list wins:

1. `application.yml` inside the jar
2. `application-{profile}.yml` inside the jar, for each active profile (section 9)
3. `application.yml` outside the jar: in the working directory, or in its `config/` folder
4. `application-{profile}.yml` outside the jar
5. operating-system environment variables: `BOOKSTORE_SHIPPING_FEE=4000`
6. Java system properties: `java -Dbookstore.shipping.fee=4000 -jar bookstore.jar`
7. command-line arguments: `java -jar bookstore.jar --bookstore.shipping.fee=4000`
8. in tests: `@SpringBootTest(properties = …)`, `@TestPropertySource` and `@DynamicPropertySource`

So the jar carries sensible defaults, and each environment overrides only what differs. Containers and cloud platforms pass configuration as environment variables, which is why item 5 matters most in practice. The full list in the Spring Boot documentation has a few more sources, in the same spirit.

Code can read any property from the `Environment`, but it rarely should. Section 4 shows the better way.

### 2. YAML, and its traps

`application.yml` uses YAML: nesting by indentation, lists with `-`, comments with `#`.

```yaml
bookstore:
  shipping:
    fee: 3000                 # bookstore.shipping.fee
    supported-regions:        # a list
      - seoul
      - busan
```

Spring Boot reads YAML with the YAML 1.1 rules, which turn some innocent-looking values into something else. These are real results from this module's Spring Boot version:

| You write | YAML reads | Why |
|---|---|---|
| `cutoff: 14:30` | the number `870` | `14:30` is a number in base 60: 14 × 60 + 30 |
| `country: no` | `false` | `yes`, `no`, `on` and `off` are booleans |
| `zip: 01234` | the number `668` | a leading `0` means octal |
| `version: 1.10` | `1.1` | it's a decimal number |

Quote such values (`"14:30"`, `"NO"`, `"01234"`), or better, use types that make them unambiguous: `14h30m` for a duration (section 5).

One file can hold several *documents*, separated by `---`. A document with `spring.config.activate.on-profile: prod` only applies when the `prod` profile is active. Separate files per profile (section 9) are easier to read, and this module uses them.

### 3. Relaxed binding

Spring Boot matches property names loosely, so each source can use its own convention. All of these set the same property, the record component `freeThreshold` under the prefix `bookstore.shipping`:

| Where | Spelling |
|---|---|
| YAML and `.properties` files (recommended) | `bookstore.shipping.free-threshold` (kebab case) |
| also accepted in files | `bookstore.shipping.freeThreshold`, `bookstore.shipping.free_threshold` |
| environment variables | `BOOKSTORE_SHIPPING_FREETHRESHOLD` |

The rule for environment variables: upper case, dots become underscores, and dashes disappear. A list is one comma-separated value: `BOOKSTORE_SHIPPING_SUPPORTEDREGIONS=seoul,busan`. Enum values are relaxed too: `seoul`, `Seoul` and `SEOUL` all bind to `Region.SEOUL`.

### 4. Type-safe properties with @ConfigurationProperties

Group the settings of one feature in a record:

```java
@ConfigurationProperties("bookstore.shipping")
@Validated
public record ShippingProperties(
        @PositiveOrZero long freeThreshold,
        @Positive long fee,
        @NotNull Duration cutoffTime,
        @NotEmpty Set<Region> supportedRegions) {
}
```

Spring Boot creates the record through its constructor (*constructor binding*), with every value converted to the component's type, and registers it as a bean. Other beans inject it like any dependency:

```java
@Component
@RequiredArgsConstructor
public class ShippingFeeCalculator {
    private final ShippingProperties properties;
}
```

A properties record must be **registered**, in one of two ways:

- `@EnableConfigurationProperties(ShippingProperties.class)` on a `@Configuration` class of the feature. The registration sits next to the feature, and it can depend on conditions and profiles (S02-02 uses that).
- `@ConfigurationPropertiesScan` on a configuration class registers every `@ConfigurationProperties` class in its package and below.

A few more details:

- **Defaults** come from `@DefaultValue("3000")` on a component. Without one, a missing value is `null` for objects and `0` or `false` for primitives. That's why `fee` is a `long` with `@Positive` (a missing fee becomes 0 and fails), while `cutoffTime` is a `Duration` with `@NotNull`.
- **Nested records** group sub-settings: `record ShippingProperties(…, Express express)` binds `bookstore.shipping.express.fee`.
- **Unknown properties are ignored**, so a misspelled property name (`bookstore.shipping.fe: 4000`) goes unnoticed. The configuration processor (section 7) helps your IDE warn you while you type.

### 5. Property types

Spring Boot converts the text of a property into the component's type:

| Type | Examples | Notes |
|---|---|---|
| `Duration` | `30s`, `500ms`, `14h`, `1d`, `PT14H` | units `ns`, `us`, `ms`, `s`, `m`, `h`, `d`. **A plain number means milliseconds**, unless the component has `@DurationUnit(ChronoUnit.SECONDS)`. |
| `DataSize` | `10MB`, `512KB`, `1GB` | units `B`, `KB`, `MB`, `GB`, `TB`. A plain number means bytes (`@DataSizeUnit` changes that). |
| enums | `seoul`, `SEOUL` | any letter case; an unknown name stops the application |
| `List<T>`, `Set<T>` | a YAML list, or `a,b,c` | elements are converted too |
| `Map<String, T>` | nested keys | a key with dots or special characters needs brackets: `"[api.example.com]": 5` |
| `URI`, `ZoneId`, `Period`, `Charset`, `Locale` | `https://…`, `Asia/Seoul`, `14d` | |

A value that can't be converted, such as `mars` for a `Region`, stops the application at startup with an error that names the property. Typed properties are validated for free: a string in a `@Value` field would have carried the mistake into production.

Now the YAML trap from section 2 bites: `cutoff-time: 14:00` gives the number 840, which a `Duration` reads as 840 milliseconds. Write `14h`.

### 6. Validation at startup

With `@Validated` on the record and `spring-boot-starter-validation` on the classpath, Spring Boot checks the Jakarta Bean Validation constraints (`@NotNull`, `@NotBlank`, `@Positive`, `@Min`, `@Max`, `@NotEmpty`, `@Pattern`, `@Email`, …) as it binds. A broken value stops the application. Started with `--bookstore.shipping.fee=-1`, the bookstore prints:

```
***************************
APPLICATION FAILED TO START
***************************

Description:

Binding to target com.example.bookstore.shipping.ShippingProperties failed:

    Property: bookstore.shipping.fee
    Value: "-1"
    Origin: "bookstore.shipping.fee" from property source "commandLineArgs"
    Reason: must be greater than 0


Action:

Update your application's configuration
```

The report names the property, the bad value, the rule, and even the source the value came from. That's the point: a typo stops the deployment, instead of every order in production being charged a negative fee. For a nested record, put `@Valid` on the component so its own constraints are checked too.

### 7. The configuration processor

Add Spring Boot's annotation processor to the build:

```kotlin
annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
```

At compile time it writes `META-INF/spring-configuration-metadata.json`: every property of every `@ConfigurationProperties` class, with its type, default and description (taken from the `@param` Javadoc of a record). IDEs with Spring support, such as IntelliJ IDEA and VS Code with the Spring Boot extension, read it to complete property names in `application.yml`, show their documentation and flag unknown ones. This module's [`build.gradle.kts`](build.gradle.kts) includes it; after a build, look into `build/classes/java/main/META-INF/`.

### 8. @Value, and why to prefer properties records

`@Value` injects one property into a field or a constructor parameter:

```java
public ShippingFeeCalculator(@Value("${bookstore.shipping.fee:3000}") long fee) { … }   // ":3000" is the default
```

It works, and you'll find it in many codebases, but it scales badly:

- The property names are strings spread through the code. Renaming one means searching for every copy.
- There's no validation, no metadata for the IDE, and only part of relaxed binding: `@Value` must use the kebab-case name.
- A bean with five `@Value` parameters is harder to read and test than one with a record.
- `#{…}` expressions (SpEL) can do logic inside a string, where no compiler or test sees it.

Use `@ConfigurationProperties` for everything a feature needs. `@Value` is fine for a single value in a test or a throwaway prototype.

### 9. Profiles

A *profile* is a name for an environment or a setup, such as `local`, `prod` or `staging`. Profiles do two things:

- **They load files.** With `prod` active, Spring Boot reads `application-prod.yml` on top of `application.yml`.
- **They switch beans.** `@Profile("local")` on a `@Component`, a `@Bean` method or a `@Configuration` class registers it only when `local` is active.

Activate profiles with the property `spring.profiles.active`: on a server usually the environment variable `SPRING_PROFILES_ACTIVE=prod`, locally `--spring.profiles.active=local`, and in tests `@ActiveProfiles("local")`. Several profiles can be active at once: `local,debug`.

```java
@Bean
@Profile("local")                 // only in local
FakeMailSender fakeMailSender() { … }

@Configuration(proxyBeanMethods = false)
@Profile("!local")                // everywhere except local, including when no profile is active
static class ApiMailConfiguration { … }
```

`@Profile` accepts expressions: `!local`, `prod | staging`, `local & debug`. When no profile is active, Spring treats the profile `default` as active; `spring.profiles.default: local` in `application.yml` would make `local` the default instead.

**Profile groups** activate several profiles under one name:

```yaml
spring:
  profiles:
    group:
      prod:
        - prod-db
        - prod-mail
```

`spring.profiles.active` and `spring.profiles.default` belong in `application.yml` (or outside the files altogether): inside a profile-specific file, Spring Boot rejects them with an `InvalidConfigDataPropertyException`, because a profile-specific file can't change which profiles are active after the files have been chosen.

**Choose safe defaults.** Think about what happens when someone forgets to set the profile on a server. If production behavior needs `@Profile("prod")`, a forgotten profile quietly runs the server with development settings, such as a fake mail sender. If only `@Profile("local")` switches on the fakes, a forgotten profile gives production behavior, and a missing secret stops the application loudly. Keep profiles few and environment-shaped, and use them to choose infrastructure, not business rules. A feature switch is a property with `@ConditionalOnProperty` (S01). See [Configuration](../../docs/handbook/configuration.md).

### 10. Secrets

Passwords, API keys and signing keys are *secrets*. The rules:

- **Never commit a secret to git**, not even in a "local" file and not for a minute: git keeps every version forever, and a repository travels further than you think.
- **Pass secrets as environment variables**, or load them from a secret manager (AWS Secrets Manager, HashiCorp Vault) or from files that Kubernetes mounts (`spring.config.import=configtree:/run/secrets/`). Relaxed binding maps the variable `BOOKSTORE_MAIL_API_KEY` to the property `bookstore.mail.api.key`, so no file needs to mention the key at all.
- **Validate them at startup** with `@NotBlank`, so a server without its key stops at once with a clear message.
- **Don't log them.** A record's generated `toString()` prints every component, and configuration objects end up in logs and error messages. Override `toString()` to hide the secret.
- **Local development uses harmless values**, such as the password of the database in your Docker Compose file, or no secret at all, as the `local` profile does in S02-02.

Spring Boot doesn't read `.env` files. Docker Compose and IDE run configurations do, and pass their values as environment variables.

### 11. Checking the effective configuration

When a value isn't what you expect, look at what the application really sees:

- The startup log names the active profiles: `The following 1 profile is active: "prod"`.
- A validation error names the source of the bad value (`origin … from property source …`).
- `environment.getProperty("bookstore.shipping.fee")` in a test or a debugger shows the merged value.
- In S14, Actuator's `/actuator/env` lists every property source and `/actuator/configprops` every properties bean, with secrets masked.

### 12. Testing configuration

`ApplicationContextRunner` (S01) doesn't read `application.yml` by itself. Add `ConfigDataApplicationContextInitializer`, and it reads the files the way `SpringApplication` does, including the files of the active profiles:

```java
private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withInitializer(new ConfigDataApplicationContextInitializer())    // application.yml, application-{profile}.yml
        .withUserConfiguration(ShippingPackage.class);

@Test
void overridesTheFee() {
    runner.withPropertyValues("bookstore.shipping.fee=4000")              // like a command-line argument
            .run(context -> assertThat(context.getBean(ShippingProperties.class).fee()).isEqualTo(4000));
}
```

To simulate environment variables, the tests in this module add a property source of the kind the operating system's variables have: `TestPropertyValues.of("BOOKSTORE_SHIPPING_FEE=4000").applyTo(environment, TestPropertyValues.Type.SYSTEM_ENVIRONMENT)`. They add it before the files are read, as on a real server, because `SPRING_PROFILES_ACTIVE` decides which files that are.

With `@SpringBootTest`, use `properties = "bookstore.shipping.fee=4000"` and `@ActiveProfiles("local")`. Each different combination starts another application context, so keep the combinations few.

## TypeScript/NestJS → Spring

| TypeScript / NestJS | Spring Boot | Watch out for |
|---|---|---|
| `process.env.MAIL_API_KEY` | the environment variable `BOOKSTORE_MAIL_API_KEY`, bound to `bookstore.mail.api.key` | Code reads the bound record, not the environment. |
| `.env` files with `dotenv` | environment variables; `application.yml` for defaults | Spring Boot doesn't read `.env` files. |
| `ConfigModule.forRoot({ envFilePath: ['.env.local', '.env'] })` | `application.yml` plus `application-{profile}.yml` | Profile files override the base file; environment variables override both. |
| `registerAs('shipping', () => ({ fee: parseInt(process.env.SHIPPING_FEE) }))` | `@ConfigurationProperties("bookstore.shipping")` record | Spring converts the types: no `parseInt`, and a bad number stops the application. |
| `validationSchema: Joi.object({...})`, zod schemas | `@Validated` and Jakarta constraints on the record | The check runs at startup, for every environment. |
| `configService.get<number>('shipping.fee')` | `@Value("${bookstore.shipping.fee}")` or `environment.getProperty(...)` | Prefer injecting the record (section 8). |
| `NODE_ENV=production` | `SPRING_PROFILES_ACTIVE=prod` | Several profiles can be active at once. |
| `if (process.env.NODE_ENV !== 'production') providers.push(FakeMailer)` | `@Profile("local")` on the bean | Make the safe behavior the default (section 9). |
| `ms('14h')`, `bytes('10MB')` | `Duration` and `DataSize` components: `14h`, `10MB` | A plain number is milliseconds or bytes. |

## In real projects

- **One `@ConfigurationProperties` record per feature, under your own prefix** (`bookstore.…`, never `spring.…`), registered next to the feature. See [Configuration](../../docs/handbook/configuration.md).
- **Validate at startup** with `@Validated`, and use precise types: `Duration`, `DataSize`, enums, `URI`. A wrong value should stop the deployment.
- **Defaults in `application.yml`, differences in profile files and environment variables.** Keep profiles few, environment-shaped, and safe when forgotten.
- **Secrets only from the environment or a secret manager**, validated with `@NotBlank`, and hidden in `toString()`. See [Security essentials](../../docs/handbook/security.md) and [Logging and observability](../../docs/handbook/logging-and-observability.md).
- **No `@Value` in production code**, except in rare one-off cases.
- **Test configuration with `ApplicationContextRunner`**: properties, environment variables and profiles in milliseconds, including the startup failures. See [Testing Spring applications](../../docs/handbook/spring-testing.md).

## Exercises

Both exercises test their beans in small contexts that read this module's configuration files the way the application does. They don't depend on each other.

### S02-01 · Type-safe configuration properties

**Build** · 30 min · [`ShippingProperties.java`](src/main/java/com/example/bookstore/shipping/ShippingProperties.java) · [`ShippingConfiguration.java`](src/main/java/com/example/bookstore/shipping/ShippingConfiguration.java) · [`ShippingFeeCalculator.java`](src/main/java/com/example/bookstore/shipping/ShippingFeeCalculator.java) · [`application.yml`](src/main/resources/application.yml)

> **BOOK-201 · Shipping rules from the configuration**
>
> In S01-02, the shipping fee (3,000 won) and the free-shipping threshold (20,000 won) were constants in `PriceCalculator`. Operations wants to change them without a release, and different numbers for a holiday promotion. Move the shipping rules into the configuration, together with the same-day cutoff time and the regions the bookstore ships to.
>
> Acceptance criteria:
> - `ShippingProperties` binds `bookstore.shipping.*` from `application.yml`: free from 20,000 won, a fee of 3,000 won, same-day shipping for orders before 14:00 Seoul time, to Seoul, Incheon, Gyeonggi and Busan.
> - Environment variables and command-line arguments override the file, in every spelling that relaxed binding allows.
> - An invalid value stops the application with an error that names the property: a fee of 0 or less, a negative threshold, no cutoff time, no regions, or an unknown region.
> - `ShippingFeeCalculator` uses the bound values: the fee or 0 for a subtotal, an error for a region the bookstore doesn't ship to, and the ship date of an order (the same Seoul day before the cutoff, the next day from the cutoff on).

**Before you start:** lesson sections 1 to 8.

**You'll practice:** a `@ConfigurationProperties` record, `@EnableConfigurationProperties`, `Duration`, enum and set properties, `@Validated` with Jakarta constraints, injecting properties into a bean, `ZonedDateTime`.

**Rules:** in the Javadoc of the three classes. `Region` is finished.

<details>
<summary>Hint 1</summary>

If a test about the cutoff fails although your logic looks right, check how you wrote the time in `application.yml`: section 2 explains why `14:00` isn't 2 p.m.
</details>

<details>
<summary>Hint 2</summary>

For the ship date, convert the instant with `orderedAt.atZone(WAREHOUSE_ZONE)`. `Duration.between(day.atStartOfDay(WAREHOUSE_ZONE), placed)` is the time since midnight, which compares directly with `cutoffTime`.
</details>

<details>
<summary>Hint 3</summary>

The constraints do nothing until something runs them: the record needs `@Validated`, and the project needs `spring-boot-starter-validation`, which this module's build already has.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: What happens if `free-threshold` is missing from every source? Is that what the business wants, and how would you make the property required?

### S02-02 · Profiles and environment-specific beans

**Build** · 30 min · [`MailApiProperties.java`](src/main/java/com/example/bookstore/notification/MailApiProperties.java) · [`MailConfiguration.java`](src/main/java/com/example/bookstore/notification/MailConfiguration.java) · [`application-local.yml`](src/main/resources/application-local.yml) · [`application-prod.yml`](src/main/resources/application-prod.yml)

> **BOOK-202 · Real mail in production, fake mail on laptops**
>
> The bookstore sends mail through a mail provider's HTTP API, authenticated with an API key. Developers must not send real mail from their laptops, and must not need the key at all. Production must send real mail, and must not start without the key. The key may never appear in git.
>
> Acceptance criteria:
> - With the profile `local`, the `MailSender` bean is a `FakeMailSender`, no API settings are needed, and the bookstore's own code logs at DEBUG level.
> - With any other profile, or none, the `MailSender` bean is an `ApiMailSender`. `application-prod.yml` sets the API URL `https://mail-api.example.com/v1/messages`; the key comes from the environment variable `BOOKSTORE_MAIL_API_KEY`.
> - Without a key, or with a blank one, the application doesn't start, and the error names `bookstore.mail.api.key`.
> - No configuration file contains the key, and `MailApiProperties.toString()` hides it.

The tests start the beans the way a server does: they set environment variables such as `SPRING_PROFILES_ACTIVE=prod` and `BOOKSTORE_MAIL_API_KEY`, then read the configuration files.

**Before you start:** lesson sections 9 to 12.

**You'll practice:** `@Profile` with `!`, profile-specific YAML files, a properties record registered only in some profiles, `@NotBlank` for a required secret, relaxed binding of environment variables, keeping a secret out of `toString()`.

**Rules:** in the Javadoc of `MailApiProperties` and `MailConfiguration`, and the TODOs in the two YAML files. `FakeMailSender` and `ApiMailSender` are finished.

**Try it:** run `./gradlew :spring:s02-configuration:bootRun` without a profile, and read why the application doesn't start. Then run it with `--args='--spring.profiles.active=local'`, and once more with the profile `prod` and an invented key: `BOOKSTORE_MAIL_API_KEY=demo ./gradlew :spring:s02-configuration:bootRun --args='--spring.profiles.active=prod'`.

<details>
<summary>Hint 1</summary>

`@Profile` and `@EnableConfigurationProperties` must apply together: the API settings may only exist where the `ApiMailSender` does. A `static` nested `@Configuration` class inside `MailConfiguration` can carry both annotations.
</details>

<details>
<summary>Hint 2</summary>

A record's `toString()` can be overridden like any method: `return "MailApiProperties[url=%s, key=******]".formatted(url);`.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: What would happen on a production server where someone forgot `SPRING_PROFILES_ACTIVE`, with your configuration and with `@Profile("prod")` instead of `@Profile("!local")`? Where else could the API key leak?

## Review questions

1. `application.yml` sets `bookstore.shipping.fee: 3000`, the server has `BOOKSTORE_SHIPPING_FEE=3500`, and the application starts with `--bookstore.shipping.fee=4000`. Which fee does it use, and why?
2. Which environment variable sets `bookstore.shipping.free-threshold`?
3. `application.yml` says `cutoff-time: 14:00`, and every order ships a day late. What happened?
4. Give three reasons to prefer a `@ConfigurationProperties` record over `@Value`.
5. A record has `@NotBlank String key` but an empty key starts the application without complaint. What's missing?
6. What's the difference between `@EnableConfigurationProperties(X.class)` and `@ConfigurationPropertiesScan`?
7. What does `@Profile("!local")` mean when no profile is active?
8. Why does `spring.profiles.active` in `application-prod.yml` fail?
9. Where should the production database password come from, and where must it never be?
10. Your properties record holds an API key. Name two places where the key could leak, and how to prevent it.
11. How do you test that a missing property stops the application?

<details>
<summary>Answers</summary>

1. 4000. Command-line arguments come after environment variables, and both come after `application.yml`, in the order of property sources; the later source wins.
2. `BOOKSTORE_SHIPPING_FREETHRESHOLD`: upper case, dots become underscores, dashes disappear.
3. YAML 1.1 reads `14:00` as a number in base 60, 840. A `Duration` without a unit is in milliseconds, so the cutoff is 840 ms after midnight. Write `14h`, or quote the value and use a type that parses times.
4. One place per feature instead of strings spread through the code; type conversion and validation at startup; IDE completion and documentation from the metadata; full relaxed binding; constructors that are easy to call in tests. Any three.
5. `@Validated` on the record (or `spring-boot-starter-validation` on the classpath). Without them, the constraint annotations are never checked.
6. `@EnableConfigurationProperties(X.class)` registers the named classes, wherever that annotation is processed, so it can live in a feature's configuration and follow its conditions and profiles. `@ConfigurationPropertiesScan` registers every `@ConfigurationProperties` class in a package and below, whatever needs them.
7. It matches: with no active profile, `local` isn't active. That's why `!local` makes the real behavior the default.
8. Profiles decide which files Spring Boot reads, so a profile-specific file can't change the active profiles afterwards. Spring Boot rejects it with `InvalidConfigDataPropertyException`. Set it in `application.yml`, or better, outside the jar.
9. From the server's environment (an environment variable) or a secret manager. Never from a file in git, including a local one, and never in a log.
10. In logs (a generated `toString()`, a debug statement, an error message) and in the repository (a committed configuration file); also in Actuator endpoints and crash reports. Override `toString()`, never commit secret values, keep Actuator's masking on, and pass secrets through the environment.
11. Start an `ApplicationContextRunner` without the property (or with an invalid value), then assert that the context failed (`assertThat(context).hasFailed()`) and that the failure's message names the property and the rule.
</details>

## Further reading

- Spring Boot reference: [Externalized configuration](https://docs.spring.io/spring-boot/reference/features/external-config.html), including type-safe configuration properties, relaxed binding and validation; [Profiles](https://docs.spring.io/spring-boot/reference/features/profiles.html)
- [Generating your own metadata with the annotation processor](https://docs.spring.io/spring-boot/specification/configuration-metadata/annotation-processor.html)
- [The Twelve-Factor App: Config](https://12factor.net/config): why configuration belongs in the environment
- [Jakarta Bean Validation constraints](https://jakarta.ee/specifications/bean-validation/3.1/apidocs/jakarta/validation/constraints/package-summary): every built-in constraint annotation
- The YAML 1.1 rules for [integers](https://yaml.org/type/int.html) and [booleans](https://yaml.org/type/bool.html): where `14:00`, `01234` and `no` get their surprising meanings
