package practice.j09.ex03;

/**
 * Starts and ends transactions: a much simplified version of Spring's {@code PlatformTransactionManager}. A real one
 * would talk to the database. Given code: you don't need to change it.
 */
public interface TxManager {

    /** Starts a transaction. */
    void begin();

    /** Makes the transaction's changes permanent. */
    void commit();

    /** Throws the transaction's changes away. */
    void rollback();
}
