package com.example.bookstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.Instant;
import java.time.LocalDate;

import com.example.bookstore.book.Book;
import com.example.bookstore.book.BookRepository;
import com.example.bookstore.book.CategoryRepository;
import com.example.bookstore.coupon.Coupon;
import com.example.bookstore.coupon.CouponIssue;
import com.example.bookstore.coupon.CouponIssueRepository;
import com.example.bookstore.coupon.CouponRepository;
import com.example.bookstore.inventory.Inventory;
import com.example.bookstore.inventory.InventoryRepository;
import com.example.bookstore.member.Member;
import com.example.bookstore.member.MemberRepository;
import com.example.bookstore.member.Role;
import com.example.bookstore.order.Order;
import com.example.bookstore.order.OrderLineRepository;
import com.example.bookstore.order.OrderRepository;
import com.example.bookstore.order.OrderStatus;
import com.example.bookstore.payment.Payment;
import com.example.bookstore.payment.PaymentRepository;
import com.example.bookstore.payment.PaymentStatus;
import com.example.bookstore.review.ReviewRepository;
import com.example.bookstore.support.IntegrationTest;
import com.example.bookstore.support.TestClock;
import com.example.bookstore.wallet.Wallet;
import com.example.bookstore.wallet.WalletRepository;
import com.example.bookstore.wallet.WalletTransactionRepository;
import com.example.bookstore.wallet.WalletTransactionType;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * The entities match the schema (the context only starts if Hibernate's validation passes), read the seed rows with
 * the right values, and write back what their business methods change.
 */
@IntegrationTest
@DisplayName("Template · Entity mappings")
class EntityMappingTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private TestClock clock;

    @Autowired
    private BookRepository books;

    @Autowired
    private CategoryRepository categories;

    @Autowired
    private InventoryRepository inventories;

    @Autowired
    private MemberRepository members;

    @Autowired
    private WalletRepository wallets;

    @Autowired
    private WalletTransactionRepository walletTransactions;

    @Autowired
    private CouponRepository coupons;

    @Autowired
    private CouponIssueRepository couponIssues;

    @Autowired
    private OrderRepository orders;

    @Autowired
    private OrderLineRepository orderLines;

    @Autowired
    private PaymentRepository payments;

    @Autowired
    private ReviewRepository reviews;

    @Nested
    @DisplayName("Reading the seed data")
    class Reading {

        @Test
        @DisplayName("every repository finds its first seed row")
        void everyEntityLoads() {
            assertThat(categories.findById(1L)).isPresent();
            assertThat(books.findById(1L)).isPresent();
            assertThat(inventories.findById(1L)).isPresent();
            assertThat(members.findById(1L)).isPresent();
            assertThat(wallets.findById(1L)).isPresent();
            assertThat(walletTransactions.findById(1L)).isPresent();
            assertThat(coupons.findById(1L)).isPresent();
            assertThat(couponIssues.findById(1L)).isPresent();
            assertThat(orders.findById(1L)).isPresent();
            assertThat(orderLines.findById(1L)).isPresent();
            assertThat(payments.findById(1L)).isPresent();
            assertThat(reviews.findById(1L)).isPresent();
        }

        @Test
        @DisplayName("book 1 with its category, price, dates and stock")
        void book() {
            Book book = books.findById(1L).orElseThrow();
            Inventory inventory = inventories.findById(1L).orElseThrow();

            assertThat(book.getIsbn()).isEqualTo("9791115881394");
            assertThat(book.getTitle()).isEqualTo("Effective Java");
            assertThat(book.getAuthor()).isEqualTo("Joshua Bloch");
            assertThat(book.getCategory().getName()).isEqualTo("Programming");
            assertThat(book.getPrice()).isEqualTo(36_000);
            assertThat(book.getPublishedDate()).isEqualTo(LocalDate.of(2018, 10, 18));
            assertThat(book.getCreatedAt()).isEqualTo(Instant.parse("2026-07-01T00:00:00Z"));
            assertThat(inventory.getQuantity()).isEqualTo(10);
            assertThat(inventory.getVersion()).isZero();
            assertThat(inventory.getBook()).isSameAs(book);
        }

        @Test
        @DisplayName("a title in Korean")
        void koreanTitle() {
            assertThat(books.findById(41L).orElseThrow().getTitle()).isEqualTo("진달래꽃");
        }

        @Test
        @DisplayName("member 1 with her wallet, and the admin")
        void members() {
            Member minji = members.findByEmail("minji@example.com").orElseThrow();
            Wallet wallet = wallets.findByMemberId(minji.getId()).orElseThrow();

            assertThat(minji.getId()).isEqualTo(1L);
            assertThat(minji.getName()).isEqualTo("Minji Kim");
            assertThat(minji.getRole()).isEqualTo(Role.USER);
            assertThat(minji.getPasswordHash()).startsWith("{bcrypt}");
            assertThat(minji.getCreatedAt()).isEqualTo(Instant.parse("2026-07-02T00:00:00Z"));
            assertThat(wallet.getBalance()).isEqualTo(100_000);
            assertThat(wallet.getVersion()).isZero();
            assertThat(walletTransactions.findById(1L).orElseThrow())
                    .returns(WalletTransactionType.CHARGE, tx -> tx.getType())
                    .returns(wallet, tx -> tx.getWallet());
            assertThat(members.findById(20L).orElseThrow().getRole()).isEqualTo(Role.ADMIN);
        }

        @Test
        @DisplayName("order 1 with its three lines and its payment")
        void orderWithLines() {
            Order order = orders.findById(1L).orElseThrow();
            Payment payment = payments.findByIdempotencyKey("16e134138e4e45ae62c63adf69db8679").orElseThrow();

            assertThat(order.getMember().getId()).isEqualTo(13L);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
            assertThat(order.getTotalAmount()).isEqualTo(118_000);
            assertThat(order.getOrderedAt()).isEqualTo(Instant.parse("2026-08-01T12:36:00Z"));
            assertThat(order.getLines())
                    .extracting(line -> line.getBook().getId(), line -> line.getQuantity(), line -> line.getUnitPrice())
                    .containsExactly(tuple(26L, 1, 26_000L), tuple(30L, 1, 49_000L), tuple(6L, 1, 43_000L));
            assertThat(payment.getOrder()).isSameAs(order);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
            assertThat(payment.getAmount()).isEqualTo(118_000);
            assertThat(payment.getCreatedAt()).isEqualTo(Instant.parse("2026-08-01T12:41:00Z"));
        }

        @Test
        @DisplayName("a coupon with its validity period, and a used coupon issue")
        void coupons() {
            Coupon flash = coupons.findByCode("FLASH10000").orElseThrow();
            CouponIssue minjisAutumnCoupon = couponIssues.findByCouponIdAndMemberId(3L, 1L).orElseThrow();

            assertThat(flash.getDiscountAmount()).isEqualTo(10_000);
            assertThat(flash.getTotalQuantity()).isEqualTo(10);
            assertThat(flash.getIssuedQuantity()).isZero();
            assertThat(flash.getValidFrom()).isEqualTo(Instant.parse("2026-10-01T00:00:00Z"));
            assertThat(flash.getValidUntil()).isEqualTo(Instant.parse("2026-10-31T23:59:59Z"));
            assertThat(flash.isValidAt(clock.instant())).isTrue();
            assertThat(minjisAutumnCoupon.getIssuedAt()).isEqualTo(Instant.parse("2026-09-02T03:00:00Z"));
            assertThat(minjisAutumnCoupon.getUsedAt()).contains(Instant.parse("2026-09-13T07:47:00Z"));
        }

        @Test
        @DisplayName("a review's rating from its TINYINT column")
        void review() {
            var review = reviews.findById(1L).orElseThrow();

            assertThat(review.getBook().getId()).isEqualTo(1L);
            assertThat(review.getRating()).isEqualTo(3);
            assertThat(review.getContent()).isEqualTo("Not what I expected, still enjoyed it.");
        }
    }

    @Nested
    @DisplayName("Writing changes")
    class Writing {

        @Test
        @DisplayName("saving an order saves its lines too")
        void cascadesToLines() {
            Order order = Order.place(members.getReferenceById(1L), clock.instant());
            order.addLine(books.findById(1L).orElseThrow(), 2);
            order.addLine(books.findById(2L).orElseThrow(), 1);

            Long id = orders.saveAndFlush(order).getId();
            entityManager.clear();

            Order reloaded = orders.findById(id).orElseThrow();
            assertThat(reloaded.getTotalAmount()).isEqualTo(2 * 36_000 + 12_000);
            assertThat(reloaded.getLines()).extracting(line -> line.getBook().getId()).containsExactly(1L, 2L);
        }

        @Test
        @DisplayName("removing a line from an order deletes its row")
        void deletesOrphanLines() {
            Order minjisPendingOrder = orders.findById(33L).orElseThrow();

            minjisPendingOrder.removeLine(books.getReferenceById(22L));
            entityManager.flush();

            assertThat(jdbc.sql("SELECT book_id FROM order_lines WHERE order_id = 33").query(Long.class).list())
                    .containsExactly(39L);
            assertThat(jdbc.sql("SELECT total_amount FROM orders WHERE id = 33").query(Long.class).single())
                    .isEqualTo(13_000);
        }

        @Test
        @DisplayName("each update of a versioned entity increases its version")
        void incrementsVersion() {
            Inventory inventory = inventories.findById(1L).orElseThrow();

            inventory.decrease(3);
            entityManager.flush();

            assertThat(jdbc.sql("SELECT quantity, version FROM inventories WHERE book_id = 1").query().singleRow())
                    .containsEntry("quantity", 7)
                    .containsEntry("version", 1L);
        }

        @Test
        @DisplayName("a withdrawal saves the new balance and, once saved, its history entry")
        void savesWalletHistory() {
            Wallet wallet = wallets.findByMemberId(1L).orElseThrow();

            walletTransactions.save(wallet.withdraw(36_000, WalletTransactionType.PAYMENT));
            entityManager.flush();

            assertThat(jdbc.sql("SELECT balance, version FROM wallets WHERE id = 1").query().singleRow())
                    .containsEntry("balance", 64_000L)
                    .containsEntry("version", 1L);
            assertThat(jdbc.sql("""
                    SELECT type, amount, created_at FROM wallet_transactions WHERE wallet_id = 1
                    ORDER BY id DESC LIMIT 1""")
                    .query((row, rowNumber) -> row.getString("type") + " " + row.getLong("amount") + " at "
                            + row.getTimestamp("created_at").toInstant()).single())
                    .isEqualTo("PAYMENT 36000 at " + TestClock.DEFAULT_INSTANT);
        }

        @Test
        @DisplayName("issuing a coupon saves the issue and the coupon's new count")
        void savesCouponIssues() {
            Coupon flash = coupons.findByCode("FLASH10000").orElseThrow();

            couponIssues.save(flash.issue(members.getReferenceById(1L), clock.instant()));
            entityManager.flush();

            assertThat(jdbc.sql("SELECT issued_quantity FROM coupons WHERE code = 'FLASH10000'")
                    .query(Integer.class).single()).isEqualTo(1);
            assertThat(couponIssues.existsByCouponIdAndMemberId(flash.getId(), 1L)).isTrue();
        }

        @Test
        @DisplayName("a payment is saved with its creation time")
        void savesPayments() {
            Order minjisPendingOrder = orders.findById(33L).orElseThrow();

            Payment payment = payments.saveAndFlush(Payment.approved(minjisPendingOrder, "new-key", 28_000));

            assertThat(payment.getId()).isNotNull();
            assertThat(payment.getCreatedAt()).isEqualTo(TestClock.DEFAULT_INSTANT);
            assertThat(payments.findByIdempotencyKey("new-key")).containsSame(payment);
        }

        @Test
        @DisplayName("a business method works on an entity that Hibernate loaded lazily")
        void businessMethodOnLazyProxy() {
            CouponIssue unused = couponIssues.findByCouponIdAndMemberId(1L, 4L).orElseThrow();
            assertThat(Hibernate.isInitialized(unused.getCoupon())).isFalse();

            unused.use(clock.instant());
            entityManager.flush();

            assertThat(jdbc.sql("SELECT used_at FROM coupon_issues WHERE coupon_id = 1 AND member_id = 4")
                    .query((row, rowNumber) -> row.getTimestamp("used_at").toInstant()).single())
                    .isEqualTo(TestClock.DEFAULT_INSTANT);
        }

        @Test
        @DisplayName("times are stored in UTC, whatever the JVM's time zone is")
        void storesTimesInUtc() {
            clock.setInstant(Instant.parse("2026-10-15T09:30:00.123456Z"));
            Member member = members.saveAndFlush(Member.register("new@example.com", "New Member", "{noop}secret"));

            String stored = jdbc.sql("SELECT DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s.%f') FROM members WHERE id = ?")
                    .param(member.getId()).query(String.class).single();

            assertThat(stored).isEqualTo("2026-10-15 09:30:00.123456");
        }
    }
}
