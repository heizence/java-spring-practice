package practice.j09.ex04.sample.lifecycle;

import practice.j09.ex04.Component;
import practice.j09.ex04.Init;

/** Components with {@code @Init} methods. */
public final class LifecycleSamples {

    private LifecycleSamples() {
    }

    @Component
    public static class Database {

        private boolean connected;

        @Init
        public void connect() {
            connected = true;
        }

        public boolean isConnected() {
            return connected;
        }
    }

    /** Its {@code @Init} method uses the database, so the database's {@code @Init} method must have run first. */
    @Component
    public static class Repository {

        private final Database database;
        private boolean warmedUp;

        public Repository(Database database) {
            this.database = database;
        }

        @Init
        public void warmUp() {
            if (!database.isConnected()) {
                throw new IllegalStateException("the database isn't connected yet");
            }
            warmedUp = true;
        }

        public boolean isWarmedUp() {
            return warmedUp;
        }
    }

    @Component
    public static class TwoInitMethods {

        @Init
        public void first() {
        }

        @Init
        public void second() {
        }
    }

    @Component
    public static class InitWithParameter {

        @Init
        public void init(String name) {
        }
    }

    @Component
    public static class FailingInit {

        @Init
        public void init() {
            throw new IllegalStateException("cache server unreachable");
        }
    }
}
