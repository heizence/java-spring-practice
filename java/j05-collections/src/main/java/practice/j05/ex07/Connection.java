package practice.j05.ex07;

import java.util.Objects;

/**
 * A track between two neighboring stations. Trains run on it in both directions.
 *
 * <p>This record is finished: you don't need to change it.
 *
 * @param a one station
 * @param b the other station
 */
public record Connection(String a, String b) {

    public Connection {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
    }
}
