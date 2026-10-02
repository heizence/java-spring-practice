package practice.j02.ex03;

/** A circle. {@link Shape} lists the rules that every shape follows. */
public final class Circle implements Shape {

    private final double radius;

    /** A circle with the given radius. */
    public Circle(double radius) {
        this.radius = Dimensions.requirePositive(radius, "radius");
    }

    /** The radius. */
    public double radius() {
        return radius;
    }

    @Override
    public double area() {
        return Math.PI * radius * radius;
    }

    @Override
    public double perimeter() {
        return 2 * Math.PI * radius;
    }

    @Override
    public String name() {
        return "circle";
    }
}
