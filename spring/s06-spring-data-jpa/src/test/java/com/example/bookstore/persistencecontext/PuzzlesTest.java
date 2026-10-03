package com.example.bookstore.persistencecontext;

import com.example.bookstore.book.BookRepository;
import com.example.bookstore.inventory.InventoryRepository;
import com.example.bookstore.member.MemberRepository;
import com.example.bookstore.order.OrderRepository;
import com.example.bookstore.support.IntegrationTest;
import com.example.bookstore.support.SeedData;
import com.example.bookstore.support.SqlStatementCounter;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import practice.testkit.Predict;

/**
 * Checks your answers in {@link Answers} against what the {@link Puzzles} really do. The puzzles start and commit
 * their own transactions, so this class turns the test transaction off, and puts the seed data back after each test.
 */
@IntegrationTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("S06-04 · Predict: the persistence context")
class PuzzlesTest {

    @Autowired
    private BookRepository books;

    @Autowired
    private MemberRepository members;

    @Autowired
    private OrderRepository orders;

    @Autowired
    private InventoryRepository inventories;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private SqlStatementCounter sqlStatements;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private SeedData seedData;

    private Puzzles puzzles;

    @BeforeEach
    void createPuzzles() {
        puzzles = new Puzzles(books, members, orders, inventories, entityManager, transactionManager, sqlStatements,
                jdbc);
    }

    @AfterEach
    void restoreSeedData() {
        seedData.restore();
    }

    @Nested
    @DisplayName("One object per row")
    class OneObjectPerRow {

        @Test
        @DisplayName("1 · findById(1L) twice in one transaction: how many statements?")
        void findTheSameBookTwice() {
            Predict.check(Answers.FIND_THE_SAME_BOOK_TWICE, puzzles.findTheSameBookTwice());
        }

        @Test
        @DisplayName("2 · findById(1L) twice in one transaction: the same object?")
        void sameObjectInOneTransaction() {
            Predict.check(Answers.SAME_OBJECT_IN_ONE_TRANSACTION, puzzles.sameObjectInOneTransaction());
        }

        @Test
        @DisplayName("3 · findById(1L), then findByIsbn() for the same book: how many statements?")
        void findByIdThenByIsbn() {
            Predict.check(Answers.FIND_BY_ID_THEN_BY_ISBN, puzzles.findByIdThenByIsbn());
        }

        @Test
        @DisplayName("4 · findById(1L) in two transactions: the same object?")
        void sameObjectInTwoTransactions() {
            Predict.check(Answers.SAME_OBJECT_IN_TWO_TRANSACTIONS, puzzles.sameObjectInTwoTransactions());
        }
    }

    @Nested
    @DisplayName("Dirty checking")
    class DirtyChecking {

        @Test
        @DisplayName("5 · change the price without save(): the price in the database?")
        void priceAfterChangeWithoutSave() {
            Predict.check(Answers.PRICE_AFTER_CHANGE_WITHOUT_SAVE, puzzles.priceAfterChangeWithoutSave());
        }

        @Test
        @DisplayName("6 · change the price and change it back: how many statements?")
        void changeAndChangeBack() {
            Predict.check(Answers.CHANGE_AND_CHANGE_BACK, puzzles.changeAndChangeBack());
        }

        @Test
        @DisplayName("7 · change the price in a read-only transaction: the price in the database?")
        void priceAfterReadOnlyTransaction() {
            Predict.check(Answers.PRICE_AFTER_READ_ONLY_TRANSACTION, puzzles.priceAfterReadOnlyTransaction());
        }
    }

    @Nested
    @DisplayName("When the SQL runs")
    class WhenTheSqlRuns {

        @Test
        @DisplayName("8 · save() a new member: how many statements before the commit?")
        void saveANewMember() {
            Predict.check(Answers.SAVE_A_NEW_MEMBER, puzzles.saveANewMember());
        }

        @Test
        @DisplayName("9 · change a book, then count the books: how many statements so far?")
        void queryTheChangedTable() {
            Predict.check(Answers.QUERY_THE_CHANGED_TABLE, puzzles.queryTheChangedTable());
        }

        @Test
        @DisplayName("10 · change a book, then count the members: how many statements so far?")
        void queryAnotherTable() {
            Predict.check(Answers.QUERY_ANOTHER_TABLE, puzzles.queryAnotherTable());
        }
    }

    @Nested
    @DisplayName("Lazy loading and proxies")
    class LazyLoadingAndProxies {

        @Test
        @DisplayName("11 · getReferenceById(1L).getId(): how many statements?")
        void referenceAndItsId() {
            Predict.check(Answers.REFERENCE_AND_ITS_ID, puzzles.referenceAndItsId());
        }

        @Test
        @DisplayName("12 · load an order and read its lines: how many statements?")
        void readTheLinesOfAnOrder() {
            Predict.check(Answers.READ_THE_LINES_OF_AN_ORDER, puzzles.readTheLinesOfAnOrder());
        }

        @Test
        @DisplayName("13 · read an order's lines after the transaction: what happens?")
        void linesAfterTheTransaction() {
            Predict.check(Answers.LINES_AFTER_THE_TRANSACTION, puzzles.linesAfterTheTransaction());
        }

        @Test
        @DisplayName("14 · read the id of an order's member after the transaction: what happens?")
        void memberIdAfterTheTransaction() {
            Predict.check(Answers.MEMBER_ID_AFTER_THE_TRANSACTION, puzzles.memberIdAfterTheTransaction());
        }
    }

    @Nested
    @DisplayName("Detached entities")
    class DetachedEntities {

        @Test
        @DisplayName("15 · change a book after its transaction ended: the price in the database?")
        void priceAfterChangingADetachedBook() {
            Predict.check(Answers.PRICE_AFTER_CHANGING_A_DETACHED_BOOK, puzzles.priceAfterChangingADetachedBook());
        }

        @Test
        @DisplayName("16 · save() a changed detached book in a new transaction: how many statements?")
        void saveADetachedBook() {
            Predict.check(Answers.SAVE_A_DETACHED_BOOK, puzzles.saveADetachedBook());
        }
    }

    @Nested
    @DisplayName("Bulk updates")
    class BulkUpdates {

        @Test
        @DisplayName("17 · a bulk UPDATE raises the price, then findById(1L): which price?")
        void priceAfterBulkUpdate() {
            Predict.check(Answers.PRICE_AFTER_BULK_UPDATE, puzzles.priceAfterBulkUpdate());
        }

        @Test
        @DisplayName("18 · the same, with clear() before findById(1L): which price?")
        void priceAfterBulkUpdateAndClear() {
            Predict.check(Answers.PRICE_AFTER_BULK_UPDATE_AND_CLEAR, puzzles.priceAfterBulkUpdateAndClear());
        }

        @Test
        @DisplayName("19 · a bulk UPDATE adds 5 copies, then the loaded inventory sells 1: the stock in the database?")
        void stockAfterRestockAndSale() {
            Predict.check(Answers.STOCK_AFTER_RESTOCK_AND_SALE, puzzles.stockAfterRestockAndSale());
        }
    }
}
