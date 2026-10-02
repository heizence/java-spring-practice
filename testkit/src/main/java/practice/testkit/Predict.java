package practice.testkit;

import static org.assertj.core.api.Assertions.fail;

import java.util.Objects;

/**
 * Checks answers in "Predict" exercises.
 *
 * <p>In a Predict exercise you write what you think a piece of code returns into an answer sheet
 * (a class with constants that start as {@code null}). The tests compare each answer with what the code
 * really returns.
 */
public final class Predict {

    private Predict() {
    }

    /**
     * Fails unless {@code prediction} equals {@code actual}. A {@code null} prediction means
     * "not answered yet" and always fails.
     */
    public static void check(Object prediction, Object actual) {
        if (prediction == null) {
            fail("Not answered yet: replace null with your prediction in the answer sheet.");
        }
        if (!Objects.equals(prediction, actual)) {
            fail("You predicted %s, but the code gives %s.".formatted(describe(prediction), describe(actual)));
        }
    }

    private static String describe(Object value) {
        return switch (value) {
            case null -> "null";
            case String s -> "\"" + s + "\"";
            case Character c -> "'" + c + "'";
            case Long l -> l + "L";
            case Float f -> f + "f";
            case Enum<?> e -> e.getDeclaringClass().getSimpleName() + "." + e.name();
            case Class<?> c -> c.getSimpleName() + ".class";
            default -> value.toString();
        };
    }
}
