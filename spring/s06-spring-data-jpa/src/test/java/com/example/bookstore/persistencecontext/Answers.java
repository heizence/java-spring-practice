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
    static final Integer FIND_THE_SAME_BOOK_TWICE = null;
    static final Boolean SAME_OBJECT_IN_ONE_TRANSACTION = null;
    static final Integer FIND_BY_ID_THEN_BY_ISBN = null;
    static final Boolean SAME_OBJECT_IN_TWO_TRANSACTIONS = null;

    // 2. Dirty checking
    static final Long PRICE_AFTER_CHANGE_WITHOUT_SAVE = null;
    static final Integer CHANGE_AND_CHANGE_BACK = null;
    static final Long PRICE_AFTER_READ_ONLY_TRANSACTION = null;

    // 3. When the SQL runs
    static final Integer SAVE_A_NEW_MEMBER = null;
    static final Integer QUERY_THE_CHANGED_TABLE = null;
    static final Integer QUERY_ANOTHER_TABLE = null;

    // 4. Lazy loading and proxies
    static final Integer REFERENCE_AND_ITS_ID = null;
    static final Integer READ_THE_LINES_OF_AN_ORDER = null;
    static final Outcome LINES_AFTER_THE_TRANSACTION = null;
    static final Outcome MEMBER_ID_AFTER_THE_TRANSACTION = null;

    // 5. Detached entities
    static final Long PRICE_AFTER_CHANGING_A_DETACHED_BOOK = null;
    static final Integer SAVE_A_DETACHED_BOOK = null;

    // 6. Bulk updates
    static final Long PRICE_AFTER_BULK_UPDATE = null;
    static final Long PRICE_AFTER_BULK_UPDATE_AND_CLEAR = null;
    static final Integer STOCK_AFTER_RESTOCK_AND_SALE = null;
}
