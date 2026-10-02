package com.example.bookstore.persistencecontext;

import com.example.bookstore.persistencecontext.Puzzles.Outcome;

/**
 * Your answer sheet for S06-04. Replace each {@code null} with what you predict the matching method in
 * {@link Puzzles} returns, then run {@code PuzzlesTest}.
 *
 * <p>The constant's type tells you what to write: a whole number for an {@code Integer} ({@code 3}), a number with an
 * {@code L} for a {@code Long} ({@code 36_000L}), {@code true} or {@code false} for a {@code Boolean}, and a constant
 * such as {@code Outcome.RETURNS_NORMALLY} for an {@code Outcome}. A compile error here stops every test of the
 * module, so check that the module still compiles after each answer.
 */
final class Answers {

    private Answers() {
    }

    // 1. One object per row

    // One SELECT: the second findById() finds book 1 in the persistence context and doesn't ask the database.
    static final Integer FIND_THE_SAME_BOOK_TWICE = 1;
    // The persistence context holds one object per row, so both calls return the same instance.
    static final Boolean SAME_OBJECT_IN_ONE_TRANSACTION = true;
    // Two: a query (findByIsbn) always runs its SQL; only lookups by id use the persistence context first.
    static final Integer FIND_BY_ID_THEN_BY_ISBN = 2;
    // Each transaction has its own persistence context, and each loads its own copy of the row.
    static final Boolean SAME_OBJECT_IN_TWO_TRANSACTIONS = false;

    // 2. Dirty checking

    // Dirty checking: at commit, Hibernate compares the managed book with its snapshot and writes an UPDATE.
    static final Long PRICE_AFTER_CHANGE_WITHOUT_SAVE = 40_000L;
    // Only the SELECT: the book ends up equal to its snapshot, so there is nothing to update.
    static final Integer CHANGE_AND_CHANGE_BACK = 1;
    // A read-only transaction doesn't flush, and Hibernate keeps no snapshots for it: the change is ignored.
    static final Long PRICE_AFTER_READ_ONLY_TRANSACTION = 36_000L;

    // 3. When the SQL runs

    // With IDENTITY ids, only MySQL knows the next id, so persist() runs the INSERT at once.
    static final Integer SAVE_A_NEW_MEMBER = 1;
    // SELECT, then the UPDATE (Hibernate flushes before a query that reads a table with pending changes), then COUNT.
    static final Integer QUERY_THE_CHANGED_TABLE = 3;
    // SELECT and COUNT: the query reads members, which has no pending changes, so Hibernate doesn't flush yet.
    static final Integer QUERY_ANOTHER_TABLE = 2;

    // 4. Lazy loading and proxies

    // getReferenceById() returns a proxy without a query, and the proxy already knows its id.
    static final Integer REFERENCE_AND_ITS_ID = 0;
    // One SELECT for the order, one for its lines when the lazy list is first used.
    static final Integer READ_THE_LINES_OF_AN_ORDER = 2;
    // The lines were never loaded, and the persistence context that could load them closed with the transaction.
    static final Outcome LINES_AFTER_THE_TRANSACTION = Outcome.THROWS_LAZY_INITIALIZATION_EXCEPTION;
    // The member is a proxy that holds its id (from orders.member_id), so getId() needs no database.
    static final Outcome MEMBER_ID_AFTER_THE_TRANSACTION = Outcome.RETURNS_NORMALLY;

    // 5. Detached entities

    // Nobody tracks a detached entity: the second transaction loads its own copy and changes nothing.
    static final Long PRICE_AFTER_CHANGING_A_DETACHED_BOOK = 36_000L;
    // save() of an entity with an id calls merge(): a SELECT to load the managed copy, then the UPDATE at commit.
    static final Integer SAVE_A_DETACHED_BOOK = 2;

    // 6. Bulk updates

    // The UPDATE bypassed the persistence context; findById() returns the cached book with its old price.
    static final Long PRICE_AFTER_BULK_UPDATE = 36_000L;
    // After clear(), findById() has to run a new SELECT, which sees the new price.
    static final Long PRICE_AFTER_BULK_UPDATE_AND_CLEAR = 37_000L;
    // The entity still holds 10, decrease(1) makes it 9, and its UPDATE overwrites the 15: the delivery is lost.
    static final Integer STOCK_AFTER_RESTOCK_AND_SALE = 9;
}
