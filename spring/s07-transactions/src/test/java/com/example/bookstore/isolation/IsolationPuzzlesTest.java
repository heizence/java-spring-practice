package com.example.bookstore.isolation;

import com.example.bookstore.support.IntegrationTest;
import com.example.bookstore.support.SeedData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import practice.testkit.Predict;

/**
 * Runs each puzzle against the test database and compares the result with your answer. The puzzles commit
 * changes, so the class opts out of the test transaction and puts the seed data back after each puzzle.
 */
@IntegrationTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("S07-04 · Predict: isolation levels in MySQL")
class IsolationPuzzlesTest {

    @Autowired
    private JdbcConnectionDetails database;

    @Autowired
    private SeedData seedData;

    private Sessions sessions;

    @BeforeEach
    void openSessions() {
        sessions = new Sessions(database);
    }

    @AfterEach
    void closeSessionsAndRestoreSeedData() {
        sessions.closeAll();
        seedData.restore();
    }

    @Nested
    @DisplayName("Dirty reads")
    class DirtyReads {

        @Test
        @DisplayName("READ UNCOMMITTED: what B reads while A's change isn't committed yet")
        void readUncommitted() {
            Predict.check(IsolationAnswers.READ_UNCOMMITTED, IsolationPuzzles.readUncommitted(sessions));
        }

        @Test
        @DisplayName("READ COMMITTED: what B reads while A's change isn't committed yet")
        void readCommitted() {
            Predict.check(IsolationAnswers.READ_COMMITTED, IsolationPuzzles.readCommitted(sessions));
        }
    }

    @Nested
    @DisplayName("Non-repeatable reads")
    class NonRepeatableReads {

        @Test
        @DisplayName("READ COMMITTED: B reads, A commits a new quantity, B reads again")
        void secondReadUnderReadCommitted() {
            Predict.check(IsolationAnswers.SECOND_READ_UNDER_READ_COMMITTED,
                    IsolationPuzzles.secondReadUnderReadCommitted(sessions));
        }

        @Test
        @DisplayName("REPEATABLE READ: B reads, A commits a new quantity, B reads again")
        void secondReadUnderRepeatableRead() {
            Predict.check(IsolationAnswers.SECOND_READ_UNDER_REPEATABLE_READ,
                    IsolationPuzzles.secondReadUnderRepeatableRead(sessions));
        }

        @Test
        @DisplayName("REPEATABLE READ: B starts a transaction, A commits a new quantity, B reads for the first time")
        void firstReadAfterBegin() {
            Predict.check(IsolationAnswers.FIRST_READ_AFTER_BEGIN, IsolationPuzzles.firstReadAfterBegin(sessions));
        }
    }

    @Nested
    @DisplayName("Phantoms")
    class Phantoms {

        @Test
        @DisplayName("READ COMMITTED: B counts Minji's orders, A inserts one and commits, B counts again")
        void secondCountUnderReadCommitted() {
            Predict.check(IsolationAnswers.SECOND_COUNT_UNDER_READ_COMMITTED,
                    IsolationPuzzles.secondCountUnderReadCommitted(sessions));
        }

        @Test
        @DisplayName("REPEATABLE READ: B counts Minji's orders, A inserts one and commits, B counts again")
        void secondCountUnderRepeatableRead() {
            Predict.check(IsolationAnswers.SECOND_COUNT_UNDER_REPEATABLE_READ,
                    IsolationPuzzles.secondCountUnderRepeatableRead(sessions));
        }

        @Test
        @DisplayName("REPEATABLE READ: as before, but B counts again with FOR SHARE")
        void lockingCountUnderRepeatableRead() {
            Predict.check(IsolationAnswers.LOCKING_COUNT_UNDER_REPEATABLE_READ,
                    IsolationPuzzles.lockingCountUnderRepeatableRead(sessions));
        }

        @Test
        @DisplayName("REPEATABLE READ: B counts Minji's orders FOR UPDATE, then A inserts an order for Minji")
        void insertIntoALockedRange() {
            Predict.check(IsolationAnswers.INSERT_INTO_A_LOCKED_RANGE,
                    IsolationPuzzles.insertIntoALockedRange(sessions));
        }
    }

    @Nested
    @DisplayName("Lost updates")
    class LostUpdates {

        @Test
        @DisplayName("REPEATABLE READ: A and B both read the stock, subtract 1 in Java and write the result back")
        void twoReadModifyWrites() {
            Predict.check(IsolationAnswers.TWO_READ_MODIFY_WRITES, IsolationPuzzles.twoReadModifyWrites(sessions));
        }

        @Test
        @DisplayName("REPEATABLE READ: B reads; A and then B run quantity = quantity - 1; what B reads now")
        void twoAtomicUpdates() {
            Predict.check(IsolationAnswers.TWO_ATOMIC_UPDATES, IsolationPuzzles.twoAtomicUpdates(sessions));
        }

        @Test
        @DisplayName("SERIALIZABLE: B reads the stock, then A updates it")
        void updateAfterASerializableRead() {
            Predict.check(IsolationAnswers.UPDATE_AFTER_A_SERIALIZABLE_READ,
                    IsolationPuzzles.updateAfterASerializableRead(sessions));
        }
    }
}
