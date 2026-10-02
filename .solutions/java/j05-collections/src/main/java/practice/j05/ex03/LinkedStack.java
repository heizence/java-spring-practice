package practice.j05.ex03;

import java.util.NoSuchElementException;
import java.util.Objects;

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

    // Each node points to the node below it. A record fits, because a node never changes once it exists:
    // push creates a new top node, and pop moves `top` one node down.
    private record Node<E>(E value, Node<E> below) {
    }

    private Node<E> top; // null when the stack is empty
    private int size;

    /** Puts {@code element} on top of the stack. */
    public void push(E element) {
        Objects.requireNonNull(element, "element");
        top = new Node<>(element, top);
        size++;
    }

    /** Removes the top element and returns it. */
    public E pop() {
        E value = peek(); // throws if the stack is empty
        top = top.below();
        size--;
        return value;
    }

    /** Returns the top element without removing it. */
    public E peek() {
        if (top == null) {
            throw new NoSuchElementException("the stack is empty");
        }
        return top.value();
    }

    /** The number of elements on the stack. */
    public int size() {
        // A counter keeps size() O(1). Counting the nodes each time would be O(n).
        return size;
    }

    /** Whether the stack has no elements. */
    public boolean isEmpty() {
        return top == null;
    }
}
