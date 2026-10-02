package practice.j09.ex04.sample.constructors;

import practice.j09.ex04.Component;
import practice.j09.ex04.Inject;
import practice.j09.ex04.Service;

/** Components with every kind of constructor: some the container can use, some it must reject. */
public final class ConstructorSamples {

    private ConstructorSamples() {
    }

    /** No constructor written, so Java adds a public one without parameters. */
    @Component
    public static class Clock {
    }

    /** A {@code @Service} without dependencies. */
    @Service
    public static class Scheduler {
    }

    /** One public constructor: no {@code @Inject} needed. */
    @Component
    public static class OneConstructor {

        private final Clock clock;

        public OneConstructor(Clock clock) {
            this.clock = clock;
        }

        public Clock clock() {
            return clock;
        }
    }

    /** Two public constructors: {@code @Inject} picks one. */
    @Component
    public static class WithInject {

        private final String chosen;

        public WithInject() {
            this.chosen = "without arguments";
        }

        @Inject
        public WithInject(Clock clock) {
            this.chosen = "@Inject";
        }

        public String chosen() {
            return chosen;
        }
    }

    /** Two public constructors and no {@code @Inject}: the container can't choose. */
    @Component
    public static class TwoConstructors {

        public TwoConstructors() {
        }

        public TwoConstructors(Clock clock) {
        }
    }

    /** Two {@code @Inject} constructors: the container can't choose either. */
    @Component
    public static class TwoInjectConstructors {

        @Inject
        public TwoInjectConstructors() {
        }

        @Inject
        public TwoInjectConstructors(Clock clock) {
        }
    }

    /** Its only constructor isn't public. */
    @Component
    public static class NoPublicConstructor {

        NoPublicConstructor() {
        }
    }

    /** Its constructor throws. */
    @Component
    public static class FailingConstructor {

        public FailingConstructor() {
            throw new IllegalStateException("no database connection");
        }
    }

    /** Not annotated, so not a component. */
    public static class NotAComponent {
    }

    /** No component implements this interface. */
    public interface Mailer {
    }

    /** Needs a Mailer, which nobody provides. */
    @Component
    public static class NeedsMailer {

        public NeedsMailer(Mailer mailer) {
        }
    }
}
