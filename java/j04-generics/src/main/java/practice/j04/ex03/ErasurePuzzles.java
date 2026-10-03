package practice.j04.ex03;

import java.util.ArrayList;
import java.util.List;

/**
 * J04-03, second part · Erasure predictions.
 *
 * <p>Each method is a small puzzle about what type erasure keeps and what it throws away. <strong>Don't run
 * anything yet.</strong> Decide what each method returns, write your prediction in {@link ErasureAnswers}, then
 * run {@code TypedMapTest}. The module README explains every answer.
 */
final class ErasurePuzzles {

    private ErasurePuzzles() {
    }

    static boolean sameClassForEveryTypeArgument() {
        List<String> strings = new ArrayList<>();
        List<Integer> numbers = new ArrayList<>();
        return strings.getClass() == numbers.getClass();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static int addThroughRawType() {
        List<String> names = new ArrayList<>();
        List raw = names;             // a raw type: no type checks at all
        raw.add(42);
        return names.size();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static Outcome readFromPollutedList() {
        List<String> names = new ArrayList<>();
        List raw = names;
        raw.add(42);
        try {
            String first = names.get(0);
            return Outcome.RETURNS_NORMALLY;
        } catch (ClassCastException e) {
            return Outcome.THROWS_CLASS_CAST_EXCEPTION;
        }
    }

    static Outcome storeInCovariantArray() {
        Object[] objects = new String[1];   // allowed: a String[] is an Object[]
        try {
            objects[0] = 42;
            return Outcome.RETURNS_NORMALLY;
        } catch (ArrayStoreException e) {
            return Outcome.THROWS_ARRAY_STORE_EXCEPTION;
        }
    }

    static boolean primitiveClassIsInstance() {
        return int.class.isInstance(42);
    }

    static boolean subclassKeepsTypeArgument() {
        Object list = new ArrayList<String>() { };   // an anonymous subclass of ArrayList<String>
        String superclass = list.getClass().getGenericSuperclass().getTypeName();
        return superclass.contains("String");
    }

    /** How a puzzle ends: with a value, or with an exception. */
    enum Outcome {
        RETURNS_NORMALLY,
        THROWS_CLASS_CAST_EXCEPTION,
        THROWS_ARRAY_STORE_EXCEPTION
    }
}
