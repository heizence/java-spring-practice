# S01 · Spring Boot fundamentals, IoC and DI

⏱ 5 sessions + 1 optional · week 7 · 6 exercises

You've used Spring Boot at work, and in J09-04 you built a small dependency injection container yourself. This module connects the two. You learn what really happens when a Spring Boot application starts: how Spring finds your classes, creates them in the right order and hands each one its dependencies, how it chooses between two implementations of an interface, how long an object lives, and how Spring Boot configures a whole application from the libraries on your classpath. These are the mechanics behind every startup error you'll ever read ("No qualifying bean of type…", "expected single matching bean but found 2…"), and they decide whether your code can be tested without starting the whole application. The bookstore has no database and no web layer yet: everything here runs in plain Java objects that Spring wires together.

## Lesson

### 1. From your mini container to Spring

In J09-04 you wrote a container that found component classes, picked their constructors, created every object once in dependency order and handed each one its dependencies. The idea has a name: *inversion of control* (IoC). Your code no longer creates the objects it needs with `new`; a container creates them and passes them in. Passing dependencies in from outside is called *dependency injection* (DI). An object that the container creates and manages is a *bean*.

Spring's container does the same work, with the same rules, and adds many features. Most of what you built has a direct counterpart:

| Your `MiniContainer` (J09-04) | Spring |
|---|---|
| `MiniContainer.scan("com.example")` finds component classes | `@ComponentScan`, which `@SpringBootApplication` includes |
| `@Component`, and `@Service` as a meta-annotation | `@Component`, and the stereotypes `@Service`, `@Repository`, `@Controller` and `@Configuration` |
| the only public constructor, or the one with `@Inject` | the only constructor, or the one with `@Autowired` |
| resolve each parameter by type; `@Primary` breaks a tie | the same, plus `@Qualifier`, parameter names and `@Fallback` (section 7) |
| `NoSuchBeanException`, `NoUniqueBeanException` | `NoSuchBeanDefinitionException`, `NoUniqueBeanDefinitionException` |
| `CircularDependencyException` | `BeanCurrentlyInCreationException`: the application doesn't start |
| every bean is a singleton, created when the container starts | singletons by default, created at startup; other *scopes* exist (section 8) |
| `@Init` runs after the constructor | `@PostConstruct`, and `@PreDestroy` when the container closes (section 9) |
| `creationOrder()`: dependencies first | the same order; beans are destroyed in reverse order |
| `@Timed` proxies | AOP proxies for `@Transactional`, `@Cacheable`, `@Async` (S03) |
| `getBean(type)` | `context.getBean(type)`, which application code should rarely call |

On top of that, Spring has *bean definitions* (a recipe for each bean, read before any object exists), an `Environment` that holds configuration properties and profiles (S02), *conditions* that decide whether a bean exists at all (section 11), events, and proxies (S03). Spring Boot adds *auto-configuration*: beans that configure themselves from the libraries on the classpath (section 10).

### 2. The ApplicationContext and beans

The container is an `ApplicationContext`. When it starts (Spring calls this a *refresh*), it works in two phases:

1. **Read the bean definitions.** It scans packages for component classes, reads `@Configuration` classes and their `@Bean` methods, and loads Spring Boot's auto-configurations. A bean definition records the bean's class, its *name*, its scope, how to create it and what it depends on. No object exists yet.
2. **Create the beans.** It creates every singleton bean, each one after the beans it depends on, injects the dependencies and calls the initialization callbacks.

Every bean has a name. A scanned class gets its class name with a lower-case first letter (`CheckoutService` → `checkoutService`); a `@Bean` method gets the method's name. You can choose another one: `@Component("goldDiscount")`.

Because singletons are created at startup, a wiring mistake (a missing bean, two candidates, a cycle) stops the application before it serves a single request. That's a feature: the error shows up in your test run or your deployment, not at 3 a.m. Spring Boot prints such failures in a block that starts with `APPLICATION FAILED TO START`, followed by a *Description* and an *Action*. Read that block first; the stack trace below it is usually longer than it is useful.

Your own code almost never calls `context.getBean(...)`. Classes declare what they need in their constructors, and the container provides it. Calling `getBean` from business code (the *service locator* pattern) hides dependencies again, which is what DI was meant to fix.

### 3. A Spring Boot application

This is a complete Spring Boot application:

```java
package com.example.bookstore;

@SpringBootApplication
public class BookstoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookstoreApplication.class, args);
    }
}
```

**`@SpringBootApplication`** combines three annotations:

- `@SpringBootConfiguration`: this class is a `@Configuration` class, the starting point of the application's configuration.
- `@EnableAutoConfiguration`: load Spring Boot's auto-configurations (section 10).
- `@ComponentScan`: find the component classes **in this class's package and its sub-packages**. That's why the application class sits in the root package, `com.example.bookstore`, above every feature package. A class in `com.example.other` isn't found, and a class in `com.example` either.

**`SpringApplication.run(BookstoreApplication.class, args)`** does the following:

1. It prepares the `Environment`: the configuration properties from `application.yml`, environment variables and the command-line arguments. An argument such as `--spring.application.name=demo` becomes a property, and it overrides `application.yml`. That only works if you pass `args` on.
2. It prints the banner and the first log lines.
3. It creates the `ApplicationContext`. Its kind depends on the *web application type*: a servlet web server (with Spring MVC on the classpath), a reactive one (with WebFlux), or none.
4. It refreshes the context: bean definitions, then beans (section 2).
5. It logs `Started BookstoreApplication in 0.4 seconds`.
6. It calls every `ApplicationRunner` and `CommandLineRunner` bean (section 9).
7. It returns the running context.

A web application keeps running because the web server's threads keep the JVM alive. An application without a web server exits when `main` returns and no other thread is running. In both cases, Spring registers a *shutdown hook*: when the JVM stops normally, the hook closes the context, which destroys the beans.

**`application.yml`** in `src/main/resources` holds the configuration. Spring Boot reads it at startup (S02 covers it in depth):

```yaml
spring:
  application:
    name: bookstore              # appears in every log line: "--- [bookstore]"
  main:
    web-application-type: none   # don't start a web server
```

Properties under `spring.main` configure `SpringApplication` itself. Spring Boot picks the web application type from the classpath, so `none` would also be the result without this line in this module. Setting it documents the intent, and it stops a web server from starting by accident when a web library arrives later as a dependency of another library.

### 4. Declaring beans: stereotypes and @Bean methods

There are two ways to make something a bean.

**Annotate the class** with a *stereotype*, an annotation that marks a component and says which role it plays. Component scanning finds it.

| Annotation | Use it on |
|---|---|
| `@Service` | use-case classes, such as `CheckoutService` |
| `@Repository` | classes that load and store data. Spring also translates their database exceptions into its own `DataAccessException` hierarchy. |
| `@Controller`, `@RestController` | web endpoints (S04) |
| `@Component` | any other bean |
| `@Configuration` | classes with `@Bean` methods |

They all make the class a bean. The specific name documents the role and switches on small extras.

**Write a `@Bean` method** in a `@Configuration` class. Use it for classes you can't annotate, because they come from the JDK or a library, and for objects whose creation needs some logic:

```java
@Configuration(proxyBeanMethods = false)
public class ClockConfiguration {

    @Bean                              // the bean's name is the method's name: "clock"
    public Clock clock() {
        return Clock.systemUTC();
    }
}
```

A `@Bean` method's parameters are injected like constructor parameters: `@Bean EmailNotifier emailNotifier(MailSender mailSender)` gets the `MailSender` bean.

**`proxyBeanMethods`.** By default, Spring wraps a `@Configuration` class in a generated subclass (a *CGLIB proxy*). Then, when one `@Bean` method calls another, the call returns the existing singleton instead of creating a second object. With `proxyBeanMethods = false`, there's no proxy: startup is a little faster, and a call to another `@Bean` method would create a new object. So take other beans as method parameters instead of calling their methods, and always write `proxyBeanMethods = false`. Spring Boot's own configuration classes do the same.

### 5. Constructor injection

Give a bean its dependencies through its constructor, and store them in `final` fields:

```java
@Service
@RequiredArgsConstructor           // Lombok writes the constructor for the final fields
public class CheckoutService {

    private final BookCatalog bookCatalog;
    private final PriceCalculator priceCalculator;
    private final Clock clock;
}
```

Lombok's `@RequiredArgsConstructor` generates `CheckoutService(BookCatalog, PriceCalculator, Clock)`, with the parameters in the order of the fields (see [Lombok](../../docs/handbook/lombok.md)). When a class has exactly one constructor, Spring uses it without `@Autowired`.

Why constructor injection, and not `@Autowired` on fields?

- **The object is complete from the start.** It can't exist without its dependencies, and `final` fields can't change afterwards.
- **Tests are plain Java.** A unit test writes `new CheckoutService(catalog, calculator, Clock.fixed(...))`. With field injection, it needs Spring or reflection to fill the fields.
- **Dependencies are visible.** A constructor with eight parameters tells you that the class does too much. Eight `@Autowired` fields are easy to overlook.
- **Cycles fail fast.** With constructors, `A → B → A` can't be built, and the application doesn't start. Since Spring Boot 2.6, cycles are refused by default with field injection too (`spring.main.allow-circular-references`). A cycle usually means that one class does two jobs: split it.

Most beans should be **stateless**: a singleton serves every request thread at the same time, so its fields hold dependencies and settings, never the data of one request (see [Spring layering](../../docs/handbook/spring-layering.md)).

### 6. Testing the wiring with ApplicationContextRunner

A `@SpringBootTest` starts the whole application. To test how a few beans are wired, start a small context with only those beans. Spring Boot's `ApplicationContextRunner` does that in milliseconds:

```java
private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withUserConfiguration(CheckoutPackages.class)       // configuration classes, as your application has them
        .withBean(BookCatalog.class)                          // single beans
        .withBean(TestClock.class, TestClock::new, definition -> definition.setPrimary(true));

@Test
void checksOut() {
    runner.withPropertyValues("bookstore.notifications.channel=email")   // configuration properties
            .run(context -> {                                           // a new context, closed afterwards
                assertThat(context).hasSingleBean(CheckoutService.class);
                ...
            });
}
```

Each `run` starts a fresh context, hands it to your lambda and closes it afterwards, which calls the `@PreDestroy` methods. If the context fails to start, `run` doesn't throw: `assertThat(context).hasFailed()` and `context.getStartupFailure()` let you test startup errors too. The runner doesn't read `application.yml`; you pass properties with `withPropertyValues`.

The tests in this module start such contexts. To find the beans the way your application does, they scan one package with a small class annotated `@ComponentScan(basePackageClasses = CheckoutService.class)`. They also use the helpers in `ContextAssertions` instead of `assertThat(context).hasSingleBean(...)`: they check the same things, with failure messages short enough for `./score` to show in full.

### 7. Several candidates for one type

When a constructor asks for a `DiscountPolicy` and four beans implement it, Spring has to choose. Without help, the application doesn't start:

```
***************************
APPLICATION FAILED TO START
***************************

Description:

Parameter 0 of constructor in com.example.bookstore.discount.DiscountService required a single bean, but 4 were found:
	- basicDiscount: defined in file [.../BasicDiscount.class]
	- goldDiscount: defined in file [.../GoldDiscount.class]
	- silverDiscount: defined in file [.../SilverDiscount.class]
	- staffDiscount: defined in file [.../StaffDiscount.class]

Action:

Consider marking one of the beans as @Primary, updating the consumer to accept multiple beans, or using @Qualifier
to identify the bean that should be consumed
```

The exception behind this report is a `NoUniqueBeanDefinitionException`: "expected single matching bean but found 4: basicDiscount,goldDiscount,silverDiscount,staffDiscount". Spring resolves the choice in this order:

1. **`@Qualifier("staffDiscount")`** on the injection point keeps only the beans with that qualifier. A bean's name counts as its qualifier, so `@Qualifier("staffDiscount")` matches the bean named `staffDiscount`. You can also give a bean its own qualifier with `@Qualifier` on its class or `@Bean` method.
2. **`@Primary`** on one bean makes it the default: it wins wherever the injection point doesn't say which bean it wants.
3. **`@Fallback`** (Spring 6.2) is the opposite of `@Primary`: a fallback bean is used only when no other candidate is left.
4. **The parameter name**: if there's still no single winner, Spring picks the bean whose name equals the parameter's name. This needs the `-parameters` compiler flag, which this repository's build sets. Don't rely on it: renaming a parameter shouldn't change your wiring.

To get **all** the candidates, ask for a collection:

```java
public DiscountService(List<DiscountPolicy> policies)                 // every policy, ordered by @Order
public DiscountService(Map<String, DiscountPolicy> policiesByName)    // bean name → bean
```

This is the *strategy pattern*: each policy is a strategy, and a small service picks one per request. Adding a strategy means adding a bean; no `if` or `switch` changes.

**The Lombok trap.** Spring reads `@Qualifier` from the constructor parameter, and Lombok's `@RequiredArgsConstructor` doesn't copy it there from the field:

```java
@RequiredArgsConstructor
public class DiscountService {
    @Qualifier("staffDiscount")              // stays on the field: Spring never sees it
    private final DiscountPolicy staffPolicy;
}
```

The application starts without an error, but `staffPolicy` is the `@Primary` policy. Write the constructor yourself when a parameter needs a qualifier, or add this line to a `lombok.config` file in the project, as many teams do:

```
lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Qualifier
```

### 8. Scopes

A bean's *scope* decides how many instances exist and how long they live.

- **singleton** (the default): one instance per application context, created at startup and shared by everyone who needs it. It's not the singleton *pattern*: nothing stops a test from calling `new`, and two contexts have two instances.
- **prototype**: a new instance every time someone asks the container for one. Spring creates it, injects its dependencies, calls its `@PostConstruct` method and hands it over. After that, Spring forgets it, so it **never calls a prototype's `@PreDestroy` method**.
- **request**, **session**, **application**: one instance per HTTP request, per HTTP session or per web application. They exist only in web applications.

```java
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)   // or @Scope("prototype")
public class ShoppingCart { ... }
```

**The prototype-in-a-singleton trap.** A singleton gets its dependencies once, when Spring creates it. If a singleton's constructor takes a `ShoppingCart`, it gets one cart, forever, and every member shares it. To get a new prototype each time, inject an `ObjectProvider` and ask it:

```java
@Service
@RequiredArgsConstructor
public class CartService {

    private final ObjectProvider<ShoppingCart> cartProvider;

    public ShoppingCart newCart() {
        return cartProvider.getObject();              // a new ShoppingCart, with its own dependencies injected
    }
}
```

`ObjectProvider` has more uses: `getIfAvailable()` for an optional dependency, and `stream()` or `orderedStream()` for all candidates. For request and session scopes, Spring injects a *scoped proxy* instead: an object that forwards each call to the instance of the current request (S04 and S12 meet them).

### 9. The lifecycle of a bean

For a singleton, Spring calls things in this order:

1. The constructor, with the dependencies as arguments.
2. Field and setter injection, if the class uses them.
3. The method annotated with **`@PostConstruct`**. By now every dependency is fully initialized, including its own `@PostConstruct` method.
4. … the bean is in use …
5. When the context closes, the method annotated with **`@PreDestroy`**. Spring destroys beans in the reverse order of creation, so a bean's dependencies still work in its `@PreDestroy` method.

```java
@Service
public class CartService {

    @PostConstruct
    void restoreSavedCarts() { ... }    // before any other bean can use this one

    @PreDestroy
    void saveOpenCarts() { ... }        // when the application stops
}
```

Things to know:

- **With constructor injection, the constructor already has every dependency.** Use `@PostConstruct` for work that you don't want in the constructor, such as loading data: a unit test that calls `new` then gets a plain object, and the loading happens only when Spring creates the bean.
- **`@PreDestroy` runs on a normal shutdown only:** when the context closes, for example through the shutdown hook on `Ctrl+C` or a `SIGTERM` from a deployment. A crash, a `kill -9` or a power cut skips it. Never keep the only copy of important data in memory until shutdown.
- **For classes you can't annotate,** name the methods on the `@Bean`: `@Bean(initMethod = "start", destroyMethod = "stop")`. For `@Bean` methods, Spring also finds a public `close()` or `shutdown()` method by itself and calls it on shutdown.
- **`ApplicationRunner` and `CommandLineRunner`** beans run once, after the application has started (after "Started BookstoreApplication"), when every bean is ready. Use them for work that needs the whole application, such as a report or a data import. A `@PostConstruct` method runs in the middle of startup instead, while other beans may not exist yet.
- The older interfaces `InitializingBean` and `DisposableBean` do the same as the annotations, but tie your class to Spring. `SmartLifecycle` is for components that start and stop with the application in phases, such as message consumers.

### 10. How Spring Boot configures itself

**Starters.** A *starter* is a dependency that brings everything one technology needs. `spring-boot-starter` is the core: Spring, auto-configuration support, logging and YAML. Spring Boot 4 split its code into small modules, one per technology: `spring-boot-webmvc`, `spring-boot-data-jpa`, `spring-boot-flyway`, and so on, each with its own auto-configuration. There's a starter for each (`spring-boot-starter-webmvc`) and a test starter for each (`spring-boot-starter-webmvc-test`). In Spring Boot 3, one large `spring-boot-autoconfigure` jar held the auto-configuration of every technology; now your classpath only holds what you use. Spring Boot's *bill of materials* (BOM) decides the version of every library it knows, so `build.gradle.kts` names no versions.

**Auto-configuration.** An *auto-configuration* is a `@Configuration` class, annotated `@AutoConfiguration`, that a library ships. Each jar lists its auto-configurations in a file named `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`, one class name per line. `@EnableAutoConfiguration` reads these files from every jar on the classpath and processes the classes **after your own configuration**. Conditions (section 11) then decide which of their beans apply. A simplified example of what the JDBC module does:

```java
@AutoConfiguration
@ConditionalOnClass(DataSource.class)                 // only if JDBC is on the classpath
public class DataSourceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(DataSource.class)       // only if the application has no DataSource of its own
    DataSource dataSource(DataSourceProperties properties) { ... }
}
```

This is the heart of Spring Boot: **it backs off**. Define a bean of your own, and the default steps aside. To switch off a whole auto-configuration, exclude it: `@SpringBootApplication(exclude = …)`, or the property `spring.autoconfigure.exclude`.

**The condition evaluation report.** Start the application with `--debug` (or set `debug: true`) and Spring Boot logs which conditions matched and why. Here's an excerpt from this module's application:

```
Positive matches:
-----------------
   NotificationConfiguration#emailNotifier matched:
      - @ConditionalOnProperty (bookstore.notifications.channel=email) matched (OnPropertyCondition)

   TaskExecutionAutoConfiguration matched:
      - @ConditionalOnClass found required class 'org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor' (OnClassCondition)

Negative matches:
-----------------
   ClockAutoConfiguration#clock:
      Did not match:
         - @ConditionalOnMissingBean (types: java.time.Clock; SearchStrategy: all) found beans of type 'java.time.Clock' clock (OnBeanCondition)

   JmxAutoConfiguration:
      Did not match:
         - @ConditionalOnBooleanProperty (spring.jmx.enabled=true) did not find property 'spring.jmx.enabled' (OnPropertyCondition)
```

When a bean you expected isn't there, or one you didn't expect is, this report tells you why. S14's Actuator shows the same information at `/actuator/conditions`.

### 11. Conditions

A *condition* decides whether a configuration class or a `@Bean` method counts at all. On a class, it applies to every bean the class defines; on a `@Bean` method, to that one bean. Spring Framework has the basic `@Conditional` and `@Profile` (S02); Spring Boot adds the ones you'll meet most:

| Condition | The bean exists only if … |
|---|---|
| `@ConditionalOnProperty(prefix = "bookstore.notifications", name = "channel", havingValue = "email")` | the property has this value. The comparison ignores letter case. `matchIfMissing = true` also matches when the property isn't set at all. |
| `@ConditionalOnBooleanProperty("feature.enabled")` | a `true`/`false` property is `true` (Spring Boot 3.5+); `matchIfMissing` works the same |
| `@ConditionalOnMissingBean(MailSender.class)` | no bean of this type exists yet |
| `@ConditionalOnBean(DataSource.class)` | a bean of this type exists |
| `@ConditionalOnClass(...)`, `@ConditionalOnMissingClass(...)` | a class is (or isn't) on the classpath |
| `@ConditionalOnWebApplication`, `@ConditionalOnNotWebApplication` | the application is (or isn't) a web application |

Two traps:

- **`@ConditionalOnMissingBean` sees only the beans registered before it.** In an auto-configuration that's reliable, because auto-configurations run after all of your beans. In your own configuration it sees the components found by scanning and the beans a test registers, but not the `@Bean` methods of configuration classes that Spring happens to process later. Use it in your own code only for defaults that live in the same class as the beans they compete with.
- **Without a type, `@ConditionalOnMissingBean` looks for the method's return type.** On a method that returns `LoggingMailSender`, it backs off only for another `LoggingMailSender`, not for another kind of `MailSender`. Name the type: `@ConditionalOnMissingBean(MailSender.class)`.

Choose silent fallbacks carefully. If a typo in a property quietly switches on a do-nothing bean, nobody notices until customers complain. It's often better to let an unknown value match no bean at all, so that the application fails at startup.

### 12. Writing your own auto-configuration

Write an auto-configuration when several applications need the same beans: a company's "starter" for its clock, its HTTP clients or its audit log. The steps:

1. **Write the class.** Annotate it `@AutoConfiguration`, put conditions on it, and register its settings with `@EnableConfigurationProperties`. Use `@ConditionalOnMissingBean` on every `@Bean` method, so applications can replace any bean.
2. **Register it.** List it in `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
3. **Keep it out of component scanning.** Put it in a package of its own, outside every application's packages. A scanned auto-configuration would be processed with the application's beans, too early for its conditions.
4. **Test it** with `ApplicationContextRunner` and `AutoConfigurations.of(...)`, which applies it the way Spring Boot does, after the user configuration:

```java
private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ClockAutoConfiguration.class));

@Test
void backsOff() {
    runner.withBean("applicationClock", Clock.class, () -> fixedClock)
            .run(context -> assertThat(context).hasSingleBean(Clock.class).getBean(Clock.class).isSameAs(fixedClock));
}
```

Settings come from a `@ConfigurationProperties` record, which Spring Boot fills from `application.yml`, environment variables and command-line arguments, converting each value to the component's type:

```java
@ConfigurationProperties("platform.clock")
public record ClockProperties(@DefaultValue("UTC") ZoneId zone, @DefaultValue("1us") Duration precision) {
}
```

S02 covers configuration properties in depth. `@AutoConfiguration(before = …, after = …)` orders auto-configurations among themselves when one builds on another's beans. Large libraries split the code into an `…-autoconfigure` module and a `…-starter` module that only pulls in dependencies; small ones ship a single jar.

## TypeScript/NestJS → Spring

| NestJS / TypeScript | Spring | Watch out for |
|---|---|---|
| `@Injectable()` class listed in a module's `providers` | `@Component`/`@Service` class, found by component scanning | There's no list: every component under the application class's package is a bean. |
| `@Module({ providers, imports, exports })` | `@Configuration` classes and packages | Spring has no module boundaries: every bean can inject every other bean. |
| `{ provide: Clock, useValue: clock }`, `useFactory` | a `@Bean` method in a `@Configuration` class | The method's parameters are injected like constructor parameters. |
| `constructor(private readonly repo: Repo) {}` | `private final Repo repo;` with `@RequiredArgsConstructor` | TypeScript types disappear at runtime, so Nest needs decorator metadata or `@Inject(token)`. Java keeps the types, so injection by type works on its own. |
| `@Inject('STAFF_DISCOUNT')`, injection tokens | `@Qualifier("staffDiscount")` | Lombok doesn't copy `@Qualifier` from a field to the constructor (section 7). |
| `Scope.DEFAULT`, `Scope.TRANSIENT`, `Scope.REQUEST` | singleton, prototype, request | In Nest, a request-scoped provider makes its consumers request-scoped too. In Spring, a singleton keeps what it got: use `ObjectProvider` or a scoped proxy. |
| `onModuleInit()`, `onModuleDestroy()` | `@PostConstruct`, `@PreDestroy` | Spring never calls `@PreDestroy` on a prototype. |
| `onApplicationBootstrap()` | `ApplicationRunner`, `@EventListener(ApplicationReadyEvent.class)` | |
| `app.enableShutdownHooks()` | on by default | Spring Boot registers the JVM shutdown hook itself. |
| `moduleRef.get(X)`, `moduleRef.resolve(X)` | `context.getBean(X)`, `ObjectProvider<X>` | Prefer constructor injection; use `ObjectProvider` for prototypes and optional beans. |
| dynamic modules: `MailModule.forRoot({ ... })` | auto-configuration with `@ConfigurationProperties` | An auto-configuration needs no call in your code: the jar on the classpath and the properties are enough. |
| `Test.createTestingModule({...}).overrideProvider(X).useValue(fake)` | `ApplicationContextRunner.withBean(...)`, `@MockitoBean` in `@SpringBootTest` | |
| `NestFactory.create(AppModule)`, then `app.listen()` | `SpringApplication.run(BookstoreApplication.class, args)` | `run` also starts the web server, when there is one. |

## In real projects

- **Constructor injection, `final` fields, `@RequiredArgsConstructor`.** No `@Autowired` on fields. When a parameter needs a `@Qualifier`, write the constructor or configure Lombok to copy the annotation. See [Lombok](../../docs/handbook/lombok.md) and [Spring layering](../../docs/handbook/spring-layering.md).
- **Stateless singletons.** A bean's fields hold its dependencies and settings; the data of one request lives in local variables, parameters and return values. If a bean must keep state, make it thread-safe. See [Concurrency](../../docs/handbook/concurrency.md).
- **The application class in the root package, features in packages below it.** Shared libraries and auto-configurations live in packages of their own. See [Spring layering](../../docs/handbook/spring-layering.md).
- **Inject a `Clock`, never call `Instant.now()`** in application code, so tests control time. See [Dates and money](../../docs/handbook/dates-and-money.md).
- **`@Configuration(proxyBeanMethods = false)`**, and take other beans as `@Bean` method parameters.
- **Fail at startup, not in production traffic.** Check required beans and settings while the context starts: in a constructor, in a `@PostConstruct` method, with conditions that match no bean for an unknown value, or with validated properties (S02). See [Configuration](../../docs/handbook/configuration.md).
- **Read startup failures from the `APPLICATION FAILED TO START` block**, and use `--debug` to see why a bean exists or doesn't. See [Logging and observability](../../docs/handbook/logging-and-observability.md).
- **Test wiring with `ApplicationContextRunner`, the whole application with few `@SpringBootTest` contexts**: every different test configuration starts another context, which costs seconds. See [Testing Spring applications](../../docs/handbook/spring-testing.md).

## Exercises

The exercises add to one application, but each one stands alone: its tests start a small context with only its own beans, so you can do them in any order. Run `./score s01` for the whole module, or `./score s01-03` for one exercise with every failing test and its message.

### S01-01 · Create a Spring Boot application

**Build** · 30 min · [`BookstoreApplication.java`](src/main/java/com/example/bookstore/BookstoreApplication.java) · [`CatalogStartupReport.java`](src/main/java/com/example/bookstore/book/CatalogStartupReport.java) · [`application.yml`](src/main/resources/application.yml)

> **BOOK-101 · Make the bookstore a Spring Boot application**
>
> The bookstore's code exists, but nothing starts it: `BookstoreApplication` is a plain class whose `main` method does nothing yet. Make it a Spring Boot application that finds its own beans, configures itself, and reports its catalog at startup, so that whoever starts it sees that the catalog is in place.
>
> Acceptance criteria:
> - `BookstoreApplication.main` starts Spring Boot and passes the command-line arguments on.
> - Component scanning finds the beans in `com.example.bookstore` and below, such as `BookCatalog`, and auto-configuration is on.
> - `application.yml` names the application `bookstore` and makes it a non-web application.
> - Once the application has started, the log shows `The catalog holds 60 books in 6 categories.`, written by `CatalogStartupReport`, an `ApplicationRunner`.

Every Spring Boot project starts like this one. Most start at [start.spring.io](https://start.spring.io) (also built into IntelliJ IDEA's *New Project* dialog): you choose the build tool, the language, the Spring Boot version, a group and artifact name, and the starters; it generates a project you can run at once. For this module, it would generate:

```
bookstore/
├── build.gradle.kts                 the build: plugins, Java version, starters
├── settings.gradle.kts              the project's name
├── gradlew, gradlew.bat, gradle/    the Gradle wrapper: the right Gradle version, without installing it
├── src/main/java/com/example/bookstore/BookstoreApplication.java
├── src/main/resources/application.properties
└── src/test/java/com/example/bookstore/BookstoreApplicationTests.java
```

| Part | Why it exists |
|---|---|
| the `org.springframework.boot` Gradle plugin | It adds `bootRun` and `bootJar`, which builds one runnable jar with every dependency inside. With it, Spring's BOM chooses the library versions, through a dependency-management plugin or Gradle's `platform(...)`, as this repository does in `build-logic`. |
| `java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }` | Gradle compiles and runs with Java 25, whatever JDK your shell has. |
| `implementation("org.springframework.boot:spring-boot-starter")` | The core starter: Spring, auto-configuration, logging, YAML. Each starter you pick adds a line, such as `spring-boot-starter-webmvc`. |
| `testImplementation("org.springframework.boot:spring-boot-starter-test")` | JUnit, AssertJ, Mockito and Spring's test support. |
| `BookstoreApplication` | The entry point: `@SpringBootApplication` and a `main` method, in the root package. |
| `application.properties` | The configuration, with one line: `spring.application.name=bookstore`. This repository uses the YAML format, `application.yml`, which nests better. |
| `BookstoreApplicationTests` | One test, `contextLoads()`, with `@SpringBootTest`: it fails if the application context can't start. Small as it is, it catches every wiring mistake. |

In this repository, the build file is shorter: the `practice.spring-exercises` plugin in `build-logic/` holds what every module shares (the Spring Boot plugin and BOM, Java 25, Lombok, the test setup), and [`build.gradle.kts`](build.gradle.kts) lists only this module's starter.

**Before you start:** lesson sections 1 to 3.

**You'll practice:** `@SpringBootApplication`, `SpringApplication.run`, `application.yml`, an `ApplicationRunner` bean with constructor injection and Lombok's `@Slf4j` logger.

**Rules:** in the Javadoc of `BookstoreApplication` and `CatalogStartupReport`, and the TODO in `application.yml`.

**Try it:** when the tests pass, run `./gradlew :spring:s01-boot-fundamentals:bootRun` from the repository root. The application starts, logs the catalog summary and exits, because no web server keeps it running. Then try `--args='--spring.application.name=demo'` and look at the log lines.

<details>
<summary>Hint 1</summary>

The tests start the application through `BookstoreApplication.main`, the way the `java` command does. If `main` doesn't hand `args` to `SpringApplication.run`, a command-line argument such as `--spring.application.name=demo` never reaches Spring.
</details>

<details>
<summary>Hint 2</summary>

`CatalogStartupReport` needs three annotations: one that makes it a bean, one that writes its constructor, and one that gives it a `log` field. Count the categories with `stream().map(Book::category).distinct().count()`.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: What would change if `BookstoreApplication` moved to the package `com.example.bookstore.app`? Why does the summary appear after "Started BookstoreApplication" and not before?

### S01-02 · Beans and constructor injection

**Build** · 30 min · [`PriceCalculator.java`](src/main/java/com/example/bookstore/order/PriceCalculator.java) · [`CheckoutService.java`](src/main/java/com/example/bookstore/order/CheckoutService.java) · [`ClockConfiguration.java`](src/main/java/com/example/bookstore/common/ClockConfiguration.java)

> **BOOK-102 · Check out an order**
>
> A member checks out a list of books with quantities. The bookstore looks each book up in the catalog, prices the order and stamps it with the time. Write the three beans this takes.
>
> Acceptance criteria:
> - `PriceCalculator` and `CheckoutService` are beans found by component scanning. `CheckoutService` gets `BookCatalog`, `PriceCalculator` and `Clock` through its only constructor, into `final` fields.
> - `ClockConfiguration` provides the `Clock` bean with a `@Bean` method: UTC, in whole microseconds.
> - A checkout lists one line per book at the catalog's price, in ascending book id order, adds 3,000 won shipping below 20,000 won, and is stamped with the injected clock's time.
> - An empty order, an unknown book and a quantity below 1 are rejected with `IllegalArgumentException`.

The tests replace the application's clock with a `TestClock` that stands still at 2026-10-15T00:00:00Z. Because it's registered as `@Primary`, it wins over your `clock` bean wherever a `Clock` is injected. That's the payoff of injecting a `Clock`: time-dependent code becomes testable.

**Before you start:** lesson sections 4 to 6.

**You'll practice:** `@Component`, `@Service`, `@Configuration(proxyBeanMethods = false)` with a `@Bean` method, constructor injection with `final` fields and `@RequiredArgsConstructor`, using an injected `Clock`.

**Rules:** in the Javadoc of the three classes. The records `OrderLine` and `Receipt` are finished.

<details>
<summary>Hint 1</summary>

`BookCatalog.findById` returns an `Optional<Book>`: `orElseThrow(() -> new IllegalArgumentException(...))` turns a missing book into the exception the tests expect. `OrderLine` already rejects a quantity below 1.
</details>

<details>
<summary>Hint 2</summary>

A `Map` has no order you can rely on. Sort its entries before you build the lines: `entrySet().stream().sorted(Map.Entry.comparingByKey())`.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: How would a plain unit test, without Spring, create your `CheckoutService` with a fixed clock? What would break if `ClockConfiguration` called `Instant.now()` instead of returning a `Clock`?

### S01-03 · Choosing between implementations

**Build** · 30 min · [`DiscountService.java`](src/main/java/com/example/bookstore/discount/DiscountService.java) · [`BasicDiscount.java`](src/main/java/com/example/bookstore/discount/BasicDiscount.java)

> **BOOK-103 · Discounts by member grade**
>
> Members get a discount by grade: none for `BASIC`, 5% for `SILVER`, 10% for `GOLD`. Guests pay the full price, and the bookstore's employees get 20%. Each rule is already a `DiscountPolicy` bean. Write the `DiscountService` that picks the right one.
>
> Acceptance criteria:
> - Each grade gets the policy bean named after it: `basicDiscount`, `silverDiscount`, `goldDiscount`.
> - Guests, and every bean that asks for "a `DiscountPolicy`" without saying which, get `BasicDiscount`.
> - Staff purchases always use the `staffDiscount` policy.
> - If a grade has no policy bean, the application doesn't start, and the error names the grade and the missing bean.

**What happens with two candidates and no qualifier?** Try it before you write any code: make `DiscountService` a `@Service` whose constructor takes one `DiscountPolicy`, and run the tests. The context doesn't start, and the failure reads `No qualifying bean of type 'com.example.bookstore.discount.DiscountPolicy' available: expected single matching bean but found 4: basicDiscount,goldDiscount,silverDiscount,staffDiscount`. Spring refuses to guess. Section 7 lists the ways to tell it which bean you mean.

**Before you start:** lesson section 7.

**You'll practice:** `@Primary`, `@Qualifier`, injecting a `Map<String, DiscountPolicy>`, the strategy pattern, checking at startup in a constructor, `EnumMap`.

**Rules:** in the Javadoc of `DiscountService`. The policies are finished; only `BasicDiscount` needs one more annotation.

<details>
<summary>Hint 1</summary>

The constructor has three parameters: `Map<String, DiscountPolicy> policiesByBeanName`, `DiscountPolicy defaultPolicy` and `@Qualifier("staffDiscount") DiscountPolicy staffPolicy`. Write it yourself instead of using Lombok: it has to check the map, and a `@Qualifier` on a field wouldn't reach the constructor (section 7).
</details>

<details>
<summary>Hint 2</summary>

In the constructor, loop over `MemberGrade.values()`, look up `grade.name().toLowerCase(Locale.ROOT) + "Discount"`, and store each policy in an `EnumMap<MemberGrade, DiscountPolicy>`. If one is missing, throw `IllegalStateException`. Then `discountFor` needs no string handling at all.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: What do you change to add a `PLATINUM` grade? Why is a missing policy better found in the constructor than in `discountFor`?

### S01-04 · Bean scopes and lifecycle

**Build** · 30 min · [`CartService.java`](src/main/java/com/example/bookstore/cart/CartService.java) · [`ShoppingCart.java`](src/main/java/com/example/bookstore/cart/ShoppingCart.java)

> **BOOK-104 · One cart per member, kept across restarts**
>
> Members need a shopping cart of their own, and after each deployment they complain that their carts are empty. `ShoppingCart` already works; make Spring create a new one for every member, and make `CartService` save the open carts when the application stops and restore them when it starts.
>
> Acceptance criteria:
> - `CartService` is a singleton; `ShoppingCart` is a prototype, and every cart gets its `BookCatalog` injected.
> - A member gets the same cart on every call; two members never share one. `discard` starts a member's next cart afresh.
> - When the application stops, every open cart that isn't empty is saved in the `SavedCartStore`.
> - When the application starts, before anyone asks for a cart, every saved cart is opened again with its books, and the store is emptied.

`SavedCartStore` plays an external store, such as Redis or a database table, that keeps data while the application restarts. The tests simulate a restart: they close one application context and start a second one with the same store.

**Before you start:** lesson sections 8 and 9.

**You'll practice:** `@Scope("prototype")`, `ObjectProvider.getObject()`, `@PostConstruct`, `@PreDestroy`, `ConcurrentHashMap.computeIfAbsent` in a bean that many threads share.

**Rules:** in the Javadoc of `CartService`. `ShoppingCart`'s methods are finished; it needs two annotations.

<details>
<summary>Hint 1</summary>

`cartsByMemberId.computeIfAbsent(memberId, id -> cartProvider.getObject())` finds a member's cart or creates it, in one atomic step on a `ConcurrentHashMap`.
</details>

<details>
<summary>Hint 2</summary>

To restore a cart, ask the provider for a new one and `add` the saved lines to it. To save, store `cart.lines()` for every cart that isn't empty. Spring calls the two lifecycle methods for you; nothing else in your code calls them.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: What happens to the open carts if the server crashes? Why would injecting a `ShoppingCart` into `CartService`'s constructor give every member the same cart, although `ShoppingCart` is a prototype?

### S01-05 · Conditional beans

**Build** · 30 min · [`NotificationConfiguration.java`](src/main/java/com/example/bookstore/notification/NotificationConfiguration.java)

> **BOOK-105 · Switch notifications per environment**
>
> Order emails must go out in production, but developers don't want real emails from their laptops. Choose the notifier with the property `bookstore.notifications.channel`, and give the mail sender a default that an application can replace.
>
> Acceptance criteria:
> - Channel `email` (in any letter case): the `Notifier` bean is an `EmailNotifier` that sends through the `MailSender` bean.
> - Channel `none`, or no channel property: the `Notifier` bean is a `NoOpNotifier`.
> - Any other value registers no `Notifier`, so that a bean that needs one stops the application at startup.
> - The `MailSender` bean is a `LoggingMailSender`, unless the application has a `MailSender` of its own.

**Before you start:** lesson sections 10 and 11.

**You'll practice:** `@Bean` methods for classes without `@Component`, `@ConditionalOnProperty` with `havingValue` and `matchIfMissing`, `@ConditionalOnMissingBean`, reading the condition evaluation report.

**Rules:** in the Javadoc of `NotificationConfiguration`. The notifiers and mail senders are finished.

**Try it:** when the tests pass, run `./gradlew :spring:s01-boot-fundamentals:bootRun --args='--debug --bookstore.notifications.channel=email'` and find `NotificationConfiguration#emailNotifier` and `NotificationConfiguration#noOpNotifier` in the report. Then try the channel `emial`: the application still starts, because no bean in it needs a `Notifier` yet.

<details>
<summary>Hint 1</summary>

Each notifier gets its own `@ConditionalOnProperty(prefix = "bookstore.notifications", name = "channel", havingValue = "...")`. Only one of them also matches when the property is missing.
</details>

<details>
<summary>Hint 2</summary>

`@ConditionalOnMissingBean(Notifier.class)` on the no-op notifier looks tempting. Which of the acceptance criteria would it break?
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Why is the `@ConditionalOnMissingBean` on the mail sender safe here, and when would it not be? What would a typo in the channel do in production with your configuration?

### S01-06 · Write your own auto-configuration ★

**Build** · 30 min · [`ClockAutoConfiguration.java`](src/main/java/com/example/platform/clock/ClockAutoConfiguration.java) · [`AutoConfiguration.imports`](src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports)

> **PLAT-17 · A clock starter for every service**
>
> Every service of the company copies the `ClockConfiguration` you wrote in S01-02. The platform team wants to ship it once, in a library: any application with the library on its classpath gets the standard clock, can set its zone and precision, and can still define its own clock, in tests for example.
>
> Acceptance criteria:
> - `ClockAutoConfiguration` is an auto-configuration, listed in `AutoConfiguration.imports`, so `@EnableAutoConfiguration` finds it.
> - It provides a `Clock`: by default UTC, in whole microseconds; `platform.clock.zone` and `platform.clock.precision` change both.
> - It backs off when the application has a `Clock` of its own, and `platform.clock.enabled=false` switches it off.
> - An unknown zone or a negative precision stops the application at startup.

The package `com.example.platform.clock` plays the library: in a real company it would be a separate jar. It sits outside `com.example.bookstore`, so the bookstore's component scanning never sees it. The properties record `ClockProperties` is finished.

**Before you start:** lesson section 12.

**You'll practice:** `@AutoConfiguration`, the `AutoConfiguration.imports` file, `@ConditionalOnMissingBean`, `@ConditionalOnBooleanProperty`, `@EnableConfigurationProperties`, testing with `AutoConfigurations.of(...)`.

**Rules:** in the Javadoc of `ClockAutoConfiguration`.

**Try it:** with S01-02 and S01-06 both done, run the application with `--debug`. The report lists `ClockAutoConfiguration#clock` under *Negative matches*, because the bookstore has a `Clock` of its own.

<details>
<summary>Hint 1</summary>

The imports file needs one line: the fully qualified name of your class. Lines that start with `#` are comments.
</details>

<details>
<summary>Hint 2</summary>

The switch is a condition on the class: `@ConditionalOnBooleanProperty(name = "platform.clock.enabled", matchIfMissing = true)`. The clock itself is `Clock.tick(Clock.system(zone), precision)`.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Why must an auto-configuration never be found by component scanning? What does the application have to do to get your clock?

## Review questions

1. What's the difference between a bean definition and a bean?
2. Which three annotations does `@SpringBootApplication` combine, and what does each do?
3. A new `@Service` in `com.example.reports` is never created, although the application class is `com.example.bookstore.BookstoreApplication`. Why?
4. When do you write a `@Bean` method instead of annotating the class with `@Component`?
5. Give three reasons to prefer constructor injection over `@Autowired` fields.
6. Two beans implement `PaymentGateway`, and a service asks for one. What happens, and what are your options?
7. A class uses Lombok's `@RequiredArgsConstructor` and has `@Qualifier("staffDiscount")` on a field. It gets the wrong bean, without any error. Why?
8. A singleton service holds a prototype bean in a field. How many instances of the prototype does it use, and how do you get a new one per call?
9. In which order does Spring call the constructor, `@PostConstruct`, `@PreDestroy` and an `ApplicationRunner`? Which of them can be skipped?
10. Your application has its own `DataSource` bean, and Spring Boot's `DataSourceAutoConfiguration` is on the classpath. Why is there still only one `DataSource`?
11. A bean you expected isn't in the context. How do you find out why?
12. Why should `@ConditionalOnMissingBean` live mainly in auto-configurations?

<details>
<summary>Answers</summary>

1. A bean definition is the recipe: the class, name, scope, dependencies and callbacks, read before any object exists. A bean is the object the container creates from it. A prototype definition produces many beans; a condition can remove a definition before any bean exists.
2. `@SpringBootConfiguration` makes the class a configuration class. `@EnableAutoConfiguration` loads the auto-configurations listed in the `AutoConfiguration.imports` files of the classpath. `@ComponentScan` finds the component classes in the class's package and below.
3. Component scanning starts at the package of the `@SpringBootApplication` class, `com.example.bookstore`, and covers only its sub-packages. `com.example.reports` is a sibling. Move the class under `com.example.bookstore`.
4. When you can't annotate the class, because it comes from the JDK or a library (`Clock`, a client from an SDK), and when creating the object takes logic or settings, such as choosing an implementation by a property.
5. The object is complete and its fields can be `final`; a unit test creates it with `new` and passes fakes; the dependencies are visible in one place, and a long constructor shows that a class does too much; cycles fail at startup.
6. The application doesn't start: `NoUniqueBeanDefinitionException`, "expected single matching bean but found 2". Mark one bean `@Primary`, put `@Qualifier` on the injection point, mark one `@Fallback`, or inject `List<PaymentGateway>` or `Map<String, PaymentGateway>` and choose in code.
7. Spring reads qualifiers from the constructor parameter, and Lombok doesn't copy `@Qualifier` from the field to the parameter it generates. Without a qualifier, the `@Primary` bean wins. Write the constructor by hand, or add `@Qualifier` to `lombok.copyableAnnotations` in `lombok.config`.
8. One: Spring injects dependencies once, when it creates the singleton. Inject `ObjectProvider<T>` and call `getObject()` for a new instance per call.
9. The constructor, then `@PostConstruct`, then (after "Started …") the `ApplicationRunner`, and `@PreDestroy` when the context closes. `@PreDestroy` is skipped on a crash or `kill -9`, and never called for prototype beans.
10. Auto-configurations run after the application's own configuration, and Spring Boot's `DataSource` bean method has `@ConditionalOnMissingBean`: it sees your `DataSource` and backs off.
11. Start with `--debug` and read the condition evaluation report: it says which condition of which configuration matched or didn't, and why. Also check that the class is under the application class's package and has a stereotype annotation. In a running application, Actuator's `/actuator/conditions` and `/actuator/beans` show the same (S14).
12. It sees only the bean definitions registered before it. Auto-configurations run after every bean of the application, so the check is complete there. In your own configuration classes, the result depends on the order in which Spring processes them.
</details>

## Further reading

- Spring Framework reference: [The IoC container](https://docs.spring.io/spring-framework/reference/core/beans.html), especially [Classpath scanning and managed components](https://docs.spring.io/spring-framework/reference/core/beans/classpath-scanning.html), [Bean scopes](https://docs.spring.io/spring-framework/reference/core/beans/factory-scopes.html), [Lifecycle callbacks](https://docs.spring.io/spring-framework/reference/core/beans/factory-nature.html) and [Autowiring with qualifiers](https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/autowired-qualifiers.html)
- Spring Boot reference: [Structuring your code](https://docs.spring.io/spring-boot/reference/using/structuring-your-code.html), [SpringApplication](https://docs.spring.io/spring-boot/reference/features/spring-application.html), [Auto-configuration](https://docs.spring.io/spring-boot/reference/using/auto-configuration.html) and [Creating your own auto-configuration](https://docs.spring.io/spring-boot/reference/features/developing-auto-configuration.html), which also covers `ApplicationContextRunner`
- [Spring Boot 4.0 migration guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide): the new modules and starters, if you meet Spring Boot 3 projects at work
- [Spring Initializr](https://start.spring.io): generate a project and read every file it contains
- [Lombok configuration](https://projectlombok.org/features/configuration): `lombok.config` and `copyableAnnotations`
