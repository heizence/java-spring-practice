package practice.j05.ex07;

import java.util.List;
import java.util.Map;

/**
 * Puts courses in an order that respects their prerequisites. Build tools, database migrations and dependency
 * injection containers solve the same problem.
 */
public final class CoursePlanner {

    private CoursePlanner() {
    }

    /**
     * Every course, in an order where each course comes after all of its prerequisites (a "topological order").
     *
     * <h2>Rules</h2>
     * <ul>
     *   <li>{@code prerequisites} maps a course to the courses you must take before it. A course that only
     *       appears in a list of prerequisites is a course, too, and is part of the result.</li>
     *   <li>When several courses are ready (all of their prerequisites are placed), the alphabetically first
     *       one comes next ({@code String.compareTo}). So the same input always gives the same order.</li>
     *   <li>If the prerequisites contain a cycle (including a course that requires itself), no valid order
     *       exists: throws {@link IllegalStateException} whose message names every course that could not be
     *       placed, and no course that could.</li>
     *   <li>A prerequisite listed twice for the same course is harmless. The input map isn't changed.</li>
     *   <li>A {@code null} map, course, list or prerequisite throws {@link NullPointerException}.</li>
     *   <li>Use Kahn's algorithm, with a {@link java.util.PriorityQueue} for the courses that are ready.</li>
     * </ul>
     */
    public static List<String> order(Map<String, List<String>> prerequisites) {
        // TODO: J05-07
        throw new UnsupportedOperationException("TODO: J05-07");
    }
}
