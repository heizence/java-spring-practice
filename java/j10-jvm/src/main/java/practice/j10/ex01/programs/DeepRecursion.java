package practice.j10.ex01.programs;

/** Adds up the numbers from 1 to 1,000,000 with a recursive method. */
public final class DeepRecursion {

    private DeepRecursion() {
    }

    public static void main(String[] args) {
        System.out.println("sum = " + sumUpTo(1_000_000));
    }

    static long sumUpTo(long n) {
        if (n == 0) {
            return 0; // the base case: the recursion does stop
        }
        return n + sumUpTo(n - 1);
    }
}
