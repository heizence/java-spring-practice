package practice.j02.ex03;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("J02-03 · Interfaces and polymorphism")
class ShapeTest {

    private static final Offset<Double> PRECISION = within(1e-9);

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("a circle's area is π·r² and its perimeter is 2·π·r")
        void circle() {
            var circle = new Circle(2);

            assertThat(circle.radius()).isEqualTo(2.0);
            assertThat(circle.area()).isCloseTo(4 * Math.PI, PRECISION);
            assertThat(circle.perimeter()).isCloseTo(4 * Math.PI, PRECISION);
        }

        @Test
        @DisplayName("a rectangle's area is width × height and its perimeter is 2 × (width + height)")
        void rectangle() {
            var rectangle = new Rectangle(3, 4.5);

            assertThat(rectangle.width()).isEqualTo(3.0);
            assertThat(rectangle.height()).isEqualTo(4.5);
            assertThat(rectangle.area()).isCloseTo(13.5, PRECISION);
            assertThat(rectangle.perimeter()).isCloseTo(15.0, PRECISION);
        }

        @Test
        @DisplayName("a square's area is side² and its perimeter is 4 × side")
        void square() {
            var square = new Square(2.5);

            assertThat(square.side()).isEqualTo(2.5);
            assertThat(square.area()).isCloseTo(6.25, PRECISION);
            assertThat(square.perimeter()).isCloseTo(10.0, PRECISION);
        }

        @Test
        @DisplayName("each shape reports its own name")
        void names() {
            assertThat(new Circle(1).name()).isEqualTo("circle");
            assertThat(new Rectangle(1, 2).name()).isEqualTo("rectangle");
            assertThat(new Square(1).name()).isEqualTo("square");
        }

        @Test
        @DisplayName("describe() shows the name, the area and the perimeter with 2 decimal places")
        void describe() {
            assertThat(new Circle(1).describe()).isEqualTo("circle with area 3.14 and perimeter 6.28");
            assertThat(new Rectangle(3, 4).describe()).isEqualTo("rectangle with area 12.00 and perimeter 14.00");
            assertThat(new Square(1.5).describe()).isEqualTo("square with area 2.25 and perimeter 6.00");
        }

        @Test
        @DisplayName("totalArea() adds up the areas of different kinds of shapes")
        void totalArea() {
            List<Shape> shapes = List.of(new Circle(1), new Rectangle(2, 3), new Square(2));

            double total = Shape.totalArea(shapes);

            assertThat(total).isCloseTo(Math.PI + 6 + 4, PRECISION);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("a square is a rectangle and a shape")
        void squareIsARectangle() {
            Object square = new Square(2);

            assertThat(square).isInstanceOf(Rectangle.class).isInstanceOf(Shape.class);
        }

        @Test
        @DisplayName("a square still calls itself a square when the variable's type is Rectangle or Shape")
        void dynamicDispatch() {
            Rectangle asRectangle = new Square(2);
            Shape asShape = new Square(2);

            assertThat(asRectangle.name()).isEqualTo("square");
            assertThat(asShape.describe()).isEqualTo("square with area 4.00 and perimeter 8.00");
        }

        @Test
        @DisplayName("describe() rounds to the nearest hundredth instead of cutting off digits")
        void describeRounds() {
            var circle = new Circle(0.5); // area 0.785398..., perimeter 3.141592...

            assertThat(circle.describe()).isEqualTo("circle with area 0.79 and perimeter 3.14");
        }

        @Test
        @DisplayName("describe() uses a dot as the decimal separator, even when the default locale uses a comma")
        void describeIgnoresDefaultLocale() {
            Locale original = Locale.getDefault();
            try {
                Locale.setDefault(Locale.GERMANY);

                String description = new Circle(1).describe();

                assertThat(description).isEqualTo("circle with area 3.14 and perimeter 6.28");
            } finally {
                Locale.setDefault(original);
            }
        }

        @Test
        @DisplayName("totalArea() of an empty list is 0")
        void totalAreaOfNothing() {
            assertThat(Shape.totalArea(List.of())).isZero();
        }

        @Test
        @DisplayName("totalArea() accepts a list of one kind of shape, such as a List<Square>")
        void totalAreaOfSquares() {
            List<Square> squares = List.of(new Square(1), new Square(2), new Square(3));

            assertThat(Shape.totalArea(squares)).isCloseTo(14.0, PRECISION);
        }

        @Test
        @DisplayName("very small and very large dimensions are allowed")
        void extremeDimensions() {
            assertThat(new Square(0.001).area()).isCloseTo(0.000001, PRECISION);
            assertThat(new Rectangle(1e10, 1e10).area()).isEqualTo(1e20);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @ParameterizedTest(name = "radius {0}")
        @ValueSource(doubles = {0.0, -0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
        @DisplayName("a circle rejects a radius that isn't a finite number greater than 0")
        void invalidRadius(double radius) {
            assertThatThrownBy(() -> new Circle(radius)).isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest(name = "width {0}")
        @ValueSource(doubles = {0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY})
        @DisplayName("a rectangle rejects a width that isn't a finite number greater than 0")
        void invalidWidth(double width) {
            assertThatThrownBy(() -> new Rectangle(width, 1)).isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest(name = "height {0}")
        @ValueSource(doubles = {0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY})
        @DisplayName("a rectangle rejects a height that isn't a finite number greater than 0")
        void invalidHeight(double height) {
            assertThatThrownBy(() -> new Rectangle(1, height)).isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest(name = "side {0}")
        @ValueSource(doubles = {0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY})
        @DisplayName("a square rejects a side that isn't a finite number greater than 0")
        void invalidSide(double side) {
            assertThatThrownBy(() -> new Square(side)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("puts the rejected value in the exception message")
        void messageContainsValue() {
            assertThatThrownBy(() -> new Circle(-1.5)).hasMessageContaining("-1.5");
            assertThatThrownBy(() -> new Rectangle(2, -7.25)).hasMessageContaining("-7.25");
            assertThatThrownBy(() -> new Square(-3.5)).hasMessageContaining("-3.5");
        }

        @Test
        @DisplayName("totalArea() rejects a null list")
        void totalAreaOfNull() {
            assertThatThrownBy(() -> Shape.totalArea(null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("totalArea() rejects a list that contains null")
        void totalAreaWithNullElement() {
            List<Shape> shapes = Arrays.asList(new Circle(1), null);

            assertThatThrownBy(() -> Shape.totalArea(shapes)).isInstanceOf(NullPointerException.class);
        }
    }
}
