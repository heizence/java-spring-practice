package com.example.bookstore.isolation;

import com.example.bookstore.isolation.DatabaseSession.Outcome;
import org.springframework.transaction.annotation.Isolation;

/**
 * S07-04 · Predict: isolation levels in MySQL.
 *
 * <p>Each puzzle runs two real database sessions, A and B, step by step: the statements run exactly in the order you
 * read them, and each session has its own transaction ({@link DatabaseSession} explains how). <strong>Don't run
 * anything yet.</strong> Read each puzzle, decide what it returns, and write your prediction in
 * {@link IsolationAnswers}. Then run {@code IsolationPuzzlesTest}.
 *
 * <p>Every puzzle starts from the seed data: book 1 has <b>10</b> copies in stock, and Minji (member 1) has
 * <b>14</b> orders. A session's transaction starts with its first statement and ends with {@code commit()} or
 * {@code rollback()}; closing a session rolls back whatever it didn't commit.
 */
final class IsolationPuzzles {

    private static final String STOCK_OF_BOOK_1 = "SELECT quantity FROM inventories WHERE book_id = 1";
    private static final String COUNT_MINJIS_ORDERS = "SELECT COUNT(*) FROM orders WHERE member_id = 1";
    private static final String INSERT_ORDER_OF_MINJI = """
            INSERT INTO orders (member_id, status, total_amount, ordered_at, created_at, updated_at)
            VALUES (1, 'PENDING', 0, NOW(6), NOW(6), NOW(6))""";

    private IsolationPuzzles() {
    }

    // ---- Dirty reads ---------------------------------------------------------------------------

    static Integer readUncommitted(Sessions db) {
        try (var a = db.open("A", Isolation.READ_COMMITTED);
             var b = db.open("B", Isolation.READ_UNCOMMITTED)) {
            a.update("UPDATE inventories SET quantity = 3 WHERE book_id = 1");
            int seenByB = b.queryInt(STOCK_OF_BOOK_1);
            a.rollback();
            return seenByB;
        }
    }

    static Integer readCommitted(Sessions db) {
        try (var a = db.open("A", Isolation.READ_COMMITTED);
             var b = db.open("B", Isolation.READ_COMMITTED)) {
            a.update("UPDATE inventories SET quantity = 3 WHERE book_id = 1");
            int seenByB = b.queryInt(STOCK_OF_BOOK_1);
            a.rollback();
            return seenByB;
        }
    }

    // ---- Non-repeatable reads ------------------------------------------------------------------

    static Integer secondReadUnderReadCommitted(Sessions db) {
        try (var a = db.open("A", Isolation.READ_COMMITTED);
             var b = db.open("B", Isolation.READ_COMMITTED)) {
            b.queryInt(STOCK_OF_BOOK_1);
            a.update("UPDATE inventories SET quantity = 7 WHERE book_id = 1");
            a.commit();
            return b.queryInt(STOCK_OF_BOOK_1);
        }
    }

    static Integer secondReadUnderRepeatableRead(Sessions db) {
        try (var a = db.open("A", Isolation.READ_COMMITTED);
             var b = db.open("B", Isolation.REPEATABLE_READ)) {
            b.queryInt(STOCK_OF_BOOK_1);
            a.update("UPDATE inventories SET quantity = 7 WHERE book_id = 1");
            a.commit();
            return b.queryInt(STOCK_OF_BOOK_1);
        }
    }

    static Integer firstReadAfterBegin(Sessions db) {
        try (var a = db.open("A", Isolation.READ_COMMITTED);
             var b = db.open("B", Isolation.REPEATABLE_READ)) {
            b.begin();
            a.update("UPDATE inventories SET quantity = 7 WHERE book_id = 1");
            a.commit();
            return b.queryInt(STOCK_OF_BOOK_1);
        }
    }

    // ---- Phantoms ------------------------------------------------------------------------------

    static Integer secondCountUnderReadCommitted(Sessions db) {
        try (var a = db.open("A", Isolation.READ_COMMITTED);
             var b = db.open("B", Isolation.READ_COMMITTED)) {
            b.queryInt(COUNT_MINJIS_ORDERS);
            a.update(INSERT_ORDER_OF_MINJI);
            a.commit();
            return b.queryInt(COUNT_MINJIS_ORDERS);
        }
    }

    static Integer secondCountUnderRepeatableRead(Sessions db) {
        try (var a = db.open("A", Isolation.READ_COMMITTED);
             var b = db.open("B", Isolation.REPEATABLE_READ)) {
            b.queryInt(COUNT_MINJIS_ORDERS);
            a.update(INSERT_ORDER_OF_MINJI);
            a.commit();
            return b.queryInt(COUNT_MINJIS_ORDERS);
        }
    }

    static Integer lockingCountUnderRepeatableRead(Sessions db) {
        try (var a = db.open("A", Isolation.READ_COMMITTED);
             var b = db.open("B", Isolation.REPEATABLE_READ)) {
            b.queryInt(COUNT_MINJIS_ORDERS);
            a.update(INSERT_ORDER_OF_MINJI);
            a.commit();
            return b.queryInt(COUNT_MINJIS_ORDERS + " FOR SHARE");
        }
    }

    static Outcome insertIntoALockedRange(Sessions db) {
        try (var a = db.open("A", Isolation.READ_COMMITTED);
             var b = db.open("B", Isolation.REPEATABLE_READ)) {
            b.queryInt(COUNT_MINJIS_ORDERS + " FOR UPDATE");
            Outcome insertByA = a.attempt(INSERT_ORDER_OF_MINJI);
            b.commit();
            return insertByA;
        }
    }

    // ---- Lost updates --------------------------------------------------------------------------

    static Integer twoReadModifyWrites(Sessions db) {
        try (var a = db.open("A", Isolation.REPEATABLE_READ);
             var b = db.open("B", Isolation.REPEATABLE_READ)) {
            // Both sessions sell one copy: read the stock, subtract 1 in Java, write the result back.
            int readByA = a.queryInt(STOCK_OF_BOOK_1);
            int readByB = b.queryInt(STOCK_OF_BOOK_1);
            a.update("UPDATE inventories SET quantity = " + (readByA - 1) + " WHERE book_id = 1");
            a.commit();
            b.update("UPDATE inventories SET quantity = " + (readByB - 1) + " WHERE book_id = 1");
            b.commit();
            return b.queryInt(STOCK_OF_BOOK_1);
        }
    }

    static Integer twoAtomicUpdates(Sessions db) {
        try (var a = db.open("A", Isolation.REPEATABLE_READ);
             var b = db.open("B", Isolation.REPEATABLE_READ)) {
            b.queryInt(STOCK_OF_BOOK_1);
            a.update("UPDATE inventories SET quantity = quantity - 1 WHERE book_id = 1");
            a.commit();
            b.update("UPDATE inventories SET quantity = quantity - 1 WHERE book_id = 1");
            return b.queryInt(STOCK_OF_BOOK_1);
        }
    }

    static Outcome updateAfterASerializableRead(Sessions db) {
        try (var a = db.open("A", Isolation.READ_COMMITTED);
             var b = db.open("B", Isolation.SERIALIZABLE)) {
            b.queryInt(STOCK_OF_BOOK_1);
            Outcome updateByA = a.attempt("UPDATE inventories SET quantity = 9 WHERE book_id = 1");
            b.commit();
            return updateByA;
        }
    }
}
