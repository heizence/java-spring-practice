package practice.j05.ex02;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * A list that grows as you add elements, backed by a plain {@code Object[]}: a small version of
 * {@link java.util.ArrayList}.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>The backing array starts with the capacity passed to the constructor, or {@value #DEFAULT_CAPACITY}.
 *       A capacity below 1 is rejected with {@link IllegalArgumentException}.</li>
 *   <li>Adding an element to a full array first doubles the capacity. The capacity never shrinks.</li>
 *   <li>{@link #get}, {@link #set} and {@link #remove} accept the indexes {@code 0} to {@code size() - 1}.
 *       {@link #add(int, Object)} also accepts {@code size()}, which appends. Any other index throws
 *       {@link IndexOutOfBoundsException} and changes nothing, not even the capacity.</li>
 *   <li>{@code null} elements are allowed. {@link #indexOf} and {@link #contains} compare elements with
 *       {@code equals()}, and they find {@code null}, too.</li>
 *   <li>The iterator returns the elements from index 0 upwards. Its {@code next()} throws
 *       {@link NoSuchElementException} when there are no more elements.</li>
 *   <li>Manage the array yourself: don't use a {@code java.util} collection inside. {@code Arrays.copyOf} and
 *       {@code System.arraycopy} are fine.</li>
 * </ul>
 *
 * @param <E> element type
 */
public class DynamicArray<E> implements Iterable<E> {

    /** The capacity of an array created with {@link #DynamicArray()}. */
    public static final int DEFAULT_CAPACITY = 10;

    // Java can't create a generic array (new E[10] doesn't compile), so the elements live in an Object[].
    // Only E values are ever stored in it, which makes the cast in elementAt() safe.
    private Object[] elements;
    private int size;

    /** Creates an empty array with a capacity of {@value #DEFAULT_CAPACITY}. */
    public DynamicArray() {
        this(DEFAULT_CAPACITY);
    }

    /** Creates an empty array with room for {@code initialCapacity} elements before it has to grow. */
    public DynamicArray(int initialCapacity) {
        if (initialCapacity < 1) {
            throw new IllegalArgumentException("initialCapacity must be at least 1, but was " + initialCapacity);
        }
        this.elements = new Object[initialCapacity];
    }

    /** The number of elements. */
    public int size() {
        return size;
    }

    /** Whether there are no elements. */
    public boolean isEmpty() {
        return size == 0;
    }

    /** The element at {@code index}. */
    public E get(int index) {
        // Objects.checkIndex throws IndexOutOfBoundsException unless 0 <= index < size. Without it, an index
        // between size and the capacity would return a stale slot of the backing array instead of failing.
        Objects.checkIndex(index, size);
        return elementAt(index);
    }

    /** Replaces the element at {@code index} and returns the element that was there before. */
    public E set(int index, E element) {
        Objects.checkIndex(index, size);
        E previous = elementAt(index);
        elements[index] = element;
        return previous;
    }

    /** Appends {@code element} at the end. */
    public void add(E element) {
        add(size, element);
    }

    /** Inserts {@code element} at {@code index}, moving the element there and all later ones one place right. */
    public void add(int index, E element) {
        // Check before growing: a call that fails must not change anything, not even the capacity.
        Objects.checkIndex(index, size + 1); // index == size is allowed here: it appends
        if (size == elements.length) {
            grow();
        }
        // Move elements[index .. size - 1] one place to the right. The source and target overlap;
        // System.arraycopy handles that correctly (a forward for loop would overwrite elements).
        System.arraycopy(elements, index, elements, index + 1, size - index);
        elements[index] = element;
        size++;
    }

    /** Removes the element at {@code index}, moves all later elements one place left and returns the removed one. */
    public E remove(int index) {
        Objects.checkIndex(index, size);
        E removed = elementAt(index);
        System.arraycopy(elements, index + 1, elements, index, size - index - 1);
        size--;
        // Clear the slot that is no longer used. Otherwise the array keeps a reference to the old last element,
        // and the garbage collector can't free it as long as this array lives.
        elements[size] = null;
        return removed;
    }

    /** The index of the first element equal to {@code element}, or -1 if there is none. */
    public int indexOf(Object element) {
        for (int i = 0; i < size; i++) {
            if (element == elements[i]) {
                return i;
            }
        }
        return -1;
    }

    /** Whether some element is equal to {@code element}. */
    public boolean contains(Object element) {
        return indexOf(element) >= 0;
    }

    /** The length of the backing array: how many elements fit before it has to grow. */
    public int capacity() {
        return elements.length;
    }

    /** Iterates over the elements from index 0 to {@code size() - 1}. */
    @Override
    public Iterator<E> iterator() {
        return new Iterator<>() {
            private int next; // the index of the element that next() returns

            @Override
            public boolean hasNext() {
                return next < size;
            }

            @Override
            public E next() {
                if (!hasNext()) {
                    throw new NoSuchElementException("no element at index " + next);
                }
                return elementAt(next++);
            }
        };
    }

    private void grow() {
        // Doubling makes add() O(1) on average: after a copy of n elements, the next n adds need no copy.
        // Growing by a fixed amount instead would copy the whole array every few adds: O(n) each.
        // (Past about a billion elements, length * 2 overflows. ArrayList guards against that; we don't.)
        elements = Arrays.copyOf(elements, elements.length * 2);
    }

    @SuppressWarnings("unchecked") // safe: only E values are ever stored in elements
    private E elementAt(int index) {
        return (E) elements[index];
    }
}
