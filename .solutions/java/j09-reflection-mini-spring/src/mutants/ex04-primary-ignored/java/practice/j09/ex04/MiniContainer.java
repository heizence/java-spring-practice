package practice.j09.ex04;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SequencedSet;

/**
 * A tiny dependency-injection container: the core idea of Spring's {@code ApplicationContext}.
 *
 * <h2>Rules</h2>
 * <ol>
 *   <li><b>Components.</b> A component class is annotated {@link Component}, directly or through a
 *       <em>meta-annotation</em>: an annotation such as {@link Service} that is itself annotated
 *       {@code @Component}. {@link #of} accepts only component classes, each one once. {@link #scan} picks the
 *       component classes out of a package and skips every other class.</li>
 *   <li><b>Constructors.</b> A bean is created with the only public constructor of its class. If the class has
 *       several public constructors, the one annotated {@link Inject} is used. No public constructor, or several
 *       without exactly one {@code @Inject}: {@link BeanCreationException}.</li>
 *   <li><b>Resolving a type.</b> The <em>candidates</em> for a type (a class or an interface) are the component
 *       classes that are that type or a subtype of it. With one candidate, that one is used. With several, the one
 *       annotated {@link Primary} is used, if exactly one is. With none: {@link NoSuchBeanException}. Otherwise:
 *       {@link NoUniqueBeanException}.</li>
 *   <li><b>Creating beans.</b> The container creates every bean when it is created, so mistakes show up at
 *       startup. It goes through the component classes in registration order. For each constructor parameter, in
 *       declaration order, it resolves the parameter's type and creates that bean first. Every bean is a
 *       <em>singleton</em>: it is created once, and everyone who needs it gets the same object.</li>
 *   <li><b>Cycles.</b> When creating a bean needs that same bean again, directly or through other beans, the
 *       container throws {@link CircularDependencyException}. Its path starts and ends with the first class of the
 *       cycle whose creation started, such as {@code [A, B, A]}. Classes that only lead into the cycle, and beans
 *       that are already finished, aren't part of the path.</li>
 *   <li><b>{@code @Init}.</b> A component class may have one public method annotated {@link Init}, without
 *       parameters. (The test classes follow this rule, so the container doesn't need to check it.) It runs right
 *       after the bean's constructor, so a bean's {@code @Init} method runs after the {@code @Init} methods of the
 *       beans it depends on.</li>
 *   <li><b>Failures.</b> When a constructor or an {@code @Init} method throws, the container throws a
 *       {@link BeanCreationException} whose cause is the original exception, not an
 *       {@link java.lang.reflect.InvocationTargetException}.</li>
 *   <li>{@code null} arguments throw {@link NullPointerException}.</li>
 * </ol>
 */
public final class MiniContainer {

    private final List<Class<?>> componentClasses;               // in registration order
    private final Map<Class<?>, Object> beans = new LinkedHashMap<>(); // component class → bean, in creation order
    private final List<TimedCall> timedCalls = new ArrayList<>();      // step 5

    private MiniContainer(List<Class<?>> componentClasses) {
        this.componentClasses = List.copyOf(componentClasses);
        // Create everything now, like Spring does for singletons: a missing or ambiguous dependency fails at startup,
        // not at the first request that happens to need the bean.
        for (Class<?> componentClass : this.componentClasses) {
            getOrCreate(componentClass, new LinkedHashSet<>());
        }
    }

    /**
     * Creates a container for these component classes, and creates all their beans.
     *
     * @throws IllegalArgumentException if a class isn't a component, or appears twice. The message names the class.
     */
    public static MiniContainer of(Class<?>... componentClasses) {
        Objects.requireNonNull(componentClasses, "componentClasses");
        SequencedSet<Class<?>> unique = new LinkedHashSet<>();
        for (Class<?> type : componentClasses) {
            Objects.requireNonNull(type, "component class");
            if (!isComponent(type)) {
                throw new IllegalArgumentException(type.getName() + " is not a component: it has no @Component");
            }
            if (!unique.add(type)) {
                throw new IllegalArgumentException(type.getName() + " is listed twice");
            }
        }
        return new MiniContainer(List.copyOf(unique));
    }

    /**
     * Creates a container for the component classes in {@code basePackage} and its sub-packages, found with
     * {@link ClassScanner}, and creates all their beans. Registration order is the scanner's order.
     */
    public static MiniContainer scan(String basePackage) {
        return new MiniContainer(ClassScanner.findClasses(basePackage).stream()
                .filter(MiniContainer::isComponent)
                .toList());
    }

    /** Returns the bean for {@code type}, resolved as rule 3 says. */
    public <T> T getBean(Class<T> type) {
        Objects.requireNonNull(type, "type");
        // Every bean exists already, so this is a lookup. cast() throws ClassCastException when a proxy (step 5)
        // stands in for the class: Spring reports the same problem as BeanNotOfRequiredTypeException.
        return type.cast(beans.get(resolve(type)));
    }

    /**
     * The component classes in the order their beans were created, as an unmodifiable list. Each class comes after
     * the classes it depends on.
     */
    public List<Class<?>> creationOrder() {
        return List.copyOf(beans.keySet());
    }

    /**
     * Optional step 5: the calls of {@link Timed} methods so far, in call order. See the README.
     *
     * <p>Leave this method as it is until you start step 5: the step 5 tests are skipped while it throws.
     */
    public List<TimedCall> timedCalls() {
        return List.copyOf(timedCalls);
    }

    // ---- Step 1 --------------------------------------------------------------------------------------------------

    /** Step 1: whether {@code type} is a component (rule 1). */
    private static boolean isComponent(Class<?> type) {
        if (type.isAnnotationPresent(Component.class)) {
            return true;
        }
        // One level of meta-annotations: @Service is annotated @Component. Spring searches any depth, and has to
        // remember what it has seen, because annotations such as @Documented are annotated with themselves.
        for (Annotation annotation : type.getAnnotations()) {
            if (annotation.annotationType().isAnnotationPresent(Component.class)) {
                return true;
            }
        }
        return false;
    }

    /** Step 1: the constructor to create a bean of {@code type} with (rule 2). */
    private static Constructor<?> injectionConstructor(Class<?> type) {
        Constructor<?>[] constructors = type.getConstructors(); // public only
        if (constructors.length == 1) {
            return constructors[0];
        }
        List<Constructor<?>> marked = Arrays.stream(constructors)
                .filter(constructor -> constructor.isAnnotationPresent(Inject.class))
                .toList();
        if (marked.size() == 1) {
            return marked.getFirst();
        }
        if (constructors.length == 0) {
            throw new BeanCreationException(type, "it has no public constructor");
        }
        throw new BeanCreationException(type,
                "it has " + constructors.length + " public constructors, so exactly one needs @Inject");
    }

    // ---- Step 2 --------------------------------------------------------------------------------------------------

    /** Step 2: the registered component class to use for {@code type} (rule 3). */
    private Class<?> resolve(Class<?> type) {
        // isAssignableFrom reads backwards: type.isAssignableFrom(c) means "a c can be used where a type is needed".
        List<Class<?>> candidates = componentClasses.stream()
                .filter(type::isAssignableFrom)
                .toList();
        if (candidates.size() == 1) {
            return candidates.getFirst();
        }
        if (candidates.isEmpty()) {
            throw new NoSuchBeanException(type);
        }
        throw new NoUniqueBeanException(type, candidates);
    }

    // ---- Step 3 --------------------------------------------------------------------------------------------------

    /**
     * Steps 1 and 3: the bean of {@code componentClass}. Creates it first, with its dependencies, if it doesn't exist
     * yet (rules 4 and 5). {@code inCreation} holds the classes whose beans are being created right now, in the
     * order their creation started: the path of a cycle.
     */
    private Object getOrCreate(Class<?> componentClass, SequencedSet<Class<?>> inCreation) {
        Object existing = beans.get(componentClass);
        if (existing != null) {
            return existing; // a singleton: everyone gets the same bean
        }
        // inCreation holds the beans whose constructors are waiting for their dependencies right now. Meeting one of
        // them again means we went round in a circle: without this check, the recursion would never end.
        if (!inCreation.add(componentClass)) {
            throw new CircularDependencyException(cyclePath(inCreation, componentClass));
        }

        Constructor<?> constructor = injectionConstructor(componentClass);
        Class<?>[] parameterTypes = constructor.getParameterTypes();
        Object[] arguments = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            // This recursion is a depth-first walk of the dependency graph: each dependency is created (and added
            // to `beans`) before the bean that needs it, so the creation order is a topological order of the graph.
            // A loop rather than a stream, because creating beans is a side effect.
            arguments[i] = getOrCreate(resolve(parameterTypes[i]), inCreation);
        }
        Object bean = instantiate(constructor, arguments);
        runInit(bean);
        Object finished = withTiming(bean);

        inCreation.removeLast(); // this bean is done, so it no longer counts for cycles
        beans.put(componentClass, finished);
        return finished;
    }

    /** {@code [Start, A, B]} with {@code A} met again → {@code [A, B, A]}. */
    private static List<Class<?>> cyclePath(SequencedSet<Class<?>> inCreation, Class<?> repeated) {
        List<Class<?>> chain = List.copyOf(inCreation);
        List<Class<?>> path = new ArrayList<>(chain.subList(chain.indexOf(repeated), chain.size()));
        path.add(repeated);
        return path;
    }

    private static Object instantiate(Constructor<?> constructor, Object[] arguments) {
        Class<?> type = constructor.getDeclaringClass();
        try {
            return constructor.newInstance(arguments);
        } catch (InvocationTargetException e) {
            // The constructor itself threw. Report the original exception as the cause, not reflection's wrapper.
            throw new BeanCreationException(type, "its constructor threw " + e.getCause(), e.getCause());
        } catch (ReflectiveOperationException e) {
            // InstantiationException (an abstract class) or IllegalAccessException (a class we can't reach).
            throw new BeanCreationException(type, e.toString(), e);
        }
    }

    // ---- Step 4 --------------------------------------------------------------------------------------------------

    /** Step 4: runs the bean's {@link Init} method, if it has one (rule 6). */
    private static void runInit(Object bean) {
        Class<?> type = bean.getClass();
        // getMethods() returns the public methods, inherited ones included. The rules allow one @Init method at most.
        for (Method init : type.getMethods()) {
            if (!init.isAnnotationPresent(Init.class)) {
                continue;
            }
            try {
                init.invoke(bean);
            } catch (InvocationTargetException e) {
                // The @Init method itself threw. As for constructors, report the original exception as the cause.
                throw new BeanCreationException(type, "@Init method " + init.getName() + " threw " + e.getCause(),
                        e.getCause());
            } catch (IllegalAccessException e) {
                throw new BeanCreationException(type, e.toString(), e);
            }
        }
    }

    // ---- Step 5 (optional) ---------------------------------------------------------------------------------------

    /**
     * Wraps the bean in a JDK proxy when its class has a public {@code @Timed} method and implements at least one
     * interface. Without an interface, a JDK proxy has nothing to implement; Spring would generate a CGLIB subclass.
     */
    private Object withTiming(Object bean) {
        Class<?> type = bean.getClass();
        boolean timed = Arrays.stream(type.getMethods()).anyMatch(method -> method.isAnnotationPresent(Timed.class));
        Class<?>[] interfaces = type.getInterfaces();
        if (!timed || interfaces.length == 0) {
            return bean;
        }
        InvocationHandler handler = (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) { // as in J09-03
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    default -> bean.toString();
                };
            }
            // The annotation sits on the class's method, not on the interface's method we get here.
            boolean measure = type.getMethod(method.getName(), method.getParameterTypes())
                    .isAnnotationPresent(Timed.class);
            long start = System.nanoTime();
            try {
                return method.invoke(bean, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            } finally {
                if (measure) {
                    timedCalls.add(new TimedCall(type.getSimpleName(), method.getName(),
                            Duration.ofNanos(System.nanoTime() - start)));
                }
            }
        };
        return Proxy.newProxyInstance(type.getClassLoader(), interfaces, handler);
    }
}
