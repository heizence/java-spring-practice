package com.example.bookstore.support;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.hibernate.resource.jdbc.spi.StatementInspector;

/**
 * Counts the SQL statements that Hibernate sends to the database, so a test can catch N+1 query problems.
 *
 * <p>Integration tests get one counter per application context ({@link IntegrationTestConfiguration} registers it
 * with Hibernate). It is emptied right before each test method runs, after any {@code @BeforeEach} methods; call
 * {@link #reset()} yourself right before the step you measure if the test does other database work first:
 *
 * <pre>{@code
 * @Autowired SqlStatementCounter sqlStatements;
 *
 * @Test
 * void listsOrdersWithTwoQueries() {
 *     mvc.get().uri("/api/orders").exchange();
 *
 *     assertThat(sqlStatements.count()).as("%s", sqlStatements).isEqualTo(2);
 * }
 * }</pre>
 *
 * <p>Things to know:
 * <ul>
 *   <li>Only Hibernate's statements count. Flyway's and those you run through {@code JdbcTemplate} or
 *       {@code JdbcClient} don't.</li>
 *   <li>Entities that the persistence context already holds load without SQL. In a {@code @Transactional} test, data
 *       you read or saved earlier in the test is still there: call {@code entityManager.flush()} and
 *       {@code entityManager.clear()} before the step you measure.</li>
 *   <li>Statements from every thread count, so stop background work (such as {@code @Async} methods) from earlier
 *       steps before you measure. The counter itself is safe to use from several threads.</li>
 * </ul>
 */
public final class SqlStatementCounter implements StatementInspector {

    private final Queue<String> statements = new ConcurrentLinkedQueue<>();

    /** Called by Hibernate for every statement it prepares; records it and returns it unchanged. */
    @Override
    public String inspect(String sql) {
        statements.add(sql);
        return sql;
    }

    /** Forgets the statements recorded so far. */
    public void reset() {
        statements.clear();
    }

    /** How many statements Hibernate prepared since the last {@link #reset()}. */
    public int count() {
        return statements.size();
    }

    /** The statements since the last {@link #reset()}, oldest first. */
    public List<String> statements() {
        return List.copyOf(statements);
    }

    /** The statements as a numbered list, for assertion messages: {@code .as("%s", sqlStatements)}. */
    @Override
    public String toString() {
        List<String> recorded = statements();
        var text = new StringBuilder().append(recorded.size()).append(" SQL statement(s)");
        for (int i = 0; i < recorded.size(); i++) {
            text.append(System.lineSeparator()).append(i + 1).append(". ").append(recorded.get(i));
        }
        return text.toString();
    }
}
