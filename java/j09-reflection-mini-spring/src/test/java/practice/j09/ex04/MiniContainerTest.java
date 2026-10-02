package practice.j09.ex04;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import practice.j09.ex04.sample.ambiguous.NotifierSamples.AlertService;
import practice.j09.ex04.sample.ambiguous.NotifierSamples.EmailNotifier;
import practice.j09.ex04.sample.ambiguous.NotifierSamples.Notifier;
import practice.j09.ex04.sample.ambiguous.NotifierSamples.PagerNotifier;
import practice.j09.ex04.sample.ambiguous.NotifierSamples.PushNotifier;
import practice.j09.ex04.sample.ambiguous.NotifierSamples.SmsNotifier;
import practice.j09.ex04.sample.constructors.ConstructorSamples.Clock;
import practice.j09.ex04.sample.constructors.ConstructorSamples.FailingConstructor;
import practice.j09.ex04.sample.constructors.ConstructorSamples.NeedsMailer;
import practice.j09.ex04.sample.constructors.ConstructorSamples.NoPublicConstructor;
import practice.j09.ex04.sample.constructors.ConstructorSamples.NotAComponent;
import practice.j09.ex04.sample.constructors.ConstructorSamples.OneConstructor;
import practice.j09.ex04.sample.constructors.ConstructorSamples.Scheduler;
import practice.j09.ex04.sample.constructors.ConstructorSamples.TwoConstructors;
import practice.j09.ex04.sample.constructors.ConstructorSamples.TwoInjectConstructors;
import practice.j09.ex04.sample.constructors.ConstructorSamples.WithInject;
import practice.j09.ex04.sample.cycle.CycleSamples.A;
import practice.j09.ex04.sample.cycle.CycleSamples.B;
import practice.j09.ex04.sample.cycle.CycleSamples.Hub;
import practice.j09.ex04.sample.cycle.CycleSamples.Leaf;
import practice.j09.ex04.sample.cycle.CycleSamples.P;
import practice.j09.ex04.sample.cycle.CycleSamples.QImpl;
import practice.j09.ex04.sample.cycle.CycleSamples.SelfLoop;
import practice.j09.ex04.sample.cycle.CycleSamples.Spoke;
import practice.j09.ex04.sample.cycle.CycleSamples.Start;
import practice.j09.ex04.sample.cycle.CycleSamples.X;
import practice.j09.ex04.sample.cycle.CycleSamples.Y;
import practice.j09.ex04.sample.cycle.CycleSamples.Z;
import practice.j09.ex04.sample.lifecycle.LifecycleSamples.Database;
import practice.j09.ex04.sample.lifecycle.LifecycleSamples.FailingInit;
import practice.j09.ex04.sample.lifecycle.LifecycleSamples.Repository;
import practice.j09.ex04.sample.shop.CheckoutService;
import practice.j09.ex04.sample.shop.FakePaymentGateway;
import practice.j09.ex04.sample.shop.InMemoryProductCatalog;
import practice.j09.ex04.sample.shop.PaymentGateway;
import practice.j09.ex04.sample.shop.PriceCalculator;
import practice.j09.ex04.sample.shop.ProductCatalog;
import practice.j09.ex04.sample.shop.ProductSearch;
import practice.j09.ex04.sample.shop.Receipt;
import practice.j09.ex04.sample.timed.TimedSamples.Greeter;
import practice.j09.ex04.sample.timed.TimedSamples.GreetingClient;
import practice.j09.ex04.sample.timed.TimedSamples.Stopwatch;
import practice.j09.ex04.sample.timed.TimedSamples.TimedGreeter;

@DisplayName("J09-04 · Capstone: a mini IoC container")
class MiniContainerTest {

    private static final String SHOP = "practice.j09.ex04.sample.shop";

    /** The shop's components, registered in an order where every class comes before its dependencies. */
    private static MiniContainer shop() {
        return MiniContainer.of(
                CheckoutService.class, PriceCalculator.class, InMemoryProductCatalog.class, FakePaymentGateway.class);
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("step 1: creates the bean of a component without dependencies")
        void beanWithoutDependencies() {
            var container = MiniContainer.of(Clock.class);

            assertThat(container.getBean(Clock.class)).isNotNull();
        }

        @Test
        @DisplayName("step 1: treats a class marked @Service as a component, because @Service carries @Component")
        void serviceIsComponent() {
            var container = MiniContainer.of(Scheduler.class);

            assertThat(container.getBean(Scheduler.class)).isNotNull();
        }

        @Test
        @DisplayName("step 2: finds a bean by an interface that its class implements")
        void beanByInterface() {
            var container = MiniContainer.of(InMemoryProductCatalog.class);

            assertThat(container.getBean(ProductCatalog.class)).isInstanceOf(InMemoryProductCatalog.class);
        }

        @Test
        @DisplayName("step 2: @Primary decides between several candidates")
        void primaryDecides() {
            var container = MiniContainer.of(EmailNotifier.class, SmsNotifier.class, PushNotifier.class);

            assertThat(container.getBean(Notifier.class)).isInstanceOf(PushNotifier.class);
        }

        @Test
        @DisplayName("step 3: injects dependencies through the constructor")
        void constructorInjection() {
            CheckoutService checkout = shop().getBean(CheckoutService.class);

            Receipt receipt = checkout.checkout("book", 2);

            assertThat(receipt).isEqualTo(new Receipt("book", 2, 5_000, "payment-1"));
        }

        @Test
        @DisplayName("step 3: every bean is a singleton: getBean() and injection give the same object")
        void singletons() {
            var container = shop();

            assertThat(container.getBean(PriceCalculator.class)).isSameAs(container.getBean(PriceCalculator.class));
            assertThat(container.getBean(CheckoutService.class).calculator())
                    .isSameAs(container.getBean(PriceCalculator.class));
            assertThat(container.getBean(CheckoutService.class).payments())
                    .isSameAs(container.getBean(PaymentGateway.class));
        }

        @Test
        @DisplayName("step 4: runs the @Init method after the constructor")
        void initRuns() {
            var container = MiniContainer.of(InMemoryProductCatalog.class);

            assertThat(container.getBean(InMemoryProductCatalog.class).isLoaded()).isTrue();
        }

        @Test
        @DisplayName("step 4: scan() finds the component classes of a package")
        void scanFindsComponents() {
            var container = MiniContainer.scan(SHOP);

            Receipt receipt = container.getBean(CheckoutService.class).checkout("pen", 3);

            assertThat(receipt).isEqualTo(new Receipt("pen", 3, 900, "payment-1"));
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("uses the only public constructor, without @Inject")
        void onlyConstructor() {
            var container = MiniContainer.of(OneConstructor.class, Clock.class);

            assertThat(container.getBean(OneConstructor.class).clock()).isSameAs(container.getBean(Clock.class));
        }

        @Test
        @DisplayName("uses the @Inject constructor when there are several public constructors")
        void injectConstructor() {
            var container = MiniContainer.of(WithInject.class, Clock.class);

            assertThat(container.getBean(WithInject.class).chosen()).isEqualTo("@Inject");
        }

        @Test
        @DisplayName("finds a class by its own type, even when other classes implement the same interface")
        void exactClass() {
            var container = MiniContainer.of(EmailNotifier.class, SmsNotifier.class);

            assertThat(container.getBean(EmailNotifier.class)).isInstanceOf(EmailNotifier.class);
        }

        @Test
        @DisplayName("injects the @Primary candidate into a constructor")
        void primaryInjected() {
            var container = MiniContainer.of(EmailNotifier.class, PushNotifier.class, AlertService.class);

            assertThat(container.getBean(AlertService.class).alert("disk full")).isEqualTo("push: disk full");
        }

        @Test
        @DisplayName("getBean(Object.class) works when the container holds exactly one bean")
        void objectWithOneBean() {
            var container = MiniContainer.of(Clock.class);

            assertThat(container.getBean(Object.class)).isInstanceOf(Clock.class);
        }

        @Test
        @DisplayName("creates each bean once, even when several beans depend on it")
        void sharedDependency() {
            var container = MiniContainer.scan(SHOP);

            ProductCatalog catalog = container.getBean(ProductCatalog.class);

            assertThat(container.getBean(PriceCalculator.class).catalog()).isSameAs(catalog);
            assertThat(container.getBean(ProductSearch.class).catalog()).isSameAs(catalog);
        }

        @Test
        @DisplayName("creates the dependencies of a bean before the bean itself")
        void dependenciesFirst() {
            List<Class<?>> order = shop().creationOrder();

            assertThat(order).containsExactlyInAnyOrder(CheckoutService.class, PriceCalculator.class,
                    InMemoryProductCatalog.class, FakePaymentGateway.class);
            assertThat(order.indexOf(InMemoryProductCatalog.class)).isLessThan(order.indexOf(PriceCalculator.class));
            assertThat(order.indexOf(PriceCalculator.class)).isLessThan(order.indexOf(CheckoutService.class));
            assertThat(order.indexOf(FakePaymentGateway.class)).isLessThan(order.indexOf(CheckoutService.class));
        }

        @Test
        @DisplayName("runs @Init methods in dependency order, whatever the registration order")
        void initInDependencyOrder() {
            var container = MiniContainer.of(Repository.class, Database.class);

            assertThat(container.getBean(Repository.class).isWarmedUp()).isTrue();
        }

        @Test
        @DisplayName("scan() skips interfaces, records and other classes that aren't components")
        void scanSkipsNonComponents() {
            var container = MiniContainer.scan(SHOP);

            assertThat(container.creationOrder()).containsExactlyInAnyOrder(CheckoutService.class,
                    PriceCalculator.class, ProductSearch.class, InMemoryProductCatalog.class, FakePaymentGateway.class);
        }

        @Test
        @DisplayName("scan() of a package without classes gives an empty container")
        void scanEmptyPackage() {
            var container = MiniContainer.scan("practice.j09.ex04.sample.nothing.here");

            assertThat(container.creationOrder()).isEmpty();
        }

        @Test
        @DisplayName("a container without components has no beans")
        void emptyContainer() {
            var container = MiniContainer.of();

            assertThat(container.creationOrder()).isEmpty();
            assertThatThrownBy(() -> container.getBean(Object.class)).isInstanceOf(NoSuchBeanException.class);
        }

        @Test
        @DisplayName("creationOrder() can't be changed by the caller")
        void creationOrderIsUnmodifiable() {
            List<Class<?>> order = MiniContainer.of(Clock.class).creationOrder();

            assertThatThrownBy(() -> order.add(String.class)).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("the path of a cycle leaves out the classes that only lead into it")
        void cyclePathOnlyCycle() {
            assertThatThrownBy(() -> MiniContainer.of(Start.class, A.class, B.class))
                    .isInstanceOf(CircularDependencyException.class)
                    .hasMessage("Circular dependency: A -> B -> A");
        }

        @Test
        @DisplayName("the path of a cycle leaves out beans that were already finished")
        void cyclePathWithoutFinishedBeans() {
            assertThatThrownBy(() -> MiniContainer.of(Hub.class, Leaf.class, Spoke.class))
                    .isInstanceOf(CircularDependencyException.class)
                    .hasMessage("Circular dependency: Hub -> Spoke -> Hub");
        }

        @Test
        @DisplayName("a bean that needs itself is a cycle too")
        void selfLoop() {
            assertThatThrownBy(() -> MiniContainer.of(SelfLoop.class))
                    .isInstanceOf(CircularDependencyException.class)
                    .hasMessage("Circular dependency: SelfLoop -> SelfLoop");
        }

        @Test
        @DisplayName("a cycle of three beans shows all three")
        void threeBeanCycle() {
            assertThatThrownBy(() -> MiniContainer.of(X.class, Y.class, Z.class))
                    .isInstanceOf(CircularDependencyException.class)
                    .hasMessage("Circular dependency: X -> Y -> Z -> X");
        }

        @Test
        @DisplayName("a cycle through an interface names the implementing class")
        void cycleThroughInterface() {
            assertThatThrownBy(() -> MiniContainer.of(P.class, QImpl.class))
                    .isInstanceOf(CircularDependencyException.class)
                    .hasMessage("Circular dependency: P -> QImpl -> P");
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("of() rejects a class that isn't a component")
        void rejectsNonComponent() {
            assertThatThrownBy(() -> MiniContainer.of(Clock.class, NotAComponent.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("NotAComponent");
        }

        @Test
        @DisplayName("of() rejects a class listed twice")
        void rejectsDuplicates() {
            assertThatThrownBy(() -> MiniContainer.of(Clock.class, Clock.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Clock");
        }

        @Test
        @DisplayName("rejects null arguments")
        void rejectsNull() {
            assertThatThrownBy(() -> MiniContainer.of((Class<?>[]) null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> MiniContainer.of(Clock.class, null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> MiniContainer.scan(null)).isInstanceOf(NullPointerException.class);
            var container = MiniContainer.of(Clock.class);
            assertThatThrownBy(() -> container.getBean(null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("getBean() throws NoSuchBeanException when no component fits")
        void noSuchBean() {
            var container = MiniContainer.of(Clock.class);

            assertThatThrownBy(() -> container.getBean(Notifier.class))
                    .isInstanceOf(NoSuchBeanException.class)
                    .hasMessage("No bean of type Notifier");
        }

        @Test
        @DisplayName("a missing dependency fails at startup with NoSuchBeanException")
        void missingDependency() {
            assertThatThrownBy(() -> MiniContainer.of(NeedsMailer.class))
                    .isInstanceOf(NoSuchBeanException.class)
                    .hasMessage("No bean of type Mailer");
        }

        @Test
        @DisplayName("several candidates without @Primary: NoUniqueBeanException that lists them")
        void noUniqueBean() {
            var container = MiniContainer.of(EmailNotifier.class, SmsNotifier.class);

            assertThatThrownBy(() -> container.getBean(Notifier.class))
                    .isInstanceOf(NoUniqueBeanException.class)
                    .hasMessage("Expected one bean of type Notifier but found 2: EmailNotifier, SmsNotifier");
        }

        @Test
        @DisplayName("two @Primary candidates are still ambiguous")
        void twoPrimaries() {
            var container = MiniContainer.of(PushNotifier.class, PagerNotifier.class);

            assertThatThrownBy(() -> container.getBean(Notifier.class)).isInstanceOf(NoUniqueBeanException.class);
        }

        @Test
        @DisplayName("an ambiguous dependency fails at startup with NoUniqueBeanException")
        void ambiguousDependency() {
            assertThatThrownBy(() -> MiniContainer.of(EmailNotifier.class, SmsNotifier.class, AlertService.class))
                    .isInstanceOf(NoUniqueBeanException.class);
        }

        @Test
        @DisplayName("a cycle fails at startup with CircularDependencyException that shows the path")
        void cycle() {
            assertThatThrownBy(() -> MiniContainer.of(A.class, B.class))
                    .isInstanceOf(CircularDependencyException.class)
                    .hasMessage("Circular dependency: A -> B -> A");
        }

        @Test
        @DisplayName("several public constructors without @Inject: BeanCreationException")
        void severalConstructors() {
            assertThatThrownBy(() -> MiniContainer.of(TwoConstructors.class, Clock.class))
                    .isInstanceOf(BeanCreationException.class)
                    .hasMessageContaining("TwoConstructors");
        }

        @Test
        @DisplayName("two @Inject constructors: BeanCreationException")
        void twoInjectConstructors() {
            assertThatThrownBy(() -> MiniContainer.of(TwoInjectConstructors.class, Clock.class))
                    .isInstanceOf(BeanCreationException.class);
        }

        @Test
        @DisplayName("no public constructor: BeanCreationException")
        void noPublicConstructor() {
            assertThatThrownBy(() -> MiniContainer.of(NoPublicConstructor.class))
                    .isInstanceOf(BeanCreationException.class)
                    .hasMessageContaining("NoPublicConstructor");
        }

        @Test
        @DisplayName("a constructor that throws: BeanCreationException caused by the original exception")
        void failingConstructor() {
            assertThatThrownBy(() -> MiniContainer.of(FailingConstructor.class))
                    .isInstanceOf(BeanCreationException.class)
                    .cause()
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("no database connection");
        }

        @Test
        @DisplayName("an @Init method that throws: BeanCreationException caused by the original exception")
        void failingInit() {
            assertThatThrownBy(() -> MiniContainer.of(FailingInit.class))
                    .isInstanceOf(BeanCreationException.class)
                    .cause()
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("cache server unreachable");
        }
    }

    /**
     * Tests for the optional step 5. They are skipped while {@link MiniContainer#timedCalls()} (or anything else in
     * the container) still throws the stub's "TODO" exception.
     */
    @Nested
    @DisplayName("Optional: @Timed proxies")
    class TimedProxies {

        private MiniContainer container;

        @BeforeEach
        void skipUntilStepFiveIsStarted() {
            try {
                container = MiniContainer.of(TimedGreeter.class, GreetingClient.class, Stopwatch.class);
                container.timedCalls();
            } catch (UnsupportedOperationException e) {
                if (String.valueOf(e.getMessage()).startsWith("TODO")) {
                    Assumptions.abort("Optional step 5 isn't started yet (see the README).");
                }
                throw e;
            }
        }

        @Test
        @DisplayName("records each call of a @Timed method: bean, method and time")
        void recordsTimedCall() {
            container.getBean(Greeter.class).greet("mina");

            assertThat(container.timedCalls()).singleElement().satisfies(call -> {
                assertThat(call.bean()).isEqualTo("TimedGreeter");
                assertThat(call.method()).isEqualTo("greet");
                assertThat(call.elapsed()).isGreaterThanOrEqualTo(Duration.ZERO);
            });
        }

        @Test
        @DisplayName("doesn't record calls of methods without @Timed")
        void skipsUntimedMethods() {
            String language = container.getBean(Greeter.class).language();

            assertThat(language).isEqualTo("English");
            assertThat(container.timedCalls()).isEmpty();
        }

        @Test
        @DisplayName("beans that depend on a timed bean get the proxy")
        void dependentsGetProxy() {
            GreetingClient client = container.getBean(GreetingClient.class);

            String welcome = client.welcome("jun");

            assertThat(welcome).isEqualTo("Hello, jun!");
            assertThat(Proxy.isProxyClass(client.greeter().getClass())).isTrue();
            assertThat(container.timedCalls()).extracting(TimedCall::method).containsExactly("greet");
        }

        @Test
        @DisplayName("records a call that throws, and rethrows the original exception")
        void recordsFailedCall() {
            Greeter greeter = container.getBean(Greeter.class);

            assertThatThrownBy(() -> greeter.greet(" "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("name is blank");
            assertThat(container.timedCalls()).extracting(TimedCall::method).containsExactly("greet");
        }

        @Test
        @DisplayName("getBean() with the implementation class fails: the bean is now a proxy of the interface")
        void implementationClassIsHidden() {
            assertThatThrownBy(() -> container.getBean(TimedGreeter.class)).isInstanceOf(ClassCastException.class);
        }

        @Test
        @DisplayName("a bean without an interface isn't proxied, so its @Timed methods aren't measured")
        void noInterfaceNoProxy() {
            Stopwatch stopwatch = container.getBean(Stopwatch.class);

            stopwatch.lap();

            assertThat(Proxy.isProxyClass(stopwatch.getClass())).isFalse();
            assertThat(container.timedCalls()).isEmpty();
        }
    }
}
