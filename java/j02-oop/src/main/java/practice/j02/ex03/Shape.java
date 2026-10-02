package practice.j02.ex03;

import java.util.List;

/**
 * A flat shape, such as a circle or a rectangle.
 *
 * <h2>Rules for every shape</h2>
 * <ul>
 *   <li>Shapes are immutable.</li>
 *   <li>Constructors reject every dimension (radius, width, height, side) that isn't a finite number greater than 0:
 *       0, negative numbers, NaN and infinity throw {@link IllegalArgumentException} with the value in the
 *       message.</li>
 * </ul>
 */
public interface Shape {

    /** The area. */
    double area();

    /** The length of the outline. */
    double perimeter();

    /** A lowercase name, such as {@code "circle"}. */
    String name();

    /**
     * A one-line description: the name, then the area and the perimeter, each rounded to exactly 2 decimal places.
     * For a circle with radius 1 that is {@code "circle with area 3.14 and perimeter 6.28"}; for a 3 × 4 rectangle,
     * {@code "rectangle with area 12.00 and perimeter 14.00"}.
     *
     * <p>The decimal separator is always a dot, whatever the computer's default locale is. Use
     * {@code String.format(Locale.ROOT, ...)}: in a German locale, {@code "%.2f"} alone would print {@code 3,14}.
     */
    default String describe() {
        // TODO: J02-03
        throw new UnsupportedOperationException("TODO: J02-03");
    }

    /**
     * The sum of the areas of {@code shapes}; 0 for an empty list. A {@code null} list or a {@code null} element
     * throws {@link NullPointerException}.
     *
     * <p>{@code List<? extends Shape>} accepts a list of any kind of shape, for example a {@code List<Circle>}.
     * J04 explains these wildcards.
     */
    static double totalArea(List<? extends Shape> shapes) {
        // TODO: J02-03
        throw new UnsupportedOperationException("TODO: J02-03");
    }
}
