package com.example.bookstore.isolation;

import com.example.bookstore.isolation.DatabaseSession.Outcome;

/**
 * Your answer sheet for S07-04. Replace each {@code null} with what you predict the matching method in
 * {@link IsolationPuzzles} returns, then run {@code IsolationPuzzlesTest}.
 *
 * <p>An {@code Integer} is a stock quantity or a number of orders, such as {@code 10}. An {@code Outcome} is
 * {@code Outcome.RUNS} or {@code Outcome.WAITS_FOR_A_LOCK}.
 */
final class IsolationAnswers {

    private IsolationAnswers() {
    }

    // Dirty reads
    static final Integer READ_UNCOMMITTED = null;
    static final Integer READ_COMMITTED = null;

    // Non-repeatable reads
    static final Integer SECOND_READ_UNDER_READ_COMMITTED = null;
    static final Integer SECOND_READ_UNDER_REPEATABLE_READ = null;
    static final Integer FIRST_READ_AFTER_BEGIN = null;

    // Phantoms
    static final Integer SECOND_COUNT_UNDER_READ_COMMITTED = null;
    static final Integer SECOND_COUNT_UNDER_REPEATABLE_READ = null;
    static final Integer LOCKING_COUNT_UNDER_REPEATABLE_READ = null;
    static final Outcome INSERT_INTO_A_LOCKED_RANGE = null;

    // Lost updates
    static final Integer TWO_READ_MODIFY_WRITES = null;
    static final Integer TWO_ATOMIC_UPDATES = null;
    static final Outcome UPDATE_AFTER_A_SERIALIZABLE_READ = null;
}
