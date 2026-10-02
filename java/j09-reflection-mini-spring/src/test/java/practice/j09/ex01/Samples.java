package practice.j09.ex01;

import java.io.IOException;

/** Small classes for {@link InspectorTest} to inspect. */
final class Samples {

    private Samples() {
    }

    /** A class with fields of every access level, two constructors and an overloaded method. */
    public static class Account {

        public static final String BANK = "Java Bank"; // static: not part of an object's field values

        private final String owner;
        private long balanceCents;
        protected boolean frozen;
        String note; // package-private, and null until someone sets it

        public Account(String owner) {
            this(owner, 0);
        }

        public Account(String owner, long balanceCents) {
            this.owner = owner;
            this.balanceCents = balanceCents;
        }

        public String owner() {
            return owner;
        }

        public long balanceCents() {
            return balanceCents;
        }

        public void deposit(long cents) {
            balanceCents += cents;
        }

        public void deposit(long cents, String note) {
            deposit(cents);
            this.note = note;
        }

        private void audit() {
            // private: invoke() must not find it
        }
    }

    /** Inherits every field and method of {@link Account}, and adds one field. */
    public static class SavingsAccount extends Account {

        private final double rate;

        public SavingsAccount(String owner, double rate) {
            super(owner);
            this.rate = rate;
        }

        public double rate() {
            return rate;
        }
    }

    /** Its constructor rejects impossible values. */
    public static class Temperature {

        private final double celsius;

        public Temperature(double celsius) {
            if (celsius < -273.15) {
                throw new IllegalArgumentException("below absolute zero: " + celsius);
            }
            this.celsius = celsius;
        }

        public double celsius() {
            return celsius;
        }
    }

    /** Its constructor always throws a checked exception. */
    public static class ConfigFile {

        public ConfigFile(String path) throws IOException {
            throw new IOException("no such file: " + path);
        }
    }

    /** A String fits both constructors; any other object fits only the second. */
    public static class Message {

        private final String kind;

        public Message(String text) {
            this.kind = "text";
        }

        public Message(Object payload) {
            this.kind = "object";
        }

        public String kind() {
            return kind;
        }
    }

    /** Has no public constructor. */
    public static class Singleton {

        private Singleton() {
        }
    }

    /** Can't be instantiated, although its constructor is public. */
    public abstract static class Shape {

        public Shape() {
        }
    }

    /** Methods that throw on purpose. */
    public static class Alarm {

        public void trigger(RuntimeException problem) {
            throw problem;
        }

        public void panic() {
            throw new AssertionError("broken invariant");
        }

        public void check() throws IOException {
            throw new IOException("sensor offline");
        }
    }

    /** Implements a generic interface, so the compiler adds a bridge method compareTo(Object). */
    public record Ticket(int number) implements Comparable<Ticket> {

        @Override
        public int compareTo(Ticket other) {
            return Integer.compare(number, other.number);
        }
    }

    public record Point(int x, int y) {
    }

    public record Segment(Point from, Point to) {
    }

    public record Empty() {
    }

    public record Named(String name) {
    }

    /** Overrides toString(), but describeRecord() still uses the standard format. */
    public record Fraction(int numerator, int denominator) {

        @Override
        public String toString() {
            return numerator + "/" + denominator;
        }
    }

    /** Overrides its accessor, so the accessor and the field disagree. */
    public record Shout(String word) {

        @Override
        public String word() {
            return word.toUpperCase();
        }
    }
}
