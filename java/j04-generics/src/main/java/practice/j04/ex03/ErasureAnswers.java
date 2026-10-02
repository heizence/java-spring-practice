package practice.j04.ex03;

import practice.j04.ex03.ErasurePuzzles.Outcome;

/**
 * Your answer sheet for the erasure predictions in J04-03. Replace each {@code null} with what you predict the
 * matching method in {@link ErasurePuzzles} returns, then run {@code TypedMapTest}.
 *
 * <p>The constant's type tells you what kind of value to write: {@code true}/{@code false} for a
 * {@code Boolean}, a number for an {@code Integer}, text in quotes for a {@code String}, and for example
 * {@code Outcome.RETURNS_NORMALLY} for an {@code Outcome}.
 */
final class ErasureAnswers {

    private ErasureAnswers() {
    }

    // After erasure, ArrayList<String> and ArrayList<Integer> are both plain ArrayList: one class at runtime.
    static final Boolean SAME_CLASS_FOR_EVERY_TYPE_ARGUMENT = true;
    // A raw List skips every type check, and at runtime the list holds Objects, so adding 42 works.
    static final Integer ADD_THROUGH_RAW_TYPE = 1;
    // The compiler inserted a hidden (String) cast where the element is read. That is where the Integer fails:
    // far away from the line that caused the problem.
    static final Outcome READ_FROM_POLLUTED_LIST = Outcome.THROWS_CLASS_CAST_EXCEPTION;
    // Arrays know their element type at runtime (they are "reified"), so the JVM checks every store.
    static final Outcome STORE_IN_COVARIANT_ARRAY = Outcome.THROWS_ARRAY_STORE_EXCEPTION;
    // 42 is boxed to an Integer, and nothing is an instance of int.class: primitives aren't objects.
    static final Boolean PRIMITIVE_CLASS_IS_INSTANCE = false;
    // Erasure removes type arguments from objects, not from class declarations. The anonymous class was
    // declared as "extends ArrayList<String>", and its class file remembers that. Type tokens build on this.
    static final String TYPE_ARGUMENT_OF_SUBCLASS = "java.util.ArrayList<java.lang.String>";
}
