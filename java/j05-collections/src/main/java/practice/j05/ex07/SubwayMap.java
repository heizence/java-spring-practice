package practice.j05.ex07;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

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

    /** Builds the map from the tracks between neighboring stations. */
    public SubwayMap(Collection<Connection> connections) {
        // TODO: J05-07
        throw new UnsupportedOperationException("TODO: J05-07");
    }

    /** A route with the fewest stops from {@code from} to {@code to}, or empty if there is none. */
    public Optional<List<String>> fewestStops(String from, String to) {
        // TODO: J05-07
        throw new UnsupportedOperationException("TODO: J05-07");
    }
}
