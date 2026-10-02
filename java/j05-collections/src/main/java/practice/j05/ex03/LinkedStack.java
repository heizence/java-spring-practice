package practice.j05.ex03;

import java.util.NoSuchElementException;

/**
 * A last-in, first-out (LIFO) stack built from linked nodes: the element you pushed last is the first one
 * you get back.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>{@link #push} puts an element on top. {@link #pop} removes the top element and returns it.
 *       {@link #peek} returns the top element without removing it.</li>
 *   <li>{@link #pop} and {@link #peek} on an empty stack throw {@link NoSuchElementException}.</li>
 *   <li>{@code null} elements are rejected with {@link NullPointerException}, as {@link java.util.ArrayDeque}
 *       does.</li>
 *   <li>Every operation takes constant time, O(1), however many elements the stack holds.</li>
 *   <li>Link your own nodes: don't use a {@code java.util} collection inside.</li>
 * </ul>
 *
 * @param <E> element type
 */
public class LinkedStack<E> {

    /** Puts {@code element} on top of the stack. */
    public void push(E element) {
        // TODO: J05-03
        throw new UnsupportedOperationException("TODO: J05-03");
    }

    /** Removes the top element and returns it. */
    public E pop() {
        // TODO: J05-03
        throw new UnsupportedOperationException("TODO: J05-03");
    }

    /** Returns the top element without removing it. */
    public E peek() {
        // TODO: J05-03
        throw new UnsupportedOperationException("TODO: J05-03");
    }

    /** The number of elements on the stack. */
    public int size() {
        // TODO: J05-03
        throw new UnsupportedOperationException("TODO: J05-03");
    }

    /** Whether the stack has no elements. */
    public boolean isEmpty() {
        // TODO: J05-03
        throw new UnsupportedOperationException("TODO: J05-03");
    }
}
