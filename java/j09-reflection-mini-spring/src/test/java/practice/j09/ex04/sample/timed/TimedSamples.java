package practice.j09.ex04.sample.timed;

import practice.j09.ex04.Component;
import practice.j09.ex04.Timed;

/** Components for the optional step 5. */
public final class TimedSamples {

    private TimedSamples() {
    }

    public interface Greeter {

        String greet(String name);

        String language();
    }

    /** Implements an interface and has a {@code @Timed} method, so the container hands out a proxy. */
    @Component
    public static class TimedGreeter implements Greeter {

        @Timed
        @Override
        public String greet(String name) {
            if (name.isBlank()) {
                throw new IllegalArgumentException("name is blank");
            }
            return "Hello, " + name;
        }

        @Override
        public String language() {
            return "English";
        }
    }

    /** Depends on the interface, so it can receive the proxy. */
    @Component
    public static class GreetingClient {

        private final Greeter greeter;

        public GreetingClient(Greeter greeter) {
            this.greeter = greeter;
        }

        public String welcome(String name) {
            return greeter.greet(name) + "!";
        }

        public Greeter greeter() {
            return greeter;
        }
    }

    /** Has a {@code @Timed} method but no interface: a JDK proxy can't stand in for it. */
    @Component
    public static class Stopwatch {

        @Timed
        public void lap() {
        }
    }
}
