package practice.j05.ex07;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;

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
        Objects.requireNonNull(prerequisites, "prerequisites");
        // The graph has an edge from each prerequisite to each course that needs it.
        Map<String, Set<String>> unlocks = new HashMap<>(); // prerequisite → the courses that need it
        Map<String, Integer> waitingFor = new HashMap<>(); // course → how many prerequisites aren't placed yet
        prerequisites.forEach((course, required) -> {
            Objects.requireNonNull(course, "course");
            Objects.requireNonNull(required, () -> "the prerequisites of " + course);
            waitingFor.putIfAbsent(course, 0);
            // Set.copyOf drops a prerequisite that is listed twice (and throws for a null one).
            for (String prerequisite : Set.copyOf(required)) {
                waitingFor.putIfAbsent(prerequisite, 0);
                unlocks.computeIfAbsent(prerequisite, key -> new HashSet<>()).add(course);
                waitingFor.merge(course, 1, Integer::sum);
            }
        });

        // Kahn's algorithm: a course is ready when it waits for nothing. Place a ready course, then every course
        // it unlocks waits for one prerequisite less. The PriorityQueue always hands out the alphabetically
        // first ready course, also one that became ready later than others.
        PriorityQueue<String> ready = new PriorityQueue<>();
        waitingFor.forEach((course, count) -> {
            if (count == 0) {
                ready.add(course);
            }
        });
        List<String> order = new ArrayList<>(waitingFor.size());
        while (!ready.isEmpty()) {
            String course = ready.poll();
            order.add(course);
            for (String next : unlocks.getOrDefault(course, Set.of())) {
                if (waitingFor.merge(next, -1, Integer::sum) == 0) {
                    ready.add(next);
                }
            }
        }

        return List.copyOf(order);
    }
}
