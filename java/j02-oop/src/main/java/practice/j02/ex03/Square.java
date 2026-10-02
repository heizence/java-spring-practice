package practice.j02.ex03;

/**
 * A square: a rectangle with four equal sides. {@link Shape} lists the rules that every shape follows.
 *
 * <p>{@code Square} inherits {@code area()} and {@code perimeter()} from {@link Rectangle} and only changes the
 * name.
 */
public final class Square extends Rectangle {

    /** A square with the given side length. */
    public Square(double side) {
        // Since Java 25, a constructor may run statements before super(...), as long as they don't use `this`.
        // Checking here gives an error message about "side", not about the "width" of the rectangle.
        Dimensions.requirePositive(side, "side");
        super(side, side);
    }

    /** The length of each side. */
    public double side() {
        return width();
    }

    @Override
    public String name() {
        return "square";
    }
}
