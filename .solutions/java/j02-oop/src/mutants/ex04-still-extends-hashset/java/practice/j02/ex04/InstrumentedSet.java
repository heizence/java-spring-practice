package practice.j02.ex04;

import java.util.HashSet;
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
public class InstrumentedSet<E> extends HashSet<E> {

    private int addCount;

    /** Wraps {@code set}. */
    public InstrumentedSet(Set<E> set) {
        super(set);
    }

    @Override
    public boolean add(E element) {
        addCount++;
        return super.add(element);
    }

    /** How many elements callers tried to add, including duplicates. */
    public int addCount() {
        return addCount;
    }
}
