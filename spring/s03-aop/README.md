# S03 · AOP

⏱ 4 sessions · weeks 7–8 · 4 exercises

Some code belongs to no single feature: measuring how long calls take, writing an audit log, opening a transaction, checking permissions, caching a result. Written by hand, it repeats in every service method and buries the business logic. *Aspect-oriented programming* (AOP) moves such code into one class, an *aspect*, and declares where it applies. Spring uses AOP for many of its own features: `@Transactional`, `@Cacheable`, `@Async` and `@PreAuthorize` are all aspects. In this module you write two aspects for the bookstore, fix the most common AOP bug (a call that skips the proxy), and predict how Spring's proxies behave. The proxies work like the ones you built in J09-03, so their traps will look familiar. They come back in S07 (transactions) and S10 (caching).

S03 has no database: the coupons and books live in memory, so the application and its tests start without Docker.

## Lesson

Each session, read the lesson sections for that day, then do the exercise.

| Session | Read | Then do |
|---|---|---|
| 1 | 1. Cross-cutting concerns · 2. Proxies · 3. JDK proxies and CGLIB · 4. Your first aspect | S03-01 |
| 2 | 5. Pointcut expressions · 6. Advice types | S03-02 |
| 3 | 8. What a proxy can't intercept | S03-03 |
| 4 | 7. Ordering advice · 9. Spring's own aspects · 10. Testing aspects · reread 3 | S03-04 |

### 1. Cross-cutting concerns

A *cross-cutting concern* is a requirement that applies to many methods across many features: timing, logging, auditing, transactions, security checks, caching, retries. Here is what it looks like when two of them are written by hand:

```java
public CouponIssue issue(String code, long memberId) {
    long start = ticker.read();
    boolean failed = true;
    try {
        CouponIssue issue = ...;                     // the business logic
        auditLog.add(AuditEntry.succeeded(clock.instant(), "CouponService.issue", new Object[] {code, memberId}));
        failed = false;
        return issue;
    } catch (RuntimeException e) {
        auditLog.add(AuditEntry.failed(clock.instant(), "CouponService.issue", new Object[] {code, memberId}, e));
        throw e;
    } finally {
        Duration elapsed = Duration.ofNanos(ticker.read() - start);
        timings.record(new MethodTiming("CouponService.issue", elapsed, failed));
    }
}
```

One line of business logic and more than ten lines of plumbing, copied into every method, slightly different each time, and forgotten in some. AOP writes the plumbing once:

| Term | Meaning | Example in this module |
|---|---|---|
| **Aspect** | A class that holds one cross-cutting concern. | `TimingAspect`, `AuditAspect` |
| **Join point** | A point in the program where an aspect can add behavior. In Spring AOP, a join point is always the call of a method on a Spring bean. | a call of `CouponService.issue` |
| **Pointcut** | An expression that selects join points. | "every method annotated with `@Timed`" |
| **Advice** | The code an aspect runs at the selected join points, and when: before, after, or around the method. | the method that measures a call |
| **Target** | The object whose method is called. | the `CouponService` object |
| **Proxy** | The object that Spring puts in front of the target to run the advice. | `CouponService$$SpringCGLIB$$0` |

### 2. Proxies: how Spring adds behavior to a bean

In J09-03 you built a dynamic proxy: an object that stands in for a target, runs your code, and forwards each call. Spring AOP works the same way. At startup, a *bean post-processor* (a bean that can inspect and replace every other bean after it's created) checks each new bean against the pointcuts of all aspects. When a pointcut matches one of the bean's methods, the post-processor wraps the bean in a proxy, and the container stores the proxy instead of the bean. From then on, every bean that depends on it gets the proxy:

```text
CouponCampaignService ──couponService.issue(…)──► proxy ──► CouponService object (the target)
                                                   │
                                                   └─ runs the advice whose pointcut matches issue(),
                                                      and calls the target's method in the middle
```

What follows from this design:

- **Only calls that go through the proxy get advice.** A call that starts inside the target, like `this.issue(…)`, never reaches the proxy (section 8).
- **A bean that no pointcut matches is not wrapped.** You get the object itself.
- **The proxy is created after the bean is initialized**, so code in the constructor and in `@PostConstruct` runs without any advice.
- **Spring AOP only intercepts method calls on Spring beans.** It can't intercept constructors, field access, `static` methods or objects you create with `new`. The full AspectJ can, because it changes the compiled bytecode (*weaving*). Spring borrows AspectJ's annotations and pointcut language, but not its weaver: no special compiler is needed.

### 3. JDK proxies and CGLIB

Spring can create two kinds of proxies:

| | JDK dynamic proxy | CGLIB proxy |
|---|---|---|
| What it is | a `java.lang.reflect.Proxy`, as in J09-03: a new class that implements the bean's interfaces | a subclass of the bean's class, generated at startup |
| Needs | at least one interface | a class that isn't `final` |
| Can be injected as | the interface only | the interface or the class |
| Class name | like `jdk.proxy2.$Proxy81` | like `CouponService$$SpringCGLIB$$0` |

Plain Spring uses a JDK proxy when the bean implements an interface, and CGLIB otherwise. **Spring Boot uses CGLIB for every proxy**: its property `spring.aop.proxy-target-class` is `true` unless you set it to `false`. The reason: with a JDK proxy, a constructor that asks for the class instead of the interface fails at startup, because the proxy is not an instance of that class.

How a CGLIB proxy works: the generated subclass overrides each method that it can (not `final`, not `private`, not `static`). The override runs the advice and calls the same method on the target. The proxy is a second object of the bean's class, created without running any constructor (Spring uses the Objenesis library for that), so its own fields stay empty: `null`, `0` or `false`. That does no harm, because each overridden method forwards to the target, whose fields are set. A `final` method can't be overridden, so the proxy runs it itself, on its own empty fields. S03-04 shows what that does.

### 4. Your first aspect

The AOP starter brings AspectJ's annotations and pointcut parser (Spring Boot 4 renamed it from `spring-boot-starter-aop`):

```kotlin
implementation("org.springframework.boot:spring-boot-starter-aspectj")
```

With it on the classpath, Spring Boot turns on aspect support for you; you don't need `@EnableAspectJAutoProxy`. An aspect is a bean whose class is annotated with `@Aspect`:

```java
@Aspect       // this class declares pointcuts and advice
@Component    // and it's a bean: @Aspect alone doesn't make Spring create it
@Slf4j
public class SlowCallLogger {

    @Around("execution(* com.example.bookstore.book.BookService.*(..))")   // the pointcut: which calls
    public Object logSlowCalls(ProceedingJoinPoint call) throws Throwable { // the advice: what happens
        long start = System.nanoTime();
        try {
            return call.proceed();                                          // runs the real method
        } finally {
            long millis = (System.nanoTime() - start) / 1_000_000;
            if (millis > 500) {
                log.warn("{} took {} ms", call.getSignature().toShortString(), millis);
            }
        }
    }
}
```

`@Around` advice receives a `ProceedingJoinPoint`, the intercepted call:

- `call.proceed()` runs the method (or the next advice, if there are several) and returns its result: a boxed value for a primitive return type, `null` for `void`. If you don't call it, the method doesn't run at all.
- `call.getSignature()` describes the method: `getName()` is `search`, `getDeclaringType()` is `BookService.class`. `call.getArgs()` returns the arguments, and `call.getTarget()` the target object.
- Return what `proceed()` returned, and let exceptions through unchanged. The advice method declares `throws Throwable`, so `catch (Throwable e) { …; throw e; }` compiles. Wrapping the exception in another one changes what callers can catch. A checked exception that the method doesn't declare would even reach them as an `UndeclaredThrowableException`.

**Binding an annotation.** A pointcut can pass values to the advice. Here, `@annotation(timed)` matches every method annotated with the type of the parameter named `timed`, and passes that method's annotation in:

```java
@Around("@annotation(timed)")
public Object time(ProceedingJoinPoint call, Timed timed) throws Throwable {
    String name = timed.value();   // read the annotation's attributes, no reflection needed
    ...
}
```

Spring reads the parameter names from the compiled class, where the compiler flag `-parameters` stores them. Without them, Spring can only work out simple cases like this one. This repository's build sets the flag, and so do Spring Boot's Gradle plugin and its Maven parent POM.

**Measuring time.** The `Clock` tells the time of day, which can jump when the computer synchronizes its clock. Durations are measured with `System.nanoTime()`, which never jumps. The bookstore wraps it in a `Ticker` bean, so tests can replace it with a fake one that moves only when the test says so.

### 5. Pointcut expressions

Spring AOP supports these *pointcut designators*:

| Designator | Matches calls of… | Example |
|---|---|---|
| `execution(…)` | methods with a matching signature | `execution(public * com.example.bookstore..*Service.find*(..))` |
| `within(…)` | every method of the matching types | `within(com.example.bookstore.coupon..*)` |
| `@annotation(…)` | methods annotated with the annotation | `@annotation(com.example.bookstore.common.timing.Timed)` |
| `@within(…)` | every method of classes annotated with the annotation | `@within(org.springframework.stereotype.Service)` |
| `bean(…)` | every method of the beans with a matching name (Spring only) | `bean(couponService)`, `bean(*Service)` |
| `args`, `this`, `target`, `@args`, `@target` | by argument types, proxy type, target type, and their annotations | `args(String, ..)` |

`execution` has the most parts. Only the return type, the name and the parameters are required:

```text
execution( public     *             com.example.bookstore.coupon.CouponService .issue  (String, long) )
           modifier   return type   declaring type                             name    parameters
```

- `*` stands for any one name, or any part of a name: `find*` matches `findCoupon` and `findBooks`.
- `..` in a package stands for any number of sub-packages: `com.example.bookstore..*` is every class in the bookstore. In parameters it stands for any number of parameters of any type: `(..)`, `(String, ..)`.
- Annotation types and classes outside `java.lang` need their full name: `@within(org.springframework.stereotype.Service)`.

Combine pointcuts with `&&`, `||` and `!`, and give reusable pieces a name with `@Pointcut`. The method only carries the name; its body stays empty:

```java
@Pointcut("within(com.example.bookstore.order..*)")
void inOrderFeature() {
}

@Pointcut("execution(* find*(..))")
void readOnly() {
}

@Before("inOrderFeature() && !readOnly()")
public void logWrite(JoinPoint call) {
    log.info("write: {}", call.getSignature().toShortString());
}
```

Choose pointcuts that match exactly what you mean:

- **An annotation you own is the clearest choice.** A method is advised if, and only if, it says so (`@annotation(…)`), and renaming the method or moving the class changes nothing.
- **Package and name patterns are silent.** A new class in the package is included automatically; a renamed method silently drops out. Use them for rules that really cover "everything in this package".
- **`within` checks the package, `@within` the class's annotation.** `@within(org.springframework.stereotype.Service)` excludes repositories and other components in the same package.
- **Spring's class proxies also intercept `protected` and package-private methods** when they're called from the same package. Add `execution(public * *(..))` when only the public API should match.
- **Watch for an aspect that advises its own helpers.** Spring never applies advice to an aspect itself. But if the audit pointcut also matched the `AuditLog` bean, writing an audit entry would trigger the audit advice again, forever.

### 6. Advice types

| Advice | Runs | Can it change what the caller gets? | Typical use |
|---|---|---|---|
| `@Before` | before the method | only by throwing: then the method doesn't run | checks, logging calls |
| `@AfterReturning` | after the method returned normally | no; it can read the result | auditing successes |
| `@AfterThrowing` | after the method threw | no; the exception continues to the caller (unless the advice throws another one) | auditing failures |
| `@After` | after the method, both ways, like `finally` | no | releasing resources |
| `@Around` | around the method: it decides whether and when the method runs | yes: arguments, result and exceptions | timing, transactions, caching, retries |

Use the least powerful advice that does the job. An `@AfterReturning` advice can't forget to call `proceed()`, swallow an exception or return the wrong result. The narrower types also bind values:

```java
@AfterReturning(pointcut = "inOrderFeature()", returning = "order")
public void logOrder(JoinPoint call, OrderResponse order) { ... }   // runs only if the result is an OrderResponse

@AfterThrowing(pointcut = "inOrderFeature()", throwing = "failure")
public void logFailure(JoinPoint call, Exception failure) { ... }   // runs for every Exception
```

The parameter's type narrows the match: with `BusinessException failure`, the advice silently ignores every other exception. A `JoinPoint` (without "Proceeding") gives non-around advice the same information as in section 4, except `proceed()`.

### 7. Ordering advice

When several aspects advise the same method, Spring nests them like layers. `@Order` on the aspect class decides the nesting: the lower the value, the higher the *precedence*, and the further out the layer. So the aspect with the lowest value starts first and ends last:

```java
@Aspect @Component @Order(1) class MetricsAspect { ... }   // the outer layer
@Aspect @Component @Order(2) class AuditAspect { ... }     // the inner layer
```

```text
MetricsAspect starts → AuditAspect starts → method → AuditAspect ends → MetricsAspect ends
```

- **Without `@Order`, the order of two aspects is undefined.** It can change between runs or versions. Set it whenever the order matters.
- **Within one aspect**, Spring orders advice by type: `@Around`, `@Before`, `@After`, `@AfterReturning`, `@AfterThrowing`. `@After` works like a `finally` block around the rest, so it runs after `@AfterReturning` and `@AfterThrowing`. The Spring documentation recommends one advice method per join point and aspect; split the rest into ordered aspects.
- **Order matters for Spring's own aspects too.** The transaction advice has the lowest precedence, so it's the innermost layer. A retry aspect must run outside the transaction, because each attempt needs a fresh transaction. Give it an `@Order` value, which places it further out.

### 8. What a proxy can't intercept

The most common AOP bug in real code is a call that never reaches the proxy:

```java
@Service
public class CouponService {

    public void issueToAll(String code, List<Long> memberIds) {
        for (long memberId : memberIds) {
            issue(code, memberId);   // means this.issue(...): a plain call inside the target object
        }
    }

    public CouponIssue issue(String code, long memberId) { ... }   // advised, but not for these calls
}
```

```text
campaign service ──► proxy ──► CouponService.issueToAll()   ← advice runs for issueToAll
                                  └─ this.issue()            ← no proxy here: no advice
```

`this` inside the target is the target, not the proxy. This *self-invocation* skips every aspect: your own, `@Transactional`, `@Cacheable`, `@Async`. The same holds for:

| What | Why the advice doesn't run |
|---|---|
| a call on `this` | it never leaves the target |
| a `private` method | a subclass can't override it, and only the class itself can call it anyway |
| a `final` method | a subclass can't override it: the proxy runs it on its own empty fields |
| a `static` method | it belongs to the class, not to the bean |
| an object created with `new` | Spring never saw it, so it has no proxy |
| a call from the constructor or `@PostConstruct` | the proxy doesn't exist yet |

Fixes for self-invocation, best first:

1. **Move the method into another bean.** Then the call goes from one bean to another, through the proxy. It's usually the better design too: the outer method was a separate job, such as running a whole campaign.
2. **Inject the bean into itself**, with `@Lazy` so that the constructor doesn't need the bean before it exists. It works, but it confuses readers.
3. **`AopContext.currentProxy()`**, which returns the current proxy, and only if `@EnableAspectJAutoProxy(exposeProxy = true)` is set. Spring's documentation discourages it, because it ties your business code to Spring AOP.

(AspectJ's bytecode weaving has none of these limits, but it needs a special build step. Few projects use it.)

### 9. Spring's own aspects

Many Spring features are advice in a proxy. They follow every rule in this lesson, including the traps of section 8:

| Annotation | What the advice does | Module |
|---|---|---|
| `@Transactional` | starts a transaction, then commits or rolls back | S07 |
| `@Cacheable`, `@CacheEvict` | returns a cached result instead of calling the method; empties the cache | S10 |
| `@Async` | runs the method on another thread | S09 |
| `@Validated` on a class | validates method arguments | S04 |
| `@PreAuthorize` | checks the caller's permissions | S12 |
| `@Retryable`, `@ConcurrencyLimit` (new in Spring Framework 7) | retries failed calls; limits concurrent calls | S11 |

So a bean that uses any of them is a proxy, even without aspects of your own. `bean.getClass()` then returns the proxy class: comparing classes with `getClass()`, logging the class name, or reading class annotations with `getClass().getAnnotation(…)` all see the proxy. Spring's `AopUtils` tells you what you have:

```java
AopUtils.isAopProxy(bean)          // is it a Spring proxy?
AopUtils.isCglibProxy(bean)        // a generated subclass?
AopUtils.isJdkDynamicProxy(bean)   // a java.lang.reflect.Proxy?
AopUtils.getTargetClass(bean)      // CouponService.class, also for its proxy
```

In tests, `AopTestUtils.getTargetObject(bean)` returns the target object behind a proxy.

### 10. Testing aspects

- **Test aspects in a Spring context.** Without Spring there's no proxy, so no advice runs. `@SpringBootTest` checks the real wiring, and `ApplicationContextRunner` starts a small context with only the beans you list (S03-04's tests use it).
- **Control time.** An aspect that measures durations reads a `Ticker` bean that the tests replace with a fake one; an aspect that writes timestamps reads the `Clock`.
- **Test the pointcut both ways.** A method it must match, and the methods it must not match: other packages, other stereotypes, excluded names. Too broad and too narrow are both bugs.
- **Test the failure path.** The advice must also run when the method throws, and the caller must get the very same exception.

## TypeScript/NestJS → Spring

| TypeScript/NestJS | Spring | Watch out for |
|---|---|---|
| NestJS interceptor: `intercept(context, next)` with `next.handle()` | `@Around` advice with `call.proceed()` | NestJS interceptors wrap route handlers only. Spring advice wraps any matching method of any bean. |
| `@UseInterceptors(LoggingInterceptor)` on a controller or method | a pointcut: `@annotation(…)`, `within(…)`, `bean(…)` | In Spring, the pointcut lives in the aspect. A custom annotation makes it visible at the method again. |
| `app.useGlobalInterceptors(…)` | a broad pointcut such as `within(com.example.bookstore..*)` | Broad pointcuts also match helper beans you didn't think of. |
| A method decorator that replaces `descriptor.value` | `@Around` advice on `@annotation(…)` | A TypeScript decorator replaces the method itself, so `this.method()` runs it too. A Spring proxy only sees calls from outside the bean. |
| `Reflect.getMetadata(…)` to read a decorator's options | binding: `@annotation(timed)` with a parameter `Timed timed` | Annotations need `@Retention(RUNTIME)` to be visible at runtime. |
| JavaScript's `Proxy` (wraps any object) | JDK proxy (interfaces) or CGLIB proxy (subclass) | A CGLIB proxy can't override `final` or `private` methods. |
| NestJS guards | `@PreAuthorize` (S12), which is advice in a proxy like yours | Spring Security also checks each request earlier, in a servlet filter, before any controller runs. |

## In real projects

- **Use AOP for technical concerns** that apply across features: metrics, logging, auditing, transactions, security, caching, retries. Keep business rules in plain code that readers can follow: an aspect that changes prices or skips a check is invisible where the method is called. See [Spring layering](../../docs/handbook/spring-layering.md).
- **Use existing aspects first:** `@Transactional`, `@Cacheable`, Spring Security's `@PreAuthorize`, Spring Framework 7's `@Retryable`, and for metrics Micrometer's `@Timed` and `@Observed` (S14). Write your own aspect when your team has a concern that none of them covers, such as this bookstore's audit log. See [Logging and observability](../../docs/handbook/logging-and-observability.md).
- **Opt in with an annotation** you define, and match it with `@annotation(…)`. Readers see at the method that something happens, and refactoring can't change what's matched. Package-wide pointcuts are for rules that truly cover a whole package.
- **Keep aspects small, fast and stateless.** They run on every matching call, on every request thread. Inject collaborators through the constructor with `@RequiredArgsConstructor`, as in any bean. See [Lombok](../../docs/handbook/lombok.md).
- **Never swallow or wrap exceptions in advice.** Rethrow the same object, and measure or log the failed calls too: slow failures are the ones you most need to see.
- **Know the proxy traps:** self-invocation, `private` and `final` methods, and objects created with `new` get no advice. The same traps silently disable `@Transactional`. See [Transactions](../../docs/handbook/transactions.md).
- **Don't make beans or their public methods `final`.** CGLIB needs to subclass them. Kotlin classes are final by default, which is why Kotlin projects use the `kotlin-spring` compiler plugin.
- **Set `@Order` whenever two aspects can meet on one method**, for example a retry aspect, which must run outside the transaction.
- **Keep Spring Boot's CGLIB default** (`spring.aop.proxy-target-class=true`) unless you have a reason. With JDK proxies, injecting the class instead of the interface fails for every proxied bean that implements an interface. See [Configuration](../../docs/handbook/configuration.md).
- **Test aspects in a Spring context**, with a controlled clock or ticker, and test what the pointcut must not match. See [Testing Spring applications](../../docs/handbook/spring-testing.md).

## Exercises

### S03-01 · Your first aspect

**Build** · 30 min · [`TimingAspect.java`](src/main/java/com/example/bookstore/common/timing/TimingAspect.java) · [`BookService.java`](src/main/java/com/example/bookstore/book/BookService.java)

**Ticket: measure how long slow operations take.** Customers say the catalog search is slow around lunchtime, but nobody can say how slow. Operations wants an annotation: put `@Timed` on a method, and the duration of each call lands in `MethodTimings`, which their admin page reads. (In S14, Micrometer metrics take over this job.) `@Timed`, `MethodTimings`, `MethodTiming` and the `Ticker` are ready; the aspect isn't.

Acceptance criteria:

- Every call to a method annotated with `@Timed` adds a `MethodTiming` to `MethodTimings`, with the duration measured by the `Ticker` bean. Methods without `@Timed` aren't timed.
- The timing's name is the one given in `@Timed("…")`, or `ClassName.methodName` when none is given.
- A call that throws is recorded too, with `failed = true`.
- Callers can't tell that the aspect is there: they get the same result, or the very same exception object.
- The catalog search, `BookService.search`, is timed as `book.search`.

**Before you start:** lesson sections 1 to 4.

**You'll practice:** `@Aspect` and `@Around`, `ProceedingJoinPoint.proceed()`, binding an annotation with `@annotation(…)`, rethrowing exceptions unchanged, measuring with a ticker.

**Rules:** in the Javadoc of `TimingAspect` and `Timed`. The tests replace the ticker with a `FakeTicker`, and their sample bean moves it forward inside each method, so every duration is exact.

<details>
<summary>Hint 1</summary>

The stub's method already has the parameters that the advice needs. What's missing is the annotation that turns it into around advice, with a pointcut that binds the `timed` parameter (lesson section 4).
</details>

<details>
<summary>Hint 2</summary>

Read the ticker once before `call.proceed()`, and once more when the call has returned or thrown. A `try` with a `catch (Throwable failure)` that records the timing and then executes `throw failure;` handles the failure path.
</details>

<details>
<summary>Hint 3</summary>

`call.getSignature().getDeclaringType().getSimpleName()` and `call.getSignature().getName()` give you `BookService` and `search`.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Why does the aspect rethrow the caught exception instead of wrapping it? If another method of `BookService` called `search`, would that call be timed?

### S03-02 · Pointcuts and advice types

**Build** · 30 min · [`AuditAspect.java`](src/main/java/com/example/bookstore/common/audit/AuditAspect.java)

**Ticket: an audit log for coupon operations.** Marketing and finance want to know who got which coupon, and which attempts failed and why. Every operation of the coupon services must leave an `AuditEntry` in the `AuditLog`. The log and its entries are ready; write the aspect that feeds them, using the narrow advice types.

Acceptance criteria:

- Every `public` method of the `@Service` classes in `com.example.bookstore.coupon` is audited, except the `find…` methods, which only read.
- A call that returns normally adds a `SUCCEEDED` entry. A call that throws adds a `FAILED` entry with the error: the error code of a `BusinessException` (such as `COUPON_EXPIRED`), or the class name of any other exception.
- Exactly one entry per call, with the time from the application's `Clock`. The caller gets the method's result or exception as before.
- Not audited: `CouponService.findCoupon`, the `CouponRepository` (a `@Repository`), methods that aren't `public`, and services in other packages, such as `BookService`.

**Before you start:** lesson sections 5 and 6.

**You'll practice:** `within`, `@within` and `execution` pointcuts, combining named `@Pointcut`s with `&&` and `!`, `@AfterReturning`, `@AfterThrowing` with a bound exception, `JoinPoint`.

**Rules:** in the Javadoc of `AuditAspect` and `AuditEntry`. The tests use the seed coupons: AUTUMN3000 is valid and was issued to members 1–10, EXPIRED2000 expired in August 2026. `CouponAuditScopeTest` adds a small coupon service of its own, with a method that isn't public.

<details>
<summary>Hint 1</summary>

Write one small named pointcut per condition (package, stereotype, public method, `find…` name), then combine them into one, such as `auditedOperation()`. Each piece is easy to read and to check on its own.
</details>

<details>
<summary>Hint 2</summary>

`within(com.example.bookstore.coupon..*)` matches every class in the package, `@within(org.springframework.stereotype.Service)` only classes annotated with `@Service`. You need both.
</details>

<details>
<summary>Hint 3</summary>

`@AfterThrowing(pointcut = "auditedOperation()", throwing = "failure")` passes the exception to the parameter named `failure`. `AuditEntry.succeeded` and `AuditEntry.failed` build the entries; `call.getArgs()` gives the arguments.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Which of your conditions would let a new class in the coupon package be audited without anyone deciding so? What would go wrong if you wrote the success entry in an `@After` advice?

### S03-03 · The self-invocation trap

**Fix** · 30 min · [`CouponService.java`](src/main/java/com/example/bookstore/coupon/CouponService.java) · [`CouponCampaignService.java`](src/main/java/com/example/bookstore/coupon/CouponCampaignService.java)

This ticket uses the audit log of S03-02, so finish S03-02 first.

**Bug: the audit log misses coupons issued in bulk.** Marketing ran the AUTUMN3000 campaign for members 11, 12 and 13, and all three got the coupon. When finance checked the audit log, they found no `CouponService.issue` entry for any of them, only one `CouponService.issueToAll` entry and one `CouponCampaignService.issueToAll` entry. When marketing gives a single member a coupon, its `CouponService.issue` entry is there. Finance needs one entry per member, including the members who were skipped and why.

Expected behavior: the Javadoc of `CouponCampaignService.issueToAll`. Each failing test shows one symptom. Fix the code, not the tests.

**Before you start:** lesson section 8.

**You'll practice:** recognizing self-invocation, moving a method to another bean so the call goes through the proxy, keeping the behavior while you restructure (skipping members, order, duplicates).

<details>
<summary>Hint 1</summary>

Set a breakpoint in `AuditAspect.recordSuccess` and run the first test in the debugger. Which calls stop there, and which `issue` calls don't?
</details>

<details>
<summary>Hint 2</summary>

Inside `CouponService.issueToAll`, the call `issue(code, memberId)` means `this.issue(code, memberId)`. What is `this` there: the proxy or the target?
</details>

<details>
<summary>Hint 3</summary>

`CouponCampaignService` already has `CouponService` injected, and what Spring injected there is the proxy. Which part of the work could live in the campaign service instead?
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Why would moving the loop into a helper object created with `new CampaignIssuer(this)` not fix the bug? Which other Spring features would the original code have broken in the same way?

### S03-04 · Predict: proxies

**Predict** · 30 min · [`ProxyPuzzles.java`](src/main/java/com/example/bookstore/pricing/ProxyPuzzles.java) → answers in [`Answers.java`](src/main/java/com/example/bookstore/pricing/Answers.java)

Fifteen questions about what Spring's proxies really do: which beans get a proxy and of which kind, which calls the advice sees, and in which order advice runs. The tests start a small context with Spring Boot's AOP auto-configuration, as the bookstore uses, and check each answer against it. Every one of these facts has caused real bugs, mostly in `@Transactional` code.

**Before you start:** lesson sections 2, 3, 7 and 8.

**How:** read `ProxyPuzzles` from top to bottom without running anything: the beans, the aspects and the `Setup` class that registers them. Write your prediction in the matching constant in `Answers`, then run `ProxyPuzzlesTest`. Each failing test tells you what you predicted and what really happens.

<details>
<summary>Explanations (open after you've run the tests)</summary>

**Which object you get**

1. **`priceCalculator` is a CGLIB proxy.** `CallRecorder`, `AuditTrail` and `Metrics` advise it, and `PriceCalculator` implements no interface, so only a generated subclass can stand in for it.
2. **`shippingFees` is a CGLIB proxy too.** `FlatShippingFees` implements `ShippingFees`, so plain Spring would create a JDK proxy. But Spring Boot sets `spring.aop.proxy-target-class=true`, which makes every proxy a subclass.
3. **With `spring.aop.proxy-target-class=false`, `shippingFees` is a JDK dynamic proxy:** a `java.lang.reflect.Proxy` that implements `ShippingFees`, like the ones you wrote in J09-03.
4. **`taxRates` is not a proxy.** No pointcut matches any of its methods, so Spring leaves the object alone. A proxy only exists where advice applies.
5. **`priceCalculator.getClass() == PriceCalculator.class` is `false`.** Its class is the generated subclass, with a name like `PriceCalculator$$SpringCGLIB$$0`. Code that compares classes, or reads class annotations through `getClass()`, sees the proxy. `AopUtils.getTargetClass` returns `PriceCalculator`.
6. **`priceCalculator instanceof PriceCalculator` is `true`.** An object of a subclass is an instance of its superclass. That's why injecting the class works with CGLIB proxies.
7. **The context doesn't start.** With JDK proxies, the `shippingFees` bean is a `$Proxy` that implements `ShippingFees` but isn't a `FlatShippingFees`, so Spring can't pass it to the constructor of `ShippingQuotes`. Inject interfaces, or keep Spring Boot's default.

**Which calls the advice sees**

8. **`linePrice` is not recorded when `orderTotal` calls it.** The recorder sees `orderTotal`, because the call came through the proxy. Inside, `linePrice(…)` means `this.linePrice(…)`, a plain Java call within the target: self-invocation (lesson section 8).
9. **`roundDownToHundreds` is not recorded.** A subclass can't override a `private` method, and only the target itself can call it, from inside.
10. **`withVat` is not recorded.** A `final` method can't be overridden, so the proxy has no place to put the advice.
11. **`withVat(10_000)` returns `10000`, not `11000`.** Since the proxy can't override `withVat`, the call runs the method on the proxy object itself. Spring created that object without running its constructor (lesson section 3), so its own `vatPercent` field is `0`. The real `vatPercent = 10` lives in the target, which this call never reaches. With an object field instead of an `int`, the same mistake throws a `NullPointerException`. So a `final` method on a proxied bean is always a bug, even while it seems to work.

**In which order the advice runs**

12. **`Metrics` starts first.** It has `@Order(1)`, the lower value, so it has the higher precedence and wraps around `AuditTrail` (`@Order(2)`).
13. **`AuditTrail` ends first.** The inner layer finishes before the outer one, like nested `try`/`finally` blocks: `Metrics starts`, `AuditTrail starts`, `linePrice`, `AuditTrail ends`, `Metrics ends`.
14. **`@Around` runs first.** Within one aspect, Spring orders advice by type: `@Around`, `@Before`, `@After`, `@AfterReturning`, `@AfterThrowing`. So `around starts` comes before `before`.
15. **`@AfterReturning` runs first after the method.** `@After` behaves like a `finally` block around the advice after it in that list, so it runs after `@AfterReturning`. The full sequence is `around starts`, `before`, `feeFor`, `afterReturning`, `after`, `around ends`.
</details>

## Review questions

1. What is a cross-cutting concern? Name three from a typical backend.
2. How does Spring add advice to a bean, and what do other beans get injected?
3. When does Spring Boot use a JDK dynamic proxy, and when a CGLIB proxy?
4. A `@Transactional` method calls a method of the same class annotated with `@Transactional(propagation = REQUIRES_NEW)`. Does the inner method get a new transaction? Why?
5. Why can't Spring AOP advise a `private` method? And a `final` one?
6. Which advice type can change the result that the caller gets? Why can't an `@AfterThrowing` advice keep the exception from reaching the caller?
7. Two aspects advise the same method, one with `@Order(10)` and one with `@Order(20)`. Which one starts first, and which one finishes first?
8. What does `@annotation(timed)` with a parameter `Timed timed` give you that `@annotation(com.example.bookstore.common.timing.Timed)` doesn't?
9. An aspect's pointcut is `within(com.example.bookstore..*)`. What can go wrong?
10. `log.info("{}", bean.getClass().getSimpleName())` prints `CouponService$$SpringCGLIB$$0`. Why, and how do you get `CouponService`?
11. A TypeScript method decorator and a Spring aspect both wrap a method. Which one also runs when the class calls the method on `this`, and why?

<details>
<summary>Answers</summary>

1. A requirement that applies to many methods across features, such as timing, logging, auditing, transactions, security checks, caching or retries.
2. At startup, a bean post-processor checks each bean against the aspects' pointcuts. If any pointcut matches one of its methods, it wraps the bean in a proxy, and the container hands out the proxy. Other beans get the proxy injected; it runs the matching advice and calls the target.
3. With its defaults, Spring Boot always uses CGLIB. It uses JDK proxies only if you set `spring.aop.proxy-target-class=false`, and then only for beans that implement an interface; beans without one still get CGLIB proxies.
4. No. The call goes from the target to itself through `this`, so it never reaches the proxy, and the transaction advice doesn't run. The inner method runs in the outer method's transaction. Move it to another bean.
5. A CGLIB proxy works by overriding methods in a generated subclass. A subclass can't override a `private` method, and only the class itself can call it, so the call never comes from the proxy. A `final` method can't be overridden either; the proxy runs it on its own empty fields.
6. Only `@Around`: it calls the method itself, so it can change the arguments, return another result, or catch the exception. `@AfterThrowing` runs after the method has already thrown, and Spring rethrows the exception when the advice ends. The advice can only replace it, by throwing another one.
7. The aspect with `@Order(10)` has the higher precedence: it starts first and finishes last. The one with `@Order(20)` runs inside it and finishes first.
8. The annotation object itself, passed to the advice, so it can read attributes such as `timed.value()` without reflection. Both forms match the same methods.
9. It matches every bean in the application, including helpers that the aspect itself calls, which can make the advice run again inside itself, forever. It also silently includes every class added later. Narrow it, for example with `@within(org.springframework.stereotype.Service)` or a custom annotation.
10. The bean is a CGLIB proxy, a generated subclass of `CouponService`. `AopUtils.getTargetClass(bean)` or `ClassUtils.getUserClass(bean)` returns `CouponService`.
11. The TypeScript decorator. It replaces the method on the class itself, so every call runs the wrapper, including `this.method()`. A Spring aspect lives in a separate proxy object, and a call on `this` inside the target never passes through it.
</details>

## Further reading

- Spring Framework reference: [Aspect Oriented Programming with Spring](https://docs.spring.io/spring-framework/reference/core/aop.html), especially [Declaring a pointcut](https://docs.spring.io/spring-framework/reference/core/aop/ataspectj/pointcuts.html), [Declaring advice](https://docs.spring.io/spring-framework/reference/core/aop/ataspectj/advice.html) (binding values, advice ordering) and [Proxying mechanisms](https://docs.spring.io/spring-framework/reference/core/aop/proxying.html) (self-invocation and its fixes)
- Spring Boot reference: [Aspect-Oriented Programming](https://docs.spring.io/spring-boot/reference/features/aop.html) (the auto-configuration and `spring.aop.proxy-target-class`)
- Spring Framework reference: [Resilience features](https://docs.spring.io/spring-framework/reference/core/resilience.html): `@Retryable` and `@ConcurrencyLimit`, two of Spring's own aspects
- Micrometer: [The `@Timed` annotation](https://docs.micrometer.io/micrometer/reference/concepts/timers.html), the production version of S03-01
- NestJS: [Interceptors](https://docs.nestjs.com/interceptors), to compare with `@Around` advice
- API docs: [`AopUtils`](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/aop/support/AopUtils.html) and [`ProceedingJoinPoint`](https://javadoc.io/doc/org.aspectj/aspectjrt/latest/org/aspectj/lang/ProceedingJoinPoint.html)
