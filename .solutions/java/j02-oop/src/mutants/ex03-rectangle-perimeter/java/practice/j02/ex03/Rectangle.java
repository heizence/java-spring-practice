package practice.j02.ex03;

/**
 * A rectangle. {@link Shape} lists the rules that every shape follows.
 *
 * <p>This class is not {@code final}, because {@link Square} extends it. That is safe only because rectangles are
 * immutable. If {@code Rectangle} had a {@code setWidth} method, a {@code Square} would have to change its height
 * too, and code written for rectangles ("set the width, the height stays the same") would break.
 */
public class Rectangle implements Shape {

    private final double width;
    private final double height;

    /** A rectangle with the given width and height. */
    public Rectangle(double width, double height) {
        this.width = Dimensions.requirePositive(width, "width");
        this.height = Dimensions.requirePositive(height, "height");
    }

    /** The width. */
    public double width() {
        return width;
    }

    /** The height. */
    public double height() {
        return height;
    }

    @Override
    public double area() {
        return width * height;
    }

    @Override
    public double perimeter() {
        return 2 * width + height;
    }

    @Override
    public String name() {
        return "rectangle";
    }
}
