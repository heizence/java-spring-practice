package practice.j05.ex02;

import java.util.Iterator;
import java.util.NoSuchElementException;

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

    /** Creates an empty array with a capacity of {@value #DEFAULT_CAPACITY}. */
    public DynamicArray() {
        // TODO: J05-02
        throw new UnsupportedOperationException("TODO: J05-02");
    }

    /** Creates an empty array with room for {@code initialCapacity} elements before it has to grow. */
    public DynamicArray(int initialCapacity) {
        // TODO: J05-02
        throw new UnsupportedOperationException("TODO: J05-02");
    }

    /** The number of elements. */
    public int size() {
        // TODO: J05-02
        throw new UnsupportedOperationException("TODO: J05-02");
    }

    /** Whether there are no elements. */
    public boolean isEmpty() {
        // TODO: J05-02
        throw new UnsupportedOperationException("TODO: J05-02");
    }

    /** The element at {@code index}. */
    public E get(int index) {
        // TODO: J05-02
        throw new UnsupportedOperationException("TODO: J05-02");
    }

    /** Replaces the element at {@code index} and returns the element that was there before. */
    public E set(int index, E element) {
        // TODO: J05-02
        throw new UnsupportedOperationException("TODO: J05-02");
    }

    /** Appends {@code element} at the end. */
    public void add(E element) {
        // TODO: J05-02
        throw new UnsupportedOperationException("TODO: J05-02");
    }

    /** Inserts {@code element} at {@code index}, moving the element there and all later ones one place right. */
    public void add(int index, E element) {
        // TODO: J05-02
        throw new UnsupportedOperationException("TODO: J05-02");
    }

    /** Removes the element at {@code index}, moves all later elements one place left and returns the removed one. */
    public E remove(int index) {
        // TODO: J05-02
        throw new UnsupportedOperationException("TODO: J05-02");
    }

    /** The index of the first element equal to {@code element}, or -1 if there is none. */
    public int indexOf(Object element) {
        // TODO: J05-02
        throw new UnsupportedOperationException("TODO: J05-02");
    }

    /** Whether some element is equal to {@code element}. */
    public boolean contains(Object element) {
        // TODO: J05-02
        throw new UnsupportedOperationException("TODO: J05-02");
    }

    /** The length of the backing array: how many elements fit before it has to grow. */
    public int capacity() {
        // TODO: J05-02
        throw new UnsupportedOperationException("TODO: J05-02");
    }

    /** Iterates over the elements from index 0 to {@code size() - 1}. */
    @Override
    public Iterator<E> iterator() {
        // TODO: J05-02
        throw new UnsupportedOperationException("TODO: J05-02");
    }
}
