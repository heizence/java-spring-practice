package practice.j05.ex06;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

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

    private final List<E> elements = new ArrayList<>();

    /** Adds {@code element}. */
    public void offer(E element) {
        Objects.requireNonNull(element, "element");
        // Put the new element in the next free spot, at the end, then let it rise to where it belongs.
        elements.add(element);
        siftUp(elements.size() - 1);
    }

    /** Removes the smallest element and returns it. */
    public E poll() {
        E smallest = peek(); // throws if the heap is empty
        E last = elements.removeLast();
        if (!elements.isEmpty()) {
            // Fill the hole at the root with the last element, then let it sink to where it belongs.
            elements.set(0, last);
            siftDown(0);
        }
        return smallest;
    }

    /** Returns the smallest element without removing it. */
    public E peek() {
        if (elements.isEmpty()) {
            throw new NoSuchElementException("the heap is empty");
        }
        return elements.getFirst();
    }

    /** The number of elements. */
    public int size() {
        return elements.size();
    }

    /** Whether the heap has no elements. */
    public boolean isEmpty() {
        return elements.isEmpty();
    }

    // Swap the element with its parent while it is smaller than the parent.
    // The heap is about log n levels high, so this takes at most about log n swaps.
    private void siftUp(int index) {
        while (index > 0) {
            int parent = (index - 1) / 2;
            if (elements.get(index).compareTo(elements.get(parent)) >= 0) {
                return; // the heap rule holds again
            }
            Collections.swap(elements, index, parent);
            index = parent;
        }
    }

    // Swap the element with its smaller child while that child is smaller than the element.
    // It must be the smaller child: that child becomes the parent of the other one.
    private void siftDown(int index) {
        int size = elements.size();
        while (true) {
            int left = 2 * index + 1;
            if (left >= size) {
                return; // no children: a leaf
            }
            int right = left + 1;
            int smaller = right < size && elements.get(right).compareTo(elements.get(left)) < 0 ? right : left;
            if (elements.get(index).compareTo(elements.get(smaller)) <= 0) {
                return; // the heap rule holds again
            }
            Collections.swap(elements, index, smaller);
            index = smaller;
        }
    }
}
