package practice.j05.ex07;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("J05-07 · Graphs: BFS and topological sort")
class CoursePlannerTest {

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("places a prerequisite before the course that needs it")
        void prerequisiteFirst() {
            var prerequisites = Map.of("calculus", List.of("algebra"));

            assertThat(CoursePlanner.order(prerequisites)).containsExactly("algebra", "calculus");
        }

        @Test
        @DisplayName("orders a chain of prerequisites")
        void chain() {
            var prerequisites = Map.of(
                    "web", List.of("databases"),
                    "databases", List.of("programming"),
                    "programming", List.<String>of());

            assertThat(CoursePlanner.order(prerequisites)).containsExactly("programming", "databases", "web");
        }

        @Test
        @DisplayName("orders courses without prerequisites alphabetically")
        void independentCourses() {
            var prerequisites = Map.of("chemistry", List.<String>of(), "art", List.<String>of(), "biology", List.<String>of());

            assertThat(CoursePlanner.order(prerequisites)).containsExactly("art", "biology", "chemistry");
        }

        @Test
        @DisplayName("places a course with two prerequisites after both of them")
        void twoPrerequisites() {
            var prerequisites = Map.of(
                    "d", List.of("b", "c"),
                    "b", List.of("a"),
                    "c", List.of("a"));

            assertThat(CoursePlanner.order(prerequisites)).containsExactly("a", "b", "c", "d");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("no courses give an empty order")
        void empty() {
            assertThat(CoursePlanner.order(Map.of())).isEmpty();
        }

        @Test
        @DisplayName("includes courses that only appear as prerequisites")
        void prerequisiteOnlyCourses() {
            var prerequisites = Map.of("calculus", List.of("geometry", "algebra"));

            assertThat(CoursePlanner.order(prerequisites)).containsExactly("algebra", "geometry", "calculus");
        }

        @Test
        @DisplayName("a course that becomes ready later can still go before an alphabetically later one")
        void readyLaterButFirstInAlphabet() {
            // Ready at the start: alpha and zeta. Placing alpha makes beta ready, and beta comes before zeta.
            var prerequisites = Map.of("beta", List.of("alpha"), "zeta", List.<String>of());

            assertThat(CoursePlanner.order(prerequisites)).containsExactly("alpha", "beta", "zeta");
        }

        @Test
        @DisplayName("an alphabetically first course still waits for its prerequisites")
        void firstInAlphabetWaits() {
            var prerequisites = Map.of("a", List.of("z"), "b", List.<String>of());

            assertThat(CoursePlanner.order(prerequisites)).containsExactly("b", "z", "a");
        }

        @Test
        @DisplayName("a prerequisite listed twice is harmless")
        void duplicatePrerequisite() {
            var prerequisites = Map.of("calculus", List.of("algebra", "algebra"));

            assertThat(CoursePlanner.order(prerequisites)).containsExactly("algebra", "calculus");
        }

        @Test
        @DisplayName("doesn't change the input map")
        void inputUnchanged() {
            Map<String, List<String>> prerequisites = new HashMap<>();
            prerequisites.put("calculus", new ArrayList<>(List.of("algebra")));
            Map<String, List<String>> copy = Map.of("calculus", List.of("algebra"));

            CoursePlanner.order(prerequisites);

            assertThat(prerequisites).isEqualTo(copy);
        }

        @Test
        @DisplayName("orders a chain of 10,000 courses")
        void longChain() {
            Map<String, List<String>> prerequisites = new HashMap<>();
            List<String> expected = new ArrayList<>();
            for (int i = 0; i < 10_000; i++) {
                String course = "c%05d".formatted(i);
                expected.add(course);
                prerequisites.put(course, i == 0 ? List.of() : List.of("c%05d".formatted(i - 1)));
            }

            assertThat(CoursePlanner.order(prerequisites)).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("rejects a cycle, naming the courses on it and the courses that depend on them")
        void cycle() {
            var prerequisites = Map.of(
                    "algebra", List.of("calculus"),
                    "calculus", List.of("algebra"),
                    "physics", List.of("calculus"),
                    "history", List.<String>of());

            assertThatThrownBy(() -> CoursePlanner.order(prerequisites))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContainingAll("algebra", "calculus", "physics")
                    .message().doesNotContain("history");
        }

        @Test
        @DisplayName("rejects a course that requires itself")
        void selfCycle() {
            var prerequisites = Map.of("art", List.of("art"), "music", List.<String>of());

            assertThatThrownBy(() -> CoursePlanner.order(prerequisites))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("art");
        }

        @Test
        @DisplayName("rejects a null map, a null list and a null prerequisite")
        void nulls() {
            Map<String, List<String>> nullList = new HashMap<>();
            nullList.put("calculus", null);
            Map<String, List<String>> nullPrerequisite = Map.of("calculus", Arrays.asList("algebra", null));

            assertThatThrownBy(() -> CoursePlanner.order(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> CoursePlanner.order(nullList)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> CoursePlanner.order(nullPrerequisite)).isInstanceOf(NullPointerException.class);
        }
    }
}
