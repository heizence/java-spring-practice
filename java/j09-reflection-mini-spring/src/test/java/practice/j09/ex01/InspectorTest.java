package practice.j09.ex01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import java.io.IOException;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.SequencedMap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import practice.j09.ex01.Samples.Account;
import practice.j09.ex01.Samples.Alarm;
import practice.j09.ex01.Samples.ConfigFile;
import practice.j09.ex01.Samples.Empty;
import practice.j09.ex01.Samples.Fraction;
import practice.j09.ex01.Samples.Message;
import practice.j09.ex01.Samples.Named;
import practice.j09.ex01.Samples.Point;
import practice.j09.ex01.Samples.SavingsAccount;
import practice.j09.ex01.Samples.Segment;
import practice.j09.ex01.Samples.Shape;
import practice.j09.ex01.Samples.Shout;
import practice.j09.ex01.Samples.Singleton;
import practice.j09.ex01.Samples.Temperature;
import practice.j09.ex01.Samples.Ticket;

@DisplayName("J09-01 · Reflection")
class InspectorTest {

    private final String owner = "mina";

    /**
     * An inner class (not static) that reads a field of the object around it. The compiler gives it a hidden,
     * synthetic field {@code this$0} that points to that outer object.
     */
    class Badge {

        private final String label = "VIP";

        String text() {
            return owner + ": " + label;
        }
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("fieldValues() returns the instance fields of every access level, in declaration order")
        void fieldValuesOfClass() {
            var account = new Account("mina", 1_500);
            account.note = "regular";

            SequencedMap<String, Object> values = Inspector.fieldValues(account);

            assertThat(values).containsExactly(
                    entry("owner", "mina"),
                    entry("balanceCents", 1_500L),
                    entry("frozen", false),
                    entry("note", "regular"));
        }

        @Test
        @DisplayName("fieldValues() works for records")
        void fieldValuesOfRecord() {
            assertThat(Inspector.fieldValues(new Point(3, 4))).containsExactly(entry("x", 3), entry("y", 4));
        }

        @Test
        @DisplayName("newInstance() calls the public constructor that fits the arguments")
        void newInstanceWithArguments() {
            Account account = Inspector.newInstance(Account.class, "mina", 1_500L);

            assertThat(account.owner()).isEqualTo("mina");
            assertThat(account.balanceCents()).isEqualTo(1_500);
        }

        @Test
        @DisplayName("newInstance() chooses a constructor by the number of arguments")
        void newInstanceChoosesByCount() {
            Account account = Inspector.newInstance(Account.class, "jun");

            assertThat(account.owner()).isEqualTo("jun");
            assertThat(account.balanceCents()).isZero();
        }

        @Test
        @DisplayName("invoke() calls a public method and returns its result")
        void invokeReturnsResult() {
            var account = new Account("mina", 1_500);

            assertThat(Inspector.invoke(account, "balanceCents")).isEqualTo(1_500L);
        }

        @Test
        @DisplayName("invoke() passes the arguments, and returns null for a void method")
        void invokePassesArguments() {
            var account = new Account("mina", 1_500);

            Object result = Inspector.invoke(account, "deposit", 500L);

            assertThat(result).isNull();
            assertThat(account.balanceCents()).isEqualTo(2_000);
        }

        @Test
        @DisplayName("describeRecord() lists the components in the format of a record's toString()")
        void describeRecord() {
            assertThat(Inspector.describeRecord(new Point(1, 2))).isEqualTo("Point[x=1, y=2]");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("fieldValues() leaves out static fields")
        void fieldValuesSkipsStatic() {
            assertThat(Inspector.fieldValues(new Account("mina"))).doesNotContainKey("BANK");
        }

        @Test
        @DisplayName("fieldValues() keeps fields whose value is null")
        void fieldValuesKeepsNull() {
            assertThat(Inspector.fieldValues(new Account("mina"))).containsEntry("note", null);
        }

        @Test
        @DisplayName("fieldValues() leaves out the fields of superclasses")
        void fieldValuesSkipsSuperclass() {
            assertThat(Inspector.fieldValues(new SavingsAccount("mina", 0.02))).containsExactly(entry("rate", 0.02));
        }

        @Test
        @DisplayName("fieldValues() leaves out synthetic fields such as this$0")
        void fieldValuesSkipsSynthetic() {
            var badge = new Badge();

            assertThat(Inspector.fieldValues(badge)).containsExactly(entry("label", "VIP"));
        }

        @Test
        @DisplayName("fieldValues() of an object without instance fields is an empty map")
        void fieldValuesOfObject() {
            assertThat(Inspector.fieldValues(new Object())).isEmpty();
        }

        @Test
        @DisplayName("fieldValues() returns a new map that the caller may change")
        void fieldValuesIsCallersMap() {
            SequencedMap<String, Object> values = Inspector.fieldValues(new Point(1, 2));

            values.put("z", 3);

            assertThat(Inspector.fieldValues(new Point(1, 2))).doesNotContainKey("z");
        }

        @Test
        @DisplayName("newInstance() accepts a wrapper value for a primitive parameter")
        void newInstanceUnboxes() {
            Temperature temperature = Inspector.newInstance(Temperature.class, 21.5);

            assertThat(temperature.celsius()).isEqualTo(21.5);
        }

        @Test
        @DisplayName("newInstance() lets null fit a parameter that isn't primitive")
        void newInstanceWithNull() {
            Account account = Inspector.newInstance(Account.class, (Object) null);

            assertThat(account.owner()).isNull();
        }

        @Test
        @DisplayName("newInstance() picks the only constructor that fits, even among overloads")
        void newInstanceAmongOverloads() {
            Message message = Inspector.newInstance(Message.class, 42);

            assertThat(message.kind()).isEqualTo("object");
        }

        @Test
        @DisplayName("invoke() finds public methods inherited from a superclass")
        void invokeInheritedMethod() {
            var savings = new SavingsAccount("mina", 0.02);

            Inspector.invoke(savings, "deposit", 100L);

            assertThat(Inspector.invoke(savings, "balanceCents")).isEqualTo(100L);
        }

        @Test
        @DisplayName("invoke() chooses an overload by the number of arguments")
        void invokeChoosesOverload() {
            var account = new Account("mina");

            Inspector.invoke(account, "deposit", 300L, "birthday");

            assertThat(account.balanceCents()).isEqualTo(300);
            assertThat(account.note).isEqualTo("birthday");
        }

        @Test
        @DisplayName("invoke() skips the bridge method of a generic interface, so the call isn't ambiguous")
        void invokeSkipsBridgeMethods() {
            assertThat(Inspector.invoke(new Ticket(1), "compareTo", new Ticket(2))).isEqualTo(-1);
        }

        @Test
        @DisplayName("invoke() works on JDK classes and boxes a primitive result")
        void invokeOnJdkClass() {
            assertThat(Inspector.invoke("text", "length")).isEqualTo(4);
        }

        @Test
        @DisplayName("describeRecord() ignores a custom toString()")
        void describeIgnoresToString() {
            assertThat(Inspector.describeRecord(new Fraction(1, 2))).isEqualTo("Fraction[numerator=1, denominator=2]");
        }

        @Test
        @DisplayName("describeRecord() reads the values through the accessor methods")
        void describeUsesAccessors() {
            assertThat(Inspector.describeRecord(new Shout("hi"))).isEqualTo("Shout[word=HI]");
        }

        @Test
        @DisplayName("describeRecord() of a record without components")
        void describeEmptyRecord() {
            assertThat(Inspector.describeRecord(new Empty())).isEqualTo("Empty[]");
        }

        @Test
        @DisplayName("describeRecord() shows a null value as null")
        void describeNull() {
            assertThat(Inspector.describeRecord(new Named(null))).isEqualTo("Named[name=null]");
        }

        @Test
        @DisplayName("describeRecord() uses the toString() of the component values")
        void describeNestedRecord() {
            var segment = new Segment(new Point(0, 0), new Point(3, 4));

            assertThat(Inspector.describeRecord(segment))
                    .isEqualTo("Segment[from=Point[x=0, y=0], to=Point[x=3, y=4]]");
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("fieldValues() can't open the private fields of JDK classes (module system)")
        void fieldValuesOfJdkClass() {
            assertThatThrownBy(() -> Inspector.fieldValues("text")).isInstanceOf(InaccessibleObjectException.class);
        }

        @Test
        @DisplayName("newInstance() rejects arguments that fit no public constructor")
        void newInstanceNoFit() {
            assertThatThrownBy(() -> Inspector.newInstance(Account.class, 1, 2, 3))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Account");
        }

        @Test
        @DisplayName("newInstance() doesn't widen: an Integer doesn't fit a long parameter")
        void newInstanceDoesNotWiden() {
            assertThatThrownBy(() -> Inspector.newInstance(Account.class, "mina", 1_500))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("newInstance() doesn't let null fit a primitive parameter")
        void newInstanceNullForPrimitive() {
            assertThatThrownBy(() -> Inspector.newInstance(Temperature.class, (Object) null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("newInstance() rejects arguments that fit several constructors")
        void newInstanceAmbiguous() {
            assertThatThrownBy(() -> Inspector.newInstance(Message.class, "hello"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Message");
        }

        @Test
        @DisplayName("newInstance() ignores constructors that aren't public")
        void newInstancePrivateConstructor() {
            assertThatThrownBy(() -> Inspector.newInstance(Singleton.class))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("newInstance() rejects an abstract class")
        void newInstanceAbstract() {
            assertThatThrownBy(() -> Inspector.newInstance(Shape.class))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("newInstance() rethrows a RuntimeException from the constructor unchanged")
        void newInstanceRethrowsRuntimeException() {
            assertThatThrownBy(() -> Inspector.newInstance(Temperature.class, -300.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("below absolute zero: -300.0");
        }

        @Test
        @DisplayName("newInstance() wraps a checked exception from the constructor in UndeclaredThrowableException")
        void newInstanceWrapsCheckedException() {
            assertThatThrownBy(() -> Inspector.newInstance(ConfigFile.class, "app.yml"))
                    .isInstanceOf(UndeclaredThrowableException.class)
                    .hasCauseInstanceOf(IOException.class);
        }

        @Test
        @DisplayName("invoke() rejects a method name that doesn't exist")
        void invokeUnknownMethod() {
            assertThatThrownBy(() -> Inspector.invoke(new Account("mina"), "withdraw", 100L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("withdraw");
        }

        @Test
        @DisplayName("invoke() can't see private methods")
        void invokePrivateMethod() {
            assertThatThrownBy(() -> Inspector.invoke(new Account("mina"), "audit"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("invoke() rethrows a RuntimeException from the method: the same object, not a wrapper")
        void invokeRethrowsRuntimeException() {
            var problem = new IllegalStateException("smoke detected");

            assertThatThrownBy(() -> Inspector.invoke(new Alarm(), "trigger", problem)).isSameAs(problem);
        }

        @Test
        @DisplayName("invoke() rethrows an Error from the method unchanged")
        void invokeRethrowsError() {
            assertThatThrownBy(() -> Inspector.invoke(new Alarm(), "panic"))
                    .isInstanceOf(AssertionError.class)
                    .hasMessage("broken invariant");
        }

        @Test
        @DisplayName("invoke() wraps a checked exception from the method in UndeclaredThrowableException")
        void invokeWrapsCheckedException() {
            assertThatThrownBy(() -> Inspector.invoke(new Alarm(), "check"))
                    .isInstanceOf(UndeclaredThrowableException.class)
                    .hasCauseInstanceOf(IOException.class);
        }

        @Test
        @DisplayName("every method rejects null")
        void rejectsNull() {
            assertThatThrownBy(() -> Inspector.fieldValues(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Inspector.newInstance(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Inspector.invoke(null, "toString")).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Inspector.invoke(new Account("mina"), null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Inspector.describeRecord(null)).isInstanceOf(NullPointerException.class);
        }
    }
}
