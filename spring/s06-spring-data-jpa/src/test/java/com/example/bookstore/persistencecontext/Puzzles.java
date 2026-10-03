package com.example.bookstore.persistencecontext;

import java.util.function.Supplier;

import com.example.bookstore.book.Book;
import com.example.bookstore.book.BookRepository;
import com.example.bookstore.inventory.Inventory;
import com.example.bookstore.inventory.InventoryRepository;
import com.example.bookstore.member.Member;
import com.example.bookstore.member.MemberRepository;
import com.example.bookstore.order.Order;
import com.example.bookstore.order.OrderRepository;
import com.example.bookstore.support.SqlStatementCounter;
import jakarta.persistence.EntityManager;
import org.hibernate.LazyInitializationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * S06-04 · Predict: the persistence context.
 *
 * <p>Each method is a small scenario that runs against the real database and returns one thing it observed: a number
 * of SQL statements, a value read back from the database, whether two variables point to the same object, or how
 * the scenario ended. <strong>Don't run anything yet.</strong> Read each puzzle, decide what it returns, and write your
 * prediction in {@link Answers}. Then run {@code PuzzlesTest}: a test fails when a prediction was wrong, and the
 * module README explains every answer.
 *
 * <h2>How to read the puzzles</h2>
 * <ul>
 *   <li>{@code inTransaction(() -> ...)} runs the code in a new transaction and commits it at the end, like a
 *       {@code @Transactional} service method that returns normally. Each call is a separate transaction with its
 *       own persistence context. Code outside {@code inTransaction} runs without a transaction.</li>
 *   <li>{@code countStatements(() -> ...)} runs the code and returns how many SQL statements Hibernate sent to the
 *       database meanwhile, including the statements of a commit. {@code statementsSoFar()} is the number since the
 *       puzzle started.</li>
 *   <li>{@code priceInDatabase(bookId)} and {@code stockInDatabase(bookId)} read a value with plain SQL, past
 *       Hibernate: what the database holds at that moment. These reads aren't counted as statements.</li>
 *   <li>{@code books}, {@code members}, {@code orders} and {@code inventories} are the application's Spring Data
 *       repositories; {@code entityManager} is the JPA {@code EntityManager} they use.</li>
 * </ul>
 *
 * <p>The seed data: book 1 is <i>Effective Java</i>, 36,000 won, with 10 copies in stock. Order 1 is one of the 200
 * seeded orders and has at least one line. Before each puzzle, the database holds exactly the seed data again.
 */
final class Puzzles {

    private final BookRepository books;
    private final MemberRepository members;
    private final OrderRepository orders;
    private final InventoryRepository inventories;
    private final EntityManager entityManager;
    private final TransactionTemplate transaction;
    private final TransactionTemplate readOnlyTransaction;
    private final SqlStatementCounter sqlStatements;
    private final JdbcClient jdbc;

    Puzzles(BookRepository books, MemberRepository members, OrderRepository orders, InventoryRepository inventories,
            EntityManager entityManager, PlatformTransactionManager transactionManager,
            SqlStatementCounter sqlStatements, JdbcClient jdbc) {
        this.books = books;
        this.members = members;
        this.orders = orders;
        this.inventories = inventories;
        this.entityManager = entityManager;
        this.transaction = new TransactionTemplate(transactionManager);
        this.readOnlyTransaction = new TransactionTemplate(transactionManager);
        this.readOnlyTransaction.setReadOnly(true);
        this.sqlStatements = sqlStatements;
        this.jdbc = jdbc;
    }

    // ---- 1. One object per row ---------------------------------------------------------------------

    /** How many SQL statements does this transaction run? */
    int findTheSameBookTwice() {
        return countStatements(() -> inTransaction(() -> {
            books.findById(1L);
            books.findById(1L);
        }));
    }

    /** Do {@code first} and {@code second} point to the same Java object? */
    boolean sameObjectInOneTransaction() {
        return inTransaction(() -> {
            Book first = books.findById(1L).orElseThrow();
            Book second = books.findById(1L).orElseThrow();
            return first == second;
        });
    }

    /** How many SQL statements does this transaction run? */
    int findByIdThenByIsbn() {
        return countStatements(() -> inTransaction(() -> {
            Book byId = books.findById(1L).orElseThrow();
            books.findByIsbn(byId.getIsbn());
        }));
    }

    /** Do {@code first} and {@code second} point to the same Java object? */
    boolean sameObjectInTwoTransactions() {
        Book first = inTransaction(() -> books.findById(1L).orElseThrow());
        Book second = inTransaction(() -> books.findById(1L).orElseThrow());
        return first == second;
    }

    // ---- 2. Dirty checking ---------------------------------------------------------------------------

    /** What is book 1's price in the database after the transaction? */
    long priceAfterChangeWithoutSave() {
        inTransaction(() -> {
            Book book = books.findById(1L).orElseThrow();
            book.changePrice(40_000);
            // no save() call
        });
        return priceInDatabase(1L);
    }

    /** How many SQL statements does this transaction run? */
    int changeAndChangeBack() {
        return countStatements(() -> inTransaction(() -> {
            Book book = books.findById(1L).orElseThrow();
            book.changePrice(40_000);
            book.changePrice(36_000);
        }));
    }

    /** What is book 1's price in the database after this read-only transaction? */
    long priceAfterReadOnlyTransaction() {
        inReadOnlyTransaction(() -> {
            Book book = books.findById(1L).orElseThrow();
            book.changePrice(40_000);
        });
        return priceInDatabase(1L);
    }

    // ---- 3. When the SQL runs ------------------------------------------------------------------------

    /** How many SQL statements have run when save() returns, before the transaction commits? */
    int saveANewMember() {
        return inTransaction(() -> {
            members.save(Member.register("new.member@example.com", "New Member", "{noop}secret"));
            return statementsSoFar();
        });
    }

    /** How many SQL statements have run when the count query on books returns? */
    int queryTheChangedTable() {
        return inTransaction(() -> {
            Book book = books.findById(1L).orElseThrow();
            book.changePrice(40_000);
            books.count();
            return statementsSoFar();
        });
    }

    /** How many SQL statements have run when the count query on members returns? */
    int queryAnotherTable() {
        return inTransaction(() -> {
            Book book = books.findById(1L).orElseThrow();
            book.changePrice(40_000);
            members.count();
            return statementsSoFar();
        });
    }

    // ---- 4. Lazy loading and proxies -----------------------------------------------------------------

    /** How many SQL statements have run after getId()? */
    int referenceAndItsId() {
        return inTransaction(() -> {
            Book book = books.getReferenceById(1L);
            book.getId();
            return statementsSoFar();
        });
    }

    /** How many SQL statements does this transaction run? */
    int readTheLinesOfAnOrder() {
        return countStatements(() -> inTransaction(() -> {
            Order order = orders.findById(1L).orElseThrow();
            order.getLines().size();
        }));
    }

    /** How does reading the lines after the transaction end? */
    Outcome linesAfterTheTransaction() {
        Order order = inTransaction(() -> orders.findById(1L).orElseThrow());
        return outcomeOf(() -> order.getLines().size());
    }

    /** How does reading the member's id after the transaction end? */
    Outcome memberIdAfterTheTransaction() {
        Order order = inTransaction(() -> orders.findById(1L).orElseThrow());
        return outcomeOf(() -> order.getMember().getId());
    }

    // ---- 5. Detached entities ------------------------------------------------------------------------

    /** What is book 1's price in the database at the end? */
    long priceAfterChangingADetachedBook() {
        Book book = inTransaction(() -> books.findById(1L).orElseThrow());
        book.changePrice(40_000); // the transaction has ended
        inTransaction(() -> books.findById(1L).orElseThrow());
        return priceInDatabase(1L);
    }

    /** How many SQL statements does the second transaction run? */
    int saveADetachedBook() {
        Book book = inTransaction(() -> books.findById(1L).orElseThrow());
        book.changePrice(40_000); // the transaction has ended
        return countStatements(() -> inTransaction(() -> books.save(book)));
    }

    // ---- 6. Bulk updates -----------------------------------------------------------------------------

    /** What price does the second findById() return? */
    long priceAfterBulkUpdate() {
        return inTransaction(() -> {
            books.findById(1L).orElseThrow();
            entityManager.createQuery("update Book b set b.price = b.price + 1000 where b.id = 1").executeUpdate();
            return books.findById(1L).orElseThrow().getPrice();
        });
    }

    /** What price does findById() return after clear()? */
    long priceAfterBulkUpdateAndClear() {
        return inTransaction(() -> {
            books.findById(1L).orElseThrow();
            entityManager.createQuery("update Book b set b.price = b.price + 1000 where b.id = 1").executeUpdate();
            entityManager.clear();
            return books.findById(1L).orElseThrow().getPrice();
        });
    }

    /** How many copies of book 1 are in stock in the database at the end? */
    int stockAfterRestockAndSale() {
        inTransaction(() -> {
            Inventory stock = inventories.findById(1L).orElseThrow();
            // A delivery of 5 copies arrives, booked with a bulk update...
            entityManager.createQuery("update Inventory i set i.quantity = i.quantity + 5 where i.bookId = 1")
                    .executeUpdate();
            // ...and a customer buys one copy.
            stock.decrease(1);
        });
        return stockInDatabase(1L);
    }

    // ---- The tools -----------------------------------------------------------------------------------

    /** How a puzzle ends: normally, or with an exception. */
    enum Outcome {
        RETURNS_NORMALLY,
        THROWS_LAZY_INITIALIZATION_EXCEPTION,
        THROWS_NULL_POINTER_EXCEPTION
    }

    private void inTransaction(Runnable code) {
        transaction.executeWithoutResult(status -> code.run());
    }

    private <T> T inTransaction(Supplier<T> code) {
        return transaction.execute(status -> code.get());
    }

    private void inReadOnlyTransaction(Runnable code) {
        readOnlyTransaction.executeWithoutResult(status -> code.run());
    }

    private int countStatements(Runnable code) {
        sqlStatements.reset();
        code.run();
        return sqlStatements.count();
    }

    private int statementsSoFar() {
        return sqlStatements.count();
    }

    private long priceInDatabase(long bookId) {
        return jdbc.sql("SELECT price FROM books WHERE id = ?").param(bookId).query(Long.class).single();
    }

    private int stockInDatabase(long bookId) {
        return jdbc.sql("SELECT quantity FROM inventories WHERE book_id = ?").param(bookId).query(Integer.class)
                .single();
    }

    private static Outcome outcomeOf(Runnable code) {
        try {
            code.run();
            return Outcome.RETURNS_NORMALLY;
        } catch (LazyInitializationException e) {
            return Outcome.THROWS_LAZY_INITIALIZATION_EXCEPTION;
        } catch (NullPointerException e) {
            return Outcome.THROWS_NULL_POINTER_EXCEPTION;
        }
    }
}
