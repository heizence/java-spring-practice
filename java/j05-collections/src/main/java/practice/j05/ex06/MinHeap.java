package practice.j05.ex06;

import java.util.NoSuchElementException;

/**
 * A priority queue that always hands out its smallest element, built as a binary min-heap: the idea behind
 * {@link java.util.PriorityQueue}.
 *
 * <p>A binary heap is a tree stored in an array without any links. The element at index {@code i} has its
 * children at {@code 2 * i + 1} and {@code 2 * i + 2}, and its parent at {@code (i - 1) / 2}. The heap rule:
 * no element is smaller than its parent, so the smallest element is always at index 0.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>{@link #offer} adds an element and {@link #poll} removes and returns the smallest one, both in
 *       O(log n). {@link #peek} returns the smallest element without removing it, in O(1).</li>
 *   <li>Elements are compared with {@code compareTo}. Equal elements are all kept, and they come out one
 *       after another, in any order among themselves.</li>
 *   <li>{@link #poll} and {@link #peek} on an empty heap throw {@link NoSuchElementException}.</li>
 *   <li>{@code null} elements are rejected with {@link NullPointerException}.</li>
 *   <li>Store the heap in an {@link java.util.ArrayList} (or an array), not in a {@code PriorityQueue}.</li>
 * </ul>
 *
 * @param <E> element type
 */
public class MinHeap<E extends Comparable<? super E>> {

    /** Adds {@code element}. */
    public void offer(E element) {
        // TODO: J05-06
        throw new UnsupportedOperationException("TODO: J05-06");
    }

    /** Removes the smallest element and returns it. */
    public E poll() {
        // TODO: J05-06
        throw new UnsupportedOperationException("TODO: J05-06");
    }

    /** Returns the smallest element without removing it. */
    public E peek() {
        // TODO: J05-06
        throw new UnsupportedOperationException("TODO: J05-06");
    }

    /** The number of elements. */
    public int size() {
        // TODO: J05-06
        throw new UnsupportedOperationException("TODO: J05-06");
    }

    /** Whether the heap has no elements. */
    public boolean isEmpty() {
        // TODO: J05-06
        throw new UnsupportedOperationException("TODO: J05-06");
    }
}
