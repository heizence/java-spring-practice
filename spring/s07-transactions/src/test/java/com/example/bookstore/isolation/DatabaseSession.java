package com.example.bookstore.isolation;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.springframework.transaction.annotation.Isolation;

/**
 * One database session for the isolation puzzles: a JDBC connection of its own, with its own transaction, driven one
 * statement at a time. Open sessions with {@link Sessions#open}.
 *
 * <ul>
 *   <li><b>Every statement joins the session's transaction.</b> Auto-commit is off, so a statement starts a
 *       transaction when none is open, and the transaction ends only with {@link #commit()} or {@link #rollback()}.
 *       {@link #close()} rolls back whatever wasn't committed.</li>
 *   <li><b>The steps run in the order of the code.</b> A puzzle calls the methods of both sessions on one thread,
 *       one after another, so the steps interleave exactly as you read them, on every run. Nothing depends on
 *       timing.</li>
 *   <li><b>A statement that must wait for a lock gives up after one second.</b> On one thread, a statement that
 *       waits for a lock held by the other session would wait forever: the other session can't run its next step
 *       to release the lock. So each session sets {@code innodb_lock_wait_timeout = 1}, and {@link #attempt}
 *       reports the timeout as {@link Outcome#WAITS_FOR_A_LOCK}. MySQL then cancels only that statement; the
 *       session's transaction stays open.</li>
 * </ul>
 */
final class DatabaseSession implements AutoCloseable {

    /** How a statement ended. */
    enum Outcome {

        /** It ran without waiting. */
        RUNS,

        /** It had to wait for a lock that the other session holds (and gave up after one second). */
        WAITS_FOR_A_LOCK
    }

    /** MySQL's error code for "Lock wait timeout exceeded". */
    private static final int LOCK_WAIT_TIMEOUT = 1205;

    private final String name;
    private final Connection connection;

    DatabaseSession(String name, Connection connection, Isolation isolation) {
        if (isolation == Isolation.DEFAULT) {
            throw new IllegalArgumentException("Name an isolation level; DEFAULT hides which one the puzzle uses");
        }
        this.name = name;
        this.connection = connection;
        run(() -> {
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(isolation.value());
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET SESSION innodb_lock_wait_timeout = 1");
            }
            return null;
        }, "open the session");
    }

    /**
     * Starts a transaction explicitly ({@code START TRANSACTION}), without reading anything yet. Not needed before
     * other statements: they start a transaction by themselves.
     */
    void begin() {
        execute("START TRANSACTION");
    }

    /** Runs a query that returns one number, such as {@code SELECT quantity …} or {@code SELECT COUNT(*) …}. */
    int queryInt(String sql) {
        return run(() -> {
            try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
                if (!result.next()) {
                    throw new IllegalStateException("No row for: " + sql);
                }
                return result.getInt(1);
            }
        }, sql);
    }

    /**
     * Runs an {@code INSERT}, {@code UPDATE} or {@code DELETE} that must not wait for a lock.
     *
     * @return how many rows it changed
     */
    int update(String sql) {
        return run(() -> {
            try (Statement statement = connection.createStatement()) {
                return statement.executeUpdate(sql);
            }
        }, sql);
    }

    /** Runs a statement that may have to wait for a lock, and reports whether it did. */
    Outcome attempt(String sql) {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
            return Outcome.RUNS;
        } catch (SQLException e) {
            if (e.getErrorCode() == LOCK_WAIT_TIMEOUT) {
                return Outcome.WAITS_FOR_A_LOCK;
            }
            throw new IllegalStateException("Session %s failed to run: %s".formatted(name, sql), e);
        }
    }

    void commit() {
        run(() -> {
            connection.commit();
            return null;
        }, "commit");
    }

    void rollback() {
        run(() -> {
            connection.rollback();
            return null;
        }, "rollback");
    }

    /** Rolls back whatever wasn't committed, and closes the connection. */
    @Override
    public void close() {
        run(() -> {
            try (connection) {
                if (!connection.isClosed()) {
                    connection.rollback();
                }
            }
            return null;
        }, "close the session");
    }

    private void execute(String sql) {
        run(() -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            }
            return null;
        }, sql);
    }

    private <T> T run(SqlAction<T> action, String what) {
        try {
            return action.run();
        } catch (SQLException e) {
            throw new IllegalStateException("Session %s failed to %s".formatted(name, what), e);
        }
    }

    @FunctionalInterface
    private interface SqlAction<T> {
        T run() throws SQLException;
    }
}
