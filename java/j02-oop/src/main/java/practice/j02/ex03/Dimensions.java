package practice.j02.ex03;

/** The dimension check that every shape constructor shares. Package-private: it isn't part of the public API. */
final class Dimensions {

    private Dimensions() {
    }

    /** Returns {@code value} if it is a finite number greater than 0, and throws otherwise. */
    static double requirePositive(double value, String name) {
        // Double.isFinite rejects NaN and both infinities. "value <= 0" alone would let NaN through: every
        // comparison with NaN is false, so NaN <= 0 is false.
        if (!Double.isFinite(value) || value <= 0) {
            throw new IllegalArgumentException(name + " must be a finite number greater than 0, but was " + value);
        }
        return value;
    }
}
