package practice.j02.ex03;

/**
 * A rectangle. {@link Shape} lists the rules that every shape follows.
 *
 * <p>This class is not {@code final}, because {@link Square} extends it. That is safe only because rectangles are
 * immutable. If {@code Rectangle} had a {@code setWidth} method, a {@code Square} would have to change its height
 * too, and code written for rectangles ("set the width, the height stays the same") would break.
 */
public class Rectangle implements Shape {

    /** A rectangle with the given width and height. */
    public Rectangle(double width, double height) {
        // TODO: J02-03
        throw new UnsupportedOperationException("TODO: J02-03");
    }

    /** The width. */
    public double width() {
        // TODO: J02-03
        throw new UnsupportedOperationException("TODO: J02-03");
    }

    /** The height. */
    public double height() {
        // TODO: J02-03
        throw new UnsupportedOperationException("TODO: J02-03");
    }

    @Override
    public double area() {
        // TODO: J02-03
        throw new UnsupportedOperationException("TODO: J02-03");
    }

    @Override
    public double perimeter() {
        // TODO: J02-03
        throw new UnsupportedOperationException("TODO: J02-03");
    }

    @Override
    public String name() {
        // TODO: J02-03
        throw new UnsupportedOperationException("TODO: J02-03");
    }
}
