package practice.j05.ex07;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A subway network as an undirected graph: the stations are the nodes, and each {@link Connection} is an edge
 * that trains travel in both directions.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>The map knows exactly the stations that appear in at least one connection. A connection that is
 *       listed twice is harmless.</li>
 *   <li>The map keeps its own copy of the connections: changing the collection after the constructor has no
 *       effect.</li>
 *   <li>{@link #fewestStops} returns a route with the fewest stops, as the list of stations from {@code from} to
 *       {@code to}, both included. When several routes are equally short, any of them is fine.</li>
 *   <li>The route from a station to itself is that one station. If no route exists, the result is empty.</li>
 *   <li>A station that the map doesn't know throws {@link IllegalArgumentException}. {@code null} throws
 *       {@link NullPointerException}.</li>
 *   <li>Use breadth-first search (BFS), with an {@link java.util.ArrayDeque} as the queue.</li>
 * </ul>
 */
public final class SubwayMap {

    // Adjacency list: each station maps to the stations one stop away.
    private final Map<String, List<String>> neighbors = new HashMap<>();

    /** Builds the map from the tracks between neighboring stations. */
    public SubwayMap(Collection<Connection> connections) {
        Objects.requireNonNull(connections, "connections");
        for (Connection connection : connections) {
            // An undirected graph: store every edge in both directions.
            neighbors.computeIfAbsent(connection.a(), station -> new ArrayList<>()).add(connection.b());
            neighbors.computeIfAbsent(connection.b(), station -> new ArrayList<>()).add(connection.a());
        }
    }

    /** A route with the fewest stops from {@code from} to {@code to}, or empty if there is none. */
    public Optional<List<String>> fewestStops(String from, String to) {
        requireKnown(from, "from");
        requireKnown(to, "to");
        // BFS explores in rings: first every station 1 stop away, then every station 2 stops away, and so on.
        // So the first time it reaches `to`, no shorter route can exist.
        Map<String, String> cameFrom = new HashMap<>(); // station → the station BFS reached it from
        Set<String> visited = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        visited.add(from);
        queue.add(from);
        while (!queue.isEmpty()) {
            String station = queue.poll();
            if (station.equals(to)) {
                return Optional.of(routeTo(to, cameFrom));
            }
            for (String next : neighbors.get(station)) {
                // Mark a station as visited when it joins the queue, not when it leaves it. Otherwise the same
                // station can wait in the queue many times.
                if (visited.add(next)) {
                    cameFrom.put(next, station);
                    queue.push(next);
                }
            }
        }
        return Optional.empty();
    }

    // Follows the cameFrom links back from `to` to the start (the only station without a link),
    // which gives the route backwards.
    private static List<String> routeTo(String to, Map<String, String> cameFrom) {
        List<String> route = new ArrayList<>();
        for (String station = to; station != null; station = cameFrom.get(station)) {
            route.add(station);
        }
        return List.copyOf(route.reversed());
    }

    private void requireKnown(String station, String name) {
        Objects.requireNonNull(station, name);
        if (!neighbors.containsKey(station)) {
            throw new IllegalArgumentException("unknown station: " + station);
        }
    }
}
