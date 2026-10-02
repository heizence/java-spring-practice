package practice.j02.ex04;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

/**
 * A set that counts how many elements callers <em>tried</em> to add, for usage metrics.
 *
 * <h2>Intended behavior</h2>
 * <ul>
 *   <li>Wraps the set passed to the constructor and works with any {@link Set} implementation. Every operation goes
 *       to the wrapped set, so the wrapped set decides how elements behave: a {@code TreeSet} keeps its sort order,
 *       a set with a comparator uses that comparator to find duplicates, and an unmodifiable set stays
 *       unmodifiable. Changes made through this object are visible in the wrapped set, and the other way round.</li>
 *   <li>{@link #addCount()} counts every element passed to {@link #add} or {@link #addAll}, including duplicates
 *       that the set doesn't store. Elements that were already in the wrapped set before it was wrapped don't
 *       count.</li>
 *   <li>Everything else, including {@code equals}, {@code hashCode} and {@code toString}, behaves exactly like the
 *       wrapped set.</li>
 *   <li>A {@code null} set throws {@link NullPointerException}.</li>
 * </ul>
 *
 * @param <E> the element type
 */
public final class InstrumentedSet<E> implements Set<E> {

    // Composition: the wrapped set is a private field, not a superclass. This class sees only the public methods
    // of Set, so how the wrapped set works inside (for example, whether its addAll calls its own add) can't
    // change the count. And because the field's type is the Set interface, any implementation works.
    private final Set<E> set;
    private int addCount;

    /** Wraps {@code set}. */
    public InstrumentedSet(Set<E> set) {
        this.set = new HashSet<>(Objects.requireNonNull(set, "set"));
    }

    @Override
    public boolean add(E element) {
        addCount++;
        return set.add(element);
    }

    @Override
    public boolean addAll(Collection<? extends E> elements) {
        addCount += elements.size();
        // If the wrapped set's addAll calls add() for each element, it calls ITS add(), not ours, so nothing is
        // counted twice.
        return set.addAll(elements);
    }

    /** How many elements callers tried to add, including duplicates. */
    public int addCount() {
        return addCount;
    }

    // Every method below forwards to the wrapped set unchanged. IDEs can write them for you
    // (IntelliJ IDEA: Code > Delegate Methods).

    @Override
    public int size() {
        return set.size();
    }

    @Override
    public boolean isEmpty() {
        return set.isEmpty();
    }

    @Override
    public boolean contains(Object element) {
        return set.contains(element);
    }

    @Override
    public Iterator<E> iterator() {
        return set.iterator();
    }

    @Override
    public Object[] toArray() {
        return set.toArray();
    }

    @Override
    public <T> T[] toArray(T[] array) {
        return set.toArray(array);
    }

    @Override
    public boolean remove(Object element) {
        return set.remove(element);
    }

    @Override
    public boolean containsAll(Collection<?> elements) {
        return set.containsAll(elements);
    }

    @Override
    public boolean retainAll(Collection<?> elements) {
        return set.retainAll(elements);
    }

    @Override
    public boolean removeAll(Collection<?> elements) {
        return set.removeAll(elements);
    }

    @Override
    public void clear() {
        set.clear();
    }

    // equals, hashCode and toString come from Object, not from the Set interface, so the compiler doesn't remind
    // you to forward them. Without them, this set would equal only itself.

    @Override
    public boolean equals(Object other) {
        return other == this || set.equals(other);
    }

    @Override
    public int hashCode() {
        return set.hashCode();
    }

    @Override
    public String toString() {
        return set.toString();
    }
}
