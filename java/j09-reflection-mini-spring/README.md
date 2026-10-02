# J09 · Reflection, annotations and proxies: build a mini Spring

⏱ 5 sessions · week 6 · 4 exercises

Spring can seem like magic. You write `@Service` and a constructor, and an object appears with its dependencies filled in. You write `@Transactional`, and commits and rollbacks happen around your method. This module shows how that works. It covers the three JDK features that Spring is built on: *reflection* (code that inspects and calls other code at runtime), *annotations* (metadata that code can read), and *dynamic proxies* (objects generated at runtime that stand in for other objects). In the capstone you build a small dependency-injection container yourself. After that, Spring's startup errors and its proxy traps will make sense, and S01 (IoC and DI) and S03 (AOP) will feel like a recap.

## Lesson

### 1. Class objects: where reflection starts

At runtime, every type has one `Class` object that describes it. You get it in three ways:

```java
Class<String> fromType = String.class;
Class<?> fromObject = "text".getClass();
Class<?> fromName = Class.forName("java.util.ArrayList");   // throws ClassNotFoundException

fromObject.getName();                          // "java.lang.String"
fromObject.getSimpleName();                    // "String"
List.class.isInterface();                      // true
Record.class.isAssignableFrom(Point.class);    // true: a Point can be used where a Record is expected
Number.class.isInstance(42);                   // true: the same as 42 instanceof Number
```

`isAssignableFrom` reads backwards: `A.isAssignableFrom(B)` asks "can a `B` be used where an `A` is needed?".

TypeScript types disappear when the code is compiled to JavaScript. Java classes stay, so a program can ask an object what it is. But Java erases *generic type arguments*: at runtime, `List<String>` and `List<Integer>` are the same `List.class`.

### 2. Fields, methods and constructors

A `Class` object lists its members as `Field`, `Method` and `Constructor` objects, which you can read and call:

```java
Method deposit = Account.class.getMethod("deposit", long.class);    // name and parameter types
deposit.invoke(account, 500L);                                      // account.deposit(500L)

Field balance = Account.class.getDeclaredField("balanceCents");
balance.setAccessible(true);                                        // allows access to a private field
long value = (long) balance.get(account);                           // values come back as objects

Constructor<Account> constructor = Account.class.getConstructor(String.class);
Account created = constructor.newInstance("jun");                   // new Account("jun")
```

Each kind of member has two lookups:

| | `getFields()`, `getMethods()`, `getConstructors()` | `getDeclaredFields()`, `getDeclaredMethods()`, `getDeclaredConstructors()` |
|---|---|---|
| Access levels | public only | all: private, package-private, protected, public |
| Inherited members | included (fields and methods) | not included: only this class |

More details that matter in practice:

- `Modifier.isStatic(field.getModifiers())` tells static members apart from instance members. There are `isPrivate`, `isFinal` and so on, too.
- Primitive types have `Class` objects as well: `long.class`. Values always travel as objects, so a `long` arrives as a `Long`. And `long.class.isInstance(3L)` is `false`, because `3L` became a `Long`: compare with the wrapper class instead.
- The Javadoc doesn't promise any order for `getDeclaredFields()` or `getDeclaredMethods()`. HotSpot, the JVM you use, returns fields in declaration order, and many tools rely on that. Methods often come back in a different order than in the source code, so sort them yourself when the order matters.
- The compiler adds members you didn't write, called *synthetic* members. An inner class that uses its outer object gets a field `this$0`. A class that implements `Comparable<Ticket>` gets a *bridge method* `compareTo(Object)` next to your `compareTo(Ticket)`. Frameworks skip them with `isSynthetic()` and `isBridge()`.
- Almost everything throws a checked `ReflectiveOperationException`: `NoSuchMethodException`, `IllegalAccessException`, `InstantiationException` (for an abstract class), and `InvocationTargetException`. The last one wraps whatever the called method or constructor threw. Use `getCause()` to get the original exception.

### 3. Records through reflection

`getRecordComponents()` returns a record's components in the order of the record header, and that order *is* guaranteed. Each component knows its name, its type and its accessor method:

```java
for (RecordComponent component : Point.class.getRecordComponents()) {
    Object value = component.getAccessor().invoke(point);
    System.out.println(component.getName() + " = " + value);   // x = 1, then y = 2
}
Constructor<Point> canonical = Point.class.getDeclaredConstructor(int.class, int.class);
```

Jackson turns records into JSON this way, and Spring binds configuration properties to records through the canonical constructor. Records stay immutable even here: `Field.set` on a record's field throws `IllegalAccessException`, even after `setAccessible(true)`.

### 4. `setAccessible` and the module system

`setAccessible(true)` turns off Java's access check for one member, so you can read a private field or call a private method. It works for classes on the class path (your own code) and for packages that a module *opens* to you.

Since Java 17, the JDK's own internals are closed (JEP 403). `String.class.getDeclaredField("value").setAccessible(true)` throws `InaccessibleObjectException`. An application can reopen a package with a command-line flag such as `--add-opens java.base/java.lang=ALL-UNNAMED`, but that only hides the problem. `trySetAccessible()` returns `false` instead of throwing. If your own application uses modules (`module-info.java`), it must open its packages to frameworks that reflect on them, for example `opens com.example.shop;`.

### 5. Annotations: declaring and reading them

An annotation is metadata on a declaration. You declare one with `@interface`:

```java
@Retention(RetentionPolicy.RUNTIME)   // keep it until runtime, so reflection can read it
@Target(ElementType.FIELD)            // allowed on fields only
public @interface MaxLength {
    int value();                      // an element; the name "value" allows @MaxLength(20)
}

public @interface Range {
    long min() default Long.MIN_VALUE;   // with a default, the element is optional: @Range(min = 0)
    long max() default Long.MAX_VALUE;
}
```

*Retention* says how long the annotation lives:

| `RetentionPolicy` | Kept until | Example |
|---|---|---|
| `SOURCE` | compilation; the compiler drops it | `@Override` |
| `CLASS` (the default) | the `.class` file, but reflection can't see it | rarely what you want |
| `RUNTIME` | runtime: reflection can read it | `@Component`, `@Transactional` |

*Target* says where the annotation may appear: `TYPE` (classes, interfaces, enums, records, annotations), `FIELD`, `METHOD`, `PARAMETER`, `CONSTRUCTOR`, `RECORD_COMPONENT`, `TYPE_USE` and a few more. Without `@Target`, it may appear on almost any declaration. Element types are limited to primitives, `String`, `Class`, enums, other annotations, and arrays of these, and their values must be constants.

Reading an annotation returns an object whose methods give you the element values:

```java
MaxLength maxLength = field.getAnnotation(MaxLength.class);   // null if the field doesn't have it
if (maxLength != null && text.length() > maxLength.value()) { ... }
boolean isTransactional = method.isAnnotationPresent(Transactional.class);
```

An annotation does nothing by itself. Some code must look for it: the compiler, an annotation processor at build time, or a framework at runtime.

Records need one more rule. When you write an annotation on a record component, the compiler copies it to the places that the annotation's `@Target` allows (JLS 8.10.3):

| `@Target` of the annotation | Where it ends up |
|---|---|
| `FIELD` | the record's private field |
| `METHOD` | the accessor method |
| `PARAMETER` | the parameter of the canonical constructor |
| `RECORD_COMPONENT` | the record component, as returned by `getRecordComponents()` |
| `TYPE_USE` | the type, in all of the places above |
| no `@Target` | every one of the declarations above |

So a `@Target(FIELD)` annotation on a record component is visible through `getDeclaredFields()`, but not through `getRecordComponents()`.

### 6. Meta-annotations: why `@Service` is a `@Component`

A *meta-annotation* is an annotation on an annotation type. Spring's `@Service` is annotated with `@Component`, so a scan for components finds `@Service` classes too:

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Component                                  // every @Service is also a @Component
public @interface Service {
}

static boolean isComponent(Class<?> type) {
    if (type.isAnnotationPresent(Component.class)) {
        return true;
    }
    for (Annotation annotation : type.getAnnotations()) {
        if (annotation.annotationType().isAnnotationPresent(Component.class)) {
            return true;                    // found through a meta-annotation
        }
    }
    return false;
}
```

Spring calls `@Service`, `@Repository` and `@Controller` *stereotype* annotations, and searches any number of levels deep (`@RestController` carries `@Controller`, which carries `@Component`). A deep search has to remember the annotation types it has already seen: `@Documented` is annotated with `@Documented`, so a naive recursion never ends.

### 7. Dynamic proxies

`java.lang.reflect.Proxy` creates, at runtime, an object that implements interfaces of your choice. Every call on it goes to one method of your *invocation handler*:

```java
Greeter target = name -> "Hello, " + name;

InvocationHandler handler = (proxy, method, args) -> {
    System.out.println("calling " + method.getName());
    try {
        return method.invoke(target, args);   // forward the call
    } catch (InvocationTargetException e) {
        throw e.getCause();                   // rethrow what the target threw
    }
};
Greeter greeter = (Greeter) Proxy.newProxyInstance(
        Greeter.class.getClassLoader(), new Class<?>[] {Greeter.class}, handler);

greeter.greet("mina");   // prints "calling greet" and returns "Hello, mina"
```

The rules you need:

- A JDK proxy can only implement interfaces. To stand in for a class, a library must generate a subclass at runtime (CGLIB or ByteBuddy).
- `args` is `null` for a method without parameters. `method.invoke(target, null)` accepts that.
- `method` is the *interface's* method. An annotation on the implementing class's method isn't on it. Look that method up with `target.getClass().getMethod(method.getName(), method.getParameterTypes())`.
- Unwrap `InvocationTargetException`. If the handler throws a checked exception that the interface method doesn't declare, the caller gets an `UndeclaredThrowableException` instead.
- `equals`, `hashCode` and `toString` go through the handler too, with the `Method` objects of `java.lang.Object`. Decide what they should do. Forwarding `equals` to the target breaks `proxy.equals(proxy)`, because the target doesn't know its proxy.

### 8. How Spring uses all three

**Component scanning.** `@SpringBootApplication` includes `@ComponentScan`. At startup, Spring looks at the classes in your application's package and below, keeps those annotated with `@Component` or a stereotype, and registers a *bean definition* for each one. It reads the class files directly instead of loading every class, which keeps startup fast.

**Constructor injection.** For each bean, Spring chooses a constructor: the only one, or the one annotated `@Autowired`. It resolves every parameter *by type*. One candidate is used directly; with several, `@Primary` or `@Qualifier` decides; with none, startup fails with `NoSuchBeanDefinitionException`. Spring creates dependencies first, creates each singleton once, and then calls its `@PostConstruct` method. A cycle of constructor dependencies fails with `BeanCurrentlyInCreationException`, and since Spring Boot 2.6 cycles are refused by default.

**`@Transactional` proxies.** For a bean with `@Transactional` methods, Spring hands out a proxy instead of the bean itself. The proxy starts a transaction, calls the bean, and commits or rolls back. The default rule comes from older Java EE standards: roll back on `RuntimeException` and `Error`, *commit* on checked exceptions. `@Transactional(rollbackFor = Exception.class)` changes that. `@Cacheable`, `@Async` and `@PreAuthorize` work through proxies in the same way.

**Self-invocation.** A call from inside the bean to its own method doesn't go through the proxy:

```text
controller ──► proxy ──► orderService.placeOrder() ──► this.saveOrder()
               (starts a transaction)                    └─ a plain call on the bean: no proxy, so
                                                            @Transactional on saveOrder() does nothing
```

The usual fixes: move the method into another bean, or put `@Transactional` on the outer method.

**CGLIB.** JDK proxies need an interface. For classes, Spring generates a subclass at runtime with CGLIB, and Spring Boot uses CGLIB for all its proxies by default. A subclass can't override a `final` class or a `final` or `private` method, so those calls can't be intercepted. This is also why Kotlin projects need the `kotlin-spring` compiler plugin: Kotlin classes are final unless you say otherwise.

### 9. The cost and limits of reflection

- **Speed.** A reflective call is slower than a direct one, although modern JDKs have narrowed the gap (JEP 416). Frameworks pay most of the cost once, at startup, and keep the `Method` objects they found.
- **No compiler checks.** `getMethod("findByEmial")` compiles fine and fails at runtime. Renaming a method in your IDE doesn't update the strings that name it.
- **Encapsulation.** Reading private fields bypasses the class's own rules, and the module system blocks it for code that isn't yours.
- **Native images.** GraalVM native images need to know about every reflective access when they are built. Spring's AOT processing generates plain Java code for bean creation instead.

Reflection belongs in frameworks, libraries and test tools, almost never in business code.

## TypeScript → Java

| TypeScript | Java | Watch out for |
|---|---|---|
| types vanish at runtime | every class has a `Class` object at runtime | generic type arguments vanish in Java too: `List<String>` is a plain `List` at runtime |
| `obj.constructor.name` | `obj.getClass().getSimpleName()` | |
| `obj[key]`, `Reflect.get(obj, key)` | `field.get(obj)` after `setAccessible(true)` | checked exceptions; the module system blocks JDK internals |
| `Reflect.apply(fn, obj, args)` | `method.invoke(obj, args)` | exceptions arrive wrapped in `InvocationTargetException` |
| decorators (`@Injectable()`) are functions that run when the class is defined | annotations are passive data | nothing happens until some code looks for them |
| `reflect-metadata` and `emitDecoratorMetadata` record constructor parameter types | `constructor.getParameterTypes()` always works | forget `@Retention(RUNTIME)` and reflection can't see your annotation |
| `new Proxy(target, handler)` intercepts any property access on any object | `Proxy.newProxyInstance` intercepts method calls on interfaces only | classes need a generated subclass (CGLIB, ByteBuddy) |
| NestJS or Angular dependency injection | Spring's `ApplicationContext` | the same ideas: a provider is a bean, `@Injectable()` is `@Component` |

## In real projects

- Use constructor injection with `final` fields. A class with one constructor needs no annotation at all. See [Java style](../../docs/handbook/java-style.md).
- Keep reflection in frameworks and test tools. Business code calls methods directly, so the compiler can check it. See [Java style](../../docs/handbook/java-style.md).
- Validate request bodies with Jakarta Bean Validation (`@NotBlank`, `@Size`, `@Min`, `@Max` and `@Valid`); J09-02 builds a tiny version of it. Turn the violations into a clear error response. See [Exceptions](../../docs/handbook/exceptions.md).
- Never rely on `@Transactional` for a call on `this`, and keep transactional methods public and not `final`. Remember the rollback rule for checked exceptions. See [Exceptions](../../docs/handbook/exceptions.md).
- Use records for DTOs and configuration properties: Jackson and Spring read them through their components and canonical constructor. See [Immutability](../../docs/handbook/immutability.md).
- Don't test private methods through reflection. Test the public behavior instead. See [Testing](../../docs/handbook/testing.md).

## Exercises

### J09-01 · Reflection

**Build** · 30 min · [`Inspector.java`](src/main/java/practice/j09/ex01/Inspector.java)

You build the kind of reflection toolkit that frameworks, test libraries and debuggers carry inside: read every field of an object, create an object from a class and some arguments, and call a method by its name. Spring, Jackson and Mockito do each of these things thousands of times while your application starts.

**You'll practice:** `getDeclaredFields`, `Modifier`, `setAccessible`, `getConstructors`, `getMethods`, `Constructor.newInstance`, `Method.invoke`, unwrapping `InvocationTargetException`, synthetic fields and bridge methods.

**Rules:** in the Javadoc of `Inspector`.

<details>
<summary>Hint 1</summary>

`newInstance` and `invoke` choose the same way. Write one helper that checks whether a list of parameter types fits the arguments, and one that returns the single fitting candidate or throws. The given helper `boxed(long.class)` returns `Long.class`, so `boxed(parameterType).isInstance(argument)` works for primitive parameters too.
</details>

<details>
<summary>Hint 2</summary>

`Constructor` and `Method` both extend `Executable`, which has `getParameterTypes()`. A generic helper such as `<E extends Executable> E onlyFit(List<E> candidates, Object[] args)` works for both.
</details>

<details>
<summary>Hint 3</summary>

To unwrap, catch `InvocationTargetException` and look at `e.getCause()`. A `switch` with type patterns handles the three cases (`RuntimeException`, `Error`, anything else) in a few lines.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Which of your methods would break silently if someone renamed a method in `Account`? Why can you call `setAccessible(true)` on your own private fields, but not on `String`'s?

### J09-02 · Custom annotations

**Build** · 30 min · [`Validator.java`](src/main/java/practice/j09/ex02/Validator.java), with the annotations [`NotBlank.java`](src/main/java/practice/j09/ex02/NotBlank.java), [`MaxLength.java`](src/main/java/practice/j09/ex02/MaxLength.java) and [`Range.java`](src/main/java/practice/j09/ex02/Range.java)

You finish three constraint annotations and write a validator that reads them at runtime, for classes and records alike. It's a small version of Jakarta Bean Validation, which Spring uses to check request bodies marked `@Valid`.

**You'll practice:** `@Retention` and `@Target`, annotation elements and defaults, `getAnnotation`, how annotations on record components reach the fields, reading private fields, sorting with a `Comparator`.

**Rules:** in the Javadoc of `Validator` and of each annotation. Start with the annotations: the first Basic tests check their `@Retention` and `@Target`.

<details>
<summary>Hint 1</summary>

Without `@Retention(RetentionPolicy.RUNTIME)`, the annotation is in the class file, but `getAnnotation` returns `null`. Every behavior test fails until all three annotations have it.
</details>

<details>
<summary>Hint 2</summary>

With `@Target(ElementType.FIELD)`, an annotation written on a record component lands on the record's private field (lesson section 5). So one loop over `getDeclaredFields()` handles records and classes.
</details>

<details>
<summary>Hint 3</summary>

A type pattern skips `null` for you: `value instanceof String text && text.length() > max` is `false` for a `null` value.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: What would your validator find if the annotations had `@Target(ElementType.RECORD_COMPONENT)` instead? Why does the type check use the field's declared type rather than the value's class?

### J09-03 · Dynamic proxies

**Build** · 30 min · [`Proxies.java`](src/main/java/practice/j09/ex03/Proxies.java)

You write two proxies with `java.lang.reflect.Proxy`: one that measures every call, and one that wraps `@Transactional` methods in begin, commit and rollback with Spring's rules. This is how Spring's `@Transactional` really works, including its best-known trap: a call from a bean to its own method skips the proxy.

**You'll practice:** `Proxy.newProxyInstance`, `InvocationHandler`, `Method.invoke` and unwrapping, `try`/`finally` for measurements, handling `RuntimeException` and `Error` apart from checked exceptions, finding an annotation on the implementation class, and `equals`, `hashCode` and `toString` on proxies.

**Rules:** in the Javadoc of `Proxies`. The ticker is a fake clock in the tests, so every timing is exact.

<details>
<summary>Hint 1</summary>

Write the parts that both proxies share first: a helper that forwards a call to the target and rethrows `e.getCause()`, one for `equals`, `hashCode` and `toString`, and one that creates the proxy.
</details>

<details>
<summary>Hint 2</summary>

In the transactional handler, `catch (RuntimeException | Error e)` must come before `catch (Throwable checked)`. Throwing a `Throwable` from the handler is allowed, because `InvocationHandler.invoke` declares `throws Throwable`.
</details>

<details>
<summary>Hint 3</summary>

The `Method` your handler receives belongs to the interface. To see an annotation on the implementation, look the method up on the target's class: `target.getClass().getMethod(method.getName(), method.getParameterTypes())`.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Why does the self-invocation test pass without any work from you? How would you fix self-invocation in a Spring application?

### J09-04 · Capstone: a mini IoC container

**Build** · 60 min · [`MiniContainer.java`](src/main/java/practice/j09/ex04/MiniContainer.java)

You build the heart of Spring: an *IoC container* (inversion of control: the container creates and wires your objects, instead of your code calling `new`). It finds component classes, chooses their constructors, resolves dependencies by type, creates every bean once in dependency order, detects cycles and runs `@Init` methods. Afterwards, you'll know where each of Spring's startup errors comes from.

**You'll practice:** everything from J09-01 to J09-03 together: annotations and meta-annotations, constructors through reflection, unwrapping exceptions, and a recursive depth-first walk that produces a topological order.

**Given code:** the annotations (`@Component`, `@Service`, `@Inject`, `@Primary`, `@Init`, `@Timed`), the exceptions with ready-made messages, and `ClassScanner`. The test fixtures are small component sets in `src/test/java/practice/j09/ex04/sample`.

**Rules:** in the Javadoc of `MiniContainer`. The stub suggests one helper per step; you may change them, because the tests only use the public methods.

**Steps:** the Basic tests start with their step number. Run the tests after each step.

1. **Register and create (15 min).** Write `isComponent`, the checks in `of()` and `injectionConstructor`. Then write a first `getOrCreate` that only handles constructors without parameters, the constructor loop that calls it for every class, and a `getBean` that looks the class up in `beans`.
2. **Resolve by type (10 min).** Write `resolve`: the candidates are the registered classes for which `type.isAssignableFrom(candidate)` is true. Handle `@Primary` and the two exceptions, and use `resolve` in `getBean`.
3. **Dependencies, singletons and cycles (20 min).** Complete `getOrCreate`: return an existing bean, detect a cycle with `inCreation`, create each constructor parameter's bean first (recursion), create the bean, and store it. Add `creationOrder()`.
4. **`@Init` and scanning (10 min).** Write `runInit` and call it right after the constructor. Write `scan()` with `ClassScanner.findClasses` and `isComponent`.
5. **Optional: `@Timed` proxies.** When a bean's class has a public `@Timed` method and implements at least one interface, store a JDK proxy for all its interfaces instead of the bean. The proxy forwards every call and records a `TimedCall` for each call of a `@Timed` method (look for the annotation on the bean's class, as in J09-03). Then implement `timedCalls()`. Until `timedCalls()` stops throwing its `TODO` exception, the six step 5 tests are skipped, so you can finish the exercise without this step.

<details>
<summary>Hint 1</summary>

For step 1, `getBean` can return `type.cast(beans.get(type))`. `Class.cast` checks the type at runtime, so you need no unchecked cast. In step 2, replace `type` with `resolve(type)`.
</details>

<details>
<summary>Hint 2</summary>

`inCreation.add(componentClass)` returns `false` when the class is already being created further up: that's a cycle. The path is the part of `inCreation` from that class to the end, plus the class once more. `List.copyOf(inCreation)`, `indexOf` and `subList` help.
</details>

<details>
<summary>Hint 3</summary>

When a bean is finished, take its class out of `inCreation` again (`removeLast()`). Otherwise a finished bean shows up in the path of a later cycle.
</details>

<details>
<summary>Hint 4</summary>

Look for `@Init` with `getMethods()`: it returns the public methods, inherited ones included. Wrap the exception of a failing constructor or `@Init` method like this: `new BeanCreationException(type, reason, e.getCause())`.
</details>

**Done?** Compare with the reference on the `solutions` branch, then ask yourself: Where exactly does your code produce the topological order? Spring can sometimes resolve a cycle when beans use field injection, but never with constructor injection. Why?

## Review questions

1. What's the difference between `getMethods()` and `getDeclaredMethods()`?
2. An annotation is on a field, but `field.getAnnotation(...)` returns `null`. Name two possible causes.
3. Why does `Method.invoke` wrap exceptions in `InvocationTargetException`, and how do you get the original?
4. A `@Target(FIELD)` annotation is written on a record component. Where can reflection find it, and where not?
5. How does Spring know that a class annotated `@Service` is a component?
6. Why doesn't a `@Transactional` method start a transaction when another method of the same bean calls it?
7. Which exceptions make Spring roll back a transaction by default, and which don't?
8. When does Spring use CGLIB instead of JDK proxies, and which methods can't a CGLIB proxy intercept?
9. Why does `proxy.equals(proxy)` return `false` if the handler forwards `equals` to the target?
10. Why can a container create beans in dependency order only when there is no cycle?
11. Name three costs of reflection.

<details>
<summary>Answers</summary>

1. `getMethods()` returns the public methods, inherited ones included. `getDeclaredMethods()` returns the methods of every access level, but only those declared in that class itself.
2. The annotation has no `@Retention(RetentionPolicy.RUNTIME)` (the default, `CLASS`, is invisible to reflection). Or it was written on a record component, and its `@Target` doesn't include `FIELD`, so the compiler didn't copy it to the field.
3. The called code can throw anything, including checked exceptions that `invoke` doesn't declare. So reflection wraps it in one checked exception type. `e.getCause()` returns the original.
4. On the record's private field, through `getDeclaredFields()`. Not on the record component (`getRecordComponents()`), the accessor method or the constructor parameter, because the annotation isn't allowed there.
5. `@Service` is itself annotated with `@Component` (a meta-annotation), and Spring's scan looks for `@Component` on the class and on the class's annotations, at any depth.
6. The transaction logic lives in the proxy. A call on `this` goes straight to the bean and never passes through the proxy.
7. It rolls back on `RuntimeException` and `Error`, and commits on checked exceptions. `rollbackFor` changes the rule.
8. When the bean's class implements no interface, and in Spring Boot by default for every proxy. CGLIB generates a subclass, so it can't intercept `final` or `private` methods, and can't proxy a `final` class at all.
9. The target doesn't know its proxy, so `target.equals(proxy)` is `false`. That breaks the rule that every object equals itself, and a proxy put in a `HashSet` can't be found again.
10. A bean can only be constructed after the beans its constructor needs. With a cycle, every bean in it waits for another, so none of them can be created first. Spring refuses such constructor cycles.
11. Slower calls, no checks by the compiler (names are strings), broken encapsulation, and extra work for native images. Any three.
</details>

## Further reading

- The Java Tutorials: [The Reflection API](https://docs.oracle.com/javase/tutorial/reflect/) (written for older Java versions, but the basics haven't changed)
- API docs: [`java.lang.reflect`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/reflect/package-summary.html), especially [`Proxy`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/reflect/Proxy.html), and [`java.lang.annotation`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/annotation/package-summary.html)
- The JLS: [9.6 Annotation interfaces](https://docs.oracle.com/javase/specs/jls/se25/html/jls-9.html#jls-9.6) and [8.10.3 Record members](https://docs.oracle.com/javase/specs/jls/se25/html/jls-8.html#jls-8.10.3) (how annotations on record components propagate)
- [JEP 403: Strongly encapsulate JDK internals](https://openjdk.org/jeps/403) and [JEP 416: Reimplement core reflection with method handles](https://openjdk.org/jeps/416)
- Spring Framework reference: [Classpath scanning and managed components](https://docs.spring.io/spring-framework/reference/core/beans/classpath-scanning.html), [Understanding AOP proxies](https://docs.spring.io/spring-framework/reference/core/aop/proxying.html) (self-invocation) and [Rolling back a declarative transaction](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/rolling-back.html)
