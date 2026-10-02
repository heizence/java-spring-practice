package practice.j10.ex01.programs;

/** Stores the numbers from 0 to 999,999 in an {@code int[]}. */
public final class PrimitiveArray {

    private PrimitiveArray() {
    }

    public static void main(String[] args) {
        int[] numbers = new int[1_000_000];
        for (int i = 0; i < numbers.length; i++) {
            numbers[i] = i;
        }
        System.out.println(numbers.length + " numbers stored");
    }
}
