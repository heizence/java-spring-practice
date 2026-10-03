package com.example.bookstore.isolation;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.transaction.annotation.Isolation;

/**
 * Opens {@link DatabaseSession}s for the isolation puzzles, and closes the ones a puzzle left open.
 *
 * <p>Each session gets a new connection straight from the JDBC driver, not one from the application's connection
 * pool, so that its settings (isolation level, lock wait timeout) never leak into connections the application uses
 * later.
 */
final class Sessions {

    private final JdbcConnectionDetails database;
    private final List<DatabaseSession> opened = new ArrayList<>();

    Sessions(JdbcConnectionDetails database) {
        this.database = database;
    }

    /** Opens a session at an isolation level, such as {@link Isolation#READ_COMMITTED}. */
    DatabaseSession open(String name, Isolation isolation) {
        try {
            Connection connection = DriverManager.getConnection(database.getJdbcUrl(), database.getUsername(),
                    database.getPassword());
            var session = new DatabaseSession(name, connection, isolation);
            opened.add(session);
            return session;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not open session " + name, e);
        }
    }

    /** Closes every session that is still open; closing a session twice is harmless. */
    void closeAll() {
        opened.forEach(DatabaseSession::close);
        opened.clear();
    }
}
