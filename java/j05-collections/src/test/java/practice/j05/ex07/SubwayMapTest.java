package practice.j05.ex07;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("J05-07 · Graphs: BFS and topological sort")
class SubwayMapTest {

    /** Connects each station to the next one: line("a", "b", "c") gives a–b and b–c. */
    private static List<Connection> line(String... stations) {
        List<Connection> connections = new ArrayList<>();
        for (int i = 0; i + 1 < stations.length; i++) {
            connections.add(new Connection(stations[i], stations[i + 1]));
        }
        return connections;
    }

    /** Checks that {@code route} starts and ends at the right stations and only uses existing connections. */
    private static void assertValidRoute(List<String> route, String from, String to, List<Connection> connections) {
        Set<Set<String>> tracks = new HashSet<>();
        for (Connection connection : connections) {
            tracks.add(Set.of(connection.a(), connection.b()));
        }
        assertThat(route.getFirst()).as("first station").isEqualTo(from);
        assertThat(route.getLast()).as("last station").isEqualTo(to);
        for (int i = 0; i + 1 < route.size(); i++) {
            assertThat(tracks).as("a track between %s and %s", route.get(i), route.get(i + 1))
                    .contains(Set.of(route.get(i), route.get(i + 1)));
        }
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("the route between two neighbors is the two stations")
        void neighbors() {
            var subway = new SubwayMap(line("A", "B"));

            assertThat(subway.fewestStops("A", "B")).contains(List.of("A", "B"));
        }

        @Test
        @DisplayName("follows a line through every station in between")
        void alongALine() {
            var subway = new SubwayMap(line("A", "B", "C", "D"));

            assertThat(subway.fewestStops("A", "D")).contains(List.of("A", "B", "C", "D"));
        }

        @Test
        @DisplayName("travels a connection in both directions")
        void bothDirections() {
            var subway = new SubwayMap(line("A", "B", "C"));

            assertThat(subway.fewestStops("C", "A")).contains(List.of("C", "B", "A"));
        }

        @Test
        @DisplayName("takes the shorter of two routes")
        void shorterOfTwoRoutes() {
            // Two ways from A to D: A–B–D (2 stops) and A–C–E–F–D (4 stops).
            var subway = new SubwayMap(List.of(
                    new Connection("A", "B"), new Connection("A", "C"), new Connection("B", "D"),
                    new Connection("C", "E"), new Connection("E", "F"), new Connection("F", "D")));

            assertThat(subway.fewestStops("A", "D")).contains(List.of("A", "B", "D"));
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("the route from a station to itself is that one station")
        void sameStation() {
            var subway = new SubwayMap(line("A", "B"));

            assertThat(subway.fewestStops("A", "A")).contains(List.of("A"));
        }

        @Test
        @DisplayName("returns empty when no route connects the two stations")
        void noRoute() {
            List<Connection> connections = new ArrayList<>(line("A", "B"));
            connections.addAll(line("X", "Y"));
            var subway = new SubwayMap(connections);

            assertThat(subway.fewestStops("A", "Y")).isEmpty();
        }

        @Test
        @DisplayName("when two routes are equally short, returns one of them")
        void tie() {
            List<Connection> square = List.of(new Connection("A", "B"), new Connection("B", "D"),
                    new Connection("A", "C"), new Connection("C", "D"));
            var subway = new SubwayMap(square);

            List<String> route = subway.fewestStops("A", "D").orElseThrow();

            assertThat(route).hasSize(3);
            assertValidRoute(route, "A", "D", square);
        }

        @Test
        @DisplayName("doesn't go around in circles on a circle line")
        void circleLine() {
            List<Connection> circle = line("A", "B", "C", "D", "E", "F", "A");
            var subway = new SubwayMap(circle);

            List<String> route = subway.fewestStops("A", "D").orElseThrow();

            assertThat(route).hasSize(4);
            assertValidRoute(route, "A", "D", circle);
        }

        @Test
        @DisplayName("finds a shortest route across a 10 x 10 grid of stations")
        void grid() {
            List<Connection> grid = new ArrayList<>();
            for (int row = 0; row < 10; row++) {
                for (int column = 0; column < 10; column++) {
                    if (column < 9) {
                        grid.add(new Connection(row + "," + column, row + "," + (column + 1)));
                    }
                    if (row < 9) {
                        grid.add(new Connection(row + "," + column, (row + 1) + "," + column));
                    }
                }
            }
            var subway = new SubwayMap(grid);

            List<String> toFarCorner = subway.fewestStops("0,0", "9,9").orElseThrow();
            List<String> alongTheEdge = subway.fewestStops("0,0", "0,9").orElseThrow();

            assertThat(toFarCorner).hasSize(19); // 9 steps down and 9 to the right, in any order
            assertValidRoute(toFarCorner, "0,0", "9,9", grid);
            assertThat(alongTheEdge).hasSize(10); // 9 steps to the right
            assertValidRoute(alongTheEdge, "0,0", "0,9", grid);
        }

        @Test
        @DisplayName("a connection listed twice is harmless")
        void duplicateConnection() {
            var subway = new SubwayMap(List.of(
                    new Connection("A", "B"), new Connection("B", "A"), new Connection("B", "C")));

            assertThat(subway.fewestStops("A", "C")).contains(List.of("A", "B", "C"));
        }

        @Test
        @DisplayName("follows a line of 10,000 stations")
        void longLine() {
            String[] stations = new String[10_000];
            for (int i = 0; i < stations.length; i++) {
                stations[i] = "S" + i;
            }
            var subway = new SubwayMap(line(stations));

            Optional<List<String>> route = subway.fewestStops("S0", "S9999");

            assertThat(route).isPresent();
            assertThat(route.get()).hasSize(10_000).startsWith("S0", "S1").endsWith("S9998", "S9999");
        }

        @Test
        @DisplayName("keeps its own copy of the connections")
        void defensiveCopy() {
            List<Connection> connections = new ArrayList<>(line("A", "B"));
            var subway = new SubwayMap(connections);

            connections.add(new Connection("B", "C"));

            assertThatThrownBy(() -> subway.fewestStops("A", "C")).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("rejects a station that the map doesn't know")
        void unknownStation() {
            var subway = new SubwayMap(line("A", "B"));

            assertThatThrownBy(() -> subway.fewestStops("Z", "A")).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> subway.fewestStops("A", "Z")).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects null stations")
        void nullStations() {
            var subway = new SubwayMap(line("A", "B"));

            assertThatThrownBy(() -> subway.fewestStops(null, "A")).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> subway.fewestStops("A", null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("rejects a null collection of connections")
        void nullConnections() {
            assertThatThrownBy(() -> new SubwayMap(null)).isInstanceOf(NullPointerException.class);
        }
    }
}
