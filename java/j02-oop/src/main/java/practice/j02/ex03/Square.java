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
        // TODO: J02-03 (keep the super(...) call: a subclass constructor must call a constructor of its superclass)
        super(side, side);
        throw new UnsupportedOperationException("TODO: J02-03");
    }

    /** The length of each side. */
    public double side() {
        // TODO: J02-03
        throw new UnsupportedOperationException("TODO: J02-03");
    }

    @Override
    public String name() {
        // TODO: J02-03
        throw new UnsupportedOperationException("TODO: J02-03");
    }
}
