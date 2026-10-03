package practice.j09.ex04;

import java.lang.reflect.Constructor;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    // Suggested fields. Change them as you like: the tests only use the public methods.
    private final List<Class<?>> componentClasses;               // in registration order
    private final Map<Class<?>, Object> beans = new LinkedHashMap<>(); // component class → bean, in creation order

    private MiniContainer(List<Class<?>> componentClasses) {
        // TODO: J09-04
        throw new UnsupportedOperationException("TODO: J09-04");
    }

    /**
     * Creates a container for these component classes, and creates all their beans.
     *
     * @throws IllegalArgumentException if a class isn't a component, or appears twice. The message names the class.
     */
    public static MiniContainer of(Class<?>... componentClasses) {
        // TODO: J09-04
        throw new UnsupportedOperationException("TODO: J09-04");
    }

    /**
     * Creates a container for the component classes in {@code basePackage} and its sub-packages, found with
     * {@link ClassScanner}, and creates all their beans. Registration order is the scanner's order.
     */
    public static MiniContainer scan(String basePackage) {
        // TODO: J09-04
        throw new UnsupportedOperationException("TODO: J09-04");
    }

    /** Returns the bean for {@code type}, resolved as rule 3 says. */
    public <T> T getBean(Class<T> type) {
        // TODO: J09-04
        throw new UnsupportedOperationException("TODO: J09-04");
    }

    /**
     * The component classes in the order their beans were created, as an unmodifiable list. Each class comes after
     * the classes it depends on.
     */
    public List<Class<?>> creationOrder() {
        // TODO: J09-04
        throw new UnsupportedOperationException("TODO: J09-04");
    }

    /**
     * Optional step 5: the calls of {@link Timed} methods so far, in call order. See the README.
     *
     * <p>Leave this method as it is until you start step 5: the step 5 tests are skipped while it throws.
     */
    public List<TimedCall> timedCalls() {
        // TODO: J09-04 (optional step 5)
        throw new UnsupportedOperationException("TODO: J09-04 (optional step 5)");
    }

    // ---- Suggested helpers, one or two per README step. Change them as you like. ------------------------------

    /** Step 1: whether {@code type} is a component (rule 1). */
    private static boolean isComponent(Class<?> type) {
        // TODO: J09-04
        throw new UnsupportedOperationException("TODO: J09-04");
    }

    /** Step 1: the constructor to create a bean of {@code type} with (rule 2). */
    private static Constructor<?> injectionConstructor(Class<?> type) {
        // TODO: J09-04
        throw new UnsupportedOperationException("TODO: J09-04");
    }

    /** Step 2: the registered component class to use for {@code type} (rule 3). */
    private Class<?> resolve(Class<?> type) {
        // TODO: J09-04
        throw new UnsupportedOperationException("TODO: J09-04");
    }

    /**
     * Steps 1 and 3: the bean of {@code componentClass}. Creates it first, with its dependencies, if it doesn't exist
     * yet (rules 4 and 5). {@code inCreation} holds the classes whose beans are being created right now, in the
     * order their creation started: the path of a cycle.
     */
    private Object getOrCreate(Class<?> componentClass, SequencedSet<Class<?>> inCreation) {
        // TODO: J09-04
        throw new UnsupportedOperationException("TODO: J09-04");
    }

    /** Step 4: runs the bean's {@link Init} method, if it has one (rule 6). */
    private static void runInit(Object bean) {
        // TODO: J09-04
        throw new UnsupportedOperationException("TODO: J09-04");
    }
}
