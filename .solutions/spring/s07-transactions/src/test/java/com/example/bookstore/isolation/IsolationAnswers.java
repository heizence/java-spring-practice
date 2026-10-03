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

    // READ UNCOMMITTED reads the newest version of the row, committed or not: a dirty read of a value that A then
    // rolls back, so it never existed.
    static final Integer READ_UNCOMMITTED = 3;
    // READ COMMITTED reads the last committed version, without waiting for A's lock: InnoDB keeps old versions of a
    // changed row (in the undo log) for exactly this.
    static final Integer READ_COMMITTED = 10;

    // Non-repeatable reads

    // READ COMMITTED takes a fresh snapshot for every statement, so the second read sees A's commit.
    static final Integer SECOND_READ_UNDER_READ_COMMITTED = 7;
    // REPEATABLE READ (MySQL's default) keeps the snapshot of the transaction's first read until the transaction ends.
    static final Integer SECOND_READ_UNDER_REPEATABLE_READ = 10;
    // The snapshot is taken at the first read, not at START TRANSACTION, and A committed before B's first read.
    static final Integer FIRST_READ_AFTER_BEGIN = 7;

    // Phantoms

    // A fresh snapshot per statement: the new order appears (a phantom row).
    static final Integer SECOND_COUNT_UNDER_READ_COMMITTED = 15;
    // B still reads its snapshot, so InnoDB's REPEATABLE READ shows no phantom to plain SELECTs.
    static final Integer SECOND_COUNT_UNDER_REPEATABLE_READ = 14;
    // A locking read (FOR SHARE, FOR UPDATE, and UPDATE or DELETE) reads the latest committed rows, not the snapshot.
    static final Integer LOCKING_COUNT_UNDER_REPEATABLE_READ = 15;
    // FOR UPDATE locked Minji's index entries and the gaps around them (next-key locks), so no new order for her can
    // be inserted until B ends its transaction.
    static final Outcome INSERT_INTO_A_LOCKED_RANGE = Outcome.WAITS_FOR_A_LOCK;

    // Lost updates

    // Both computed 10 - 1 from their snapshots. B's UPDATE overwrote A's 9 with another 9: one sale was lost.
    // MySQL's REPEATABLE READ doesn't detect this.
    static final Integer TWO_READ_MODIFY_WRITES = 9;
    // An UPDATE reads the latest committed row (9), not B's snapshot (10), and B then sees its own change: 8.
    static final Integer TWO_ATOMIC_UPDATES = 8;
    // With auto-commit off, SERIALIZABLE turns every plain SELECT into SELECT ... FOR SHARE, and A's update needs an
    // exclusive lock on the row B has share-locked.
    static final Outcome UPDATE_AFTER_A_SERIALIZABLE_READ = Outcome.WAITS_FOR_A_LOCK;
}
