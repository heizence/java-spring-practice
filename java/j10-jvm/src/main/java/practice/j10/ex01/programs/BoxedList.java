package practice.j10.ex01.programs;

import java.util.ArrayList;
import java.util.List;

/** Stores the numbers from 0 to 999,999 in a {@code List<Integer>}. */
public final class BoxedList {

    private BoxedList() {
    }

    public static void main(String[] args) {
        List<Integer> numbers = new ArrayList<>();
        for (int i = 0; i < 1_000_000; i++) {
            numbers.add(i);
        }
        System.out.println(numbers.size() + " numbers stored");
    }
}
