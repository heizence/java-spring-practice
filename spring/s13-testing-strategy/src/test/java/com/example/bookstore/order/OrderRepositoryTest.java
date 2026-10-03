package com.example.bookstore.order;

import static com.example.bookstore.order.OrderBuilder.anOrder;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.example.bookstore.book.Book;
import com.example.bookstore.common.ClockConfiguration;
import com.example.bookstore.common.JpaAuditingConfiguration;
import com.example.bookstore.member.Member;
import com.example.bookstore.support.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * Reference tests for S13-02.
 *
 * <p>Three habits make these tests catch bugs:
 * <ul>
 *   <li>Each test creates its own member and that member's orders, so the seed data can't change the result, and
 *       the assertions can be exact. The cutoff for pending orders lies in June 2026, before every seeded order.</li>
 *   <li>The test data can tell right from wrong: orders saved in a different order than the expected result, a page
 *       smaller than the number of orders (only then does Spring Data run the count query), and an order exactly on
 *       each boundary.</li>
 *   <li>Every rule in the Javadoc gets a test, including the empty case: a member without orders.</li>
 * </ul>
 */
@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaAuditingConfiguration.class, ClockConfiguration.class})
@DisplayName("S13-02 · Write tests: a repository slice")
class OrderRepositoryTest {

    private static final Instant CUTOFF = Instant.parse("2026-06-15T00:00:00Z");

    @Autowired
    private OrderRepository orders;

    @Autowired
    private TestEntityManager entityManager;

    private Member buyer;

    /** Book 1, Effective Java, 36,000 won (spring/BOOKSTORE.md). */
    private Book effectiveJava;

    @BeforeEach
    void createABuyer() {
        buyer = newMember("buyer@example.org");
        effectiveJava = entityManager.find(Book.class, 1L);
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("findMemberOrders() returns the member's orders newest first, whatever order they were saved in")
        void newestFirst() {
            Order middle = save(anOrder(buyer, effectiveJava).placedAt("2026-06-02T00:00:00Z"));
            Order newest = save(anOrder(buyer, effectiveJava).placedAt("2026-06-03T00:00:00Z"));
            Order oldest = save(anOrder(buyer, effectiveJava).placedAt("2026-06-01T00:00:00Z"));

            Page<Order> page = orders.findMemberOrders(buyer.getId(), null, PageRequest.of(0, 10));

            assertThat(idsOf(page.getContent())).containsExactly(newest.getId(), middle.getId(), oldest.getId());
        }

        @Test
        @DisplayName("findMemberOrders() pages through the orders, and every page counts all of them")
        void pagesAndCounts() {
            List<Order> newestFirst = saveOrdersOnFiveDays(buyer);

            Page<Order> first = orders.findMemberOrders(buyer.getId(), null, PageRequest.of(0, 2));
            Page<Order> last = orders.findMemberOrders(buyer.getId(), null, PageRequest.of(2, 2));

            assertThat(idsOf(first.getContent())).containsExactlyElementsOf(idsOf(newestFirst.subList(0, 2)));
            assertThat(idsOf(last.getContent())).containsExactly(newestFirst.get(4).getId());
            assertThat(first.getTotalElements()).isEqualTo(5);
            assertThat(first.getTotalPages()).isEqualTo(3);
            assertThat(last.getTotalElements()).isEqualTo(5);
        }

        @Test
        @DisplayName("totalSpentBy() adds up the totals of the member's PAID and SHIPPED orders")
        void addsUpPaidAndShippedOrders() {
            save(anOrder(buyer, effectiveJava).copies(2).status(OrderStatus.PAID));
            save(anOrder(buyer, effectiveJava).status(OrderStatus.SHIPPED));

            assertThat(orders.totalSpentBy(buyer.getId())).isEqualTo(2 * 36_000 + 36_000);
        }

        @Test
        @DisplayName("findPendingOrderedBefore() returns the pending orders placed before the cutoff, oldest first")
        void findsPendingOrdersBeforeTheCutoff() {
            Order later = save(anOrder(buyer, effectiveJava).placedAt("2026-06-10T00:00:00Z"));
            Order earlier = save(anOrder(buyer, effectiveJava).placedAt("2026-06-05T00:00:00Z"));
            save(anOrder(buyer, effectiveJava).placedAt("2026-06-20T00:00:00Z"));

            assertThat(idsOf(orders.findPendingOrderedBefore(CUTOFF))).containsExactly(earlier.getId(), later.getId());
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("findMemberOrders() puts the higher id first when two orders were placed at the same moment")
        void sameMomentHighestIdFirst() {
            Order first = save(anOrder(buyer, effectiveJava).placedAt("2026-06-02T00:00:00Z"));
            Order second = save(anOrder(buyer, effectiveJava).placedAt("2026-06-02T00:00:00Z"));

            Page<Order> page = orders.findMemberOrders(buyer.getId(), null, PageRequest.of(0, 10));

            assertThat(idsOf(page.getContent())).containsExactly(second.getId(), first.getId());
        }

        @Test
        @DisplayName("findMemberOrders() leaves out other members' orders, in the content and in the total")
        void onlyTheMembersOrders() {
            Member other = newMember("other@example.org");
            Order mine = save(anOrder(buyer, effectiveJava));
            save(anOrder(other, effectiveJava));
            save(anOrder(other, effectiveJava));

            Page<Order> page = orders.findMemberOrders(buyer.getId(), null, PageRequest.of(0, 1));

            assertThat(idsOf(page.getContent())).containsExactly(mine.getId());
            assertThat(page.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("findMemberOrders() with a status returns and counts only the orders in that status")
        void filtersByStatus() {
            save(anOrder(buyer, effectiveJava).placedAt("2026-06-01T00:00:00Z").status(OrderStatus.PAID));
            Order paidMiddle = save(anOrder(buyer, effectiveJava).placedAt("2026-06-02T00:00:00Z")
                    .status(OrderStatus.PAID));
            Order paidNewest = save(anOrder(buyer, effectiveJava).placedAt("2026-06-03T00:00:00Z")
                    .status(OrderStatus.PAID));
            save(anOrder(buyer, effectiveJava).placedAt("2026-06-04T00:00:00Z")); // newer, but PENDING

            Page<Order> page = orders.findMemberOrders(buyer.getId(), OrderStatus.PAID, PageRequest.of(0, 2));

            assertThat(idsOf(page.getContent())).containsExactly(paidNewest.getId(), paidMiddle.getId());
            assertThat(page.getTotalElements()).isEqualTo(3);
        }

        @Test
        @DisplayName("findMemberOrders() returns an empty page for a member without orders")
        void memberWithoutOrders() {
            Page<Order> page = orders.findMemberOrders(buyer.getId(), null, PageRequest.of(0, 10));

            assertThat(page.getContent()).isEmpty();
            assertThat(page.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("totalSpentBy() doesn't count PENDING or CANCELLED orders")
        void ignoresUnpaidOrders() {
            save(anOrder(buyer, effectiveJava).status(OrderStatus.PAID));
            save(anOrder(buyer, effectiveJava).status(OrderStatus.PENDING));
            save(anOrder(buyer, effectiveJava).status(OrderStatus.CANCELLED));

            assertThat(orders.totalSpentBy(buyer.getId())).isEqualTo(36_000);
        }

        @Test
        @DisplayName("totalSpentBy() doesn't count other members' orders")
        void ignoresOtherMembers() {
            Member other = newMember("other@example.org");
            save(anOrder(buyer, effectiveJava).status(OrderStatus.PAID));
            save(anOrder(other, effectiveJava).copies(3).status(OrderStatus.PAID));

            assertThat(orders.totalSpentBy(buyer.getId())).isEqualTo(36_000);
        }

        @Test
        @DisplayName("totalSpentBy() is 0 for a member without paid or shipped orders")
        void zeroWithoutPaidOrders() {
            Member newcomer = newMember("newcomer@example.org");
            save(anOrder(buyer, effectiveJava).status(OrderStatus.PENDING));

            assertThat(orders.totalSpentBy(buyer.getId())).isZero();
            assertThat(orders.totalSpentBy(newcomer.getId())).isZero();
        }

        @Test
        @DisplayName("findPendingOrderedBefore() leaves out an order placed exactly at the cutoff")
        void excludesTheCutoff() {
            save(anOrder(buyer, effectiveJava).placedAt(CUTOFF));

            assertThat(orders.findPendingOrderedBefore(CUTOFF)).isEmpty();
        }

        @Test
        @DisplayName("findPendingOrderedBefore() includes an order placed one microsecond before the cutoff")
        void includesJustBeforeTheCutoff() {
            Order justBefore = save(anOrder(buyer, effectiveJava).placedAt(CUTOFF.minusNanos(1_000)));

            assertThat(idsOf(orders.findPendingOrderedBefore(CUTOFF))).containsExactly(justBefore.getId());
        }

        @Test
        @DisplayName("findPendingOrderedBefore() leaves out PAID, SHIPPED and CANCELLED orders")
        void onlyPendingOrders() {
            save(anOrder(buyer, effectiveJava).placedAt("2026-06-01T00:00:00Z").status(OrderStatus.PAID));
            save(anOrder(buyer, effectiveJava).placedAt("2026-06-02T00:00:00Z").status(OrderStatus.SHIPPED));
            save(anOrder(buyer, effectiveJava).placedAt("2026-06-03T00:00:00Z").status(OrderStatus.CANCELLED));
            Order pending = save(anOrder(buyer, effectiveJava).placedAt("2026-06-04T00:00:00Z"));

            assertThat(idsOf(orders.findPendingOrderedBefore(CUTOFF))).containsExactly(pending.getId());
        }

        @Test
        @DisplayName("findPendingOrderedBefore() puts the lower id first when two orders were placed at the same moment")
        void sameMomentLowestIdFirst() {
            Order first = save(anOrder(buyer, effectiveJava).placedAt("2026-06-05T00:00:00Z"));
            Order second = save(anOrder(buyer, effectiveJava).placedAt("2026-06-05T00:00:00Z"));

            assertThat(idsOf(orders.findPendingOrderedBefore(CUTOFF))).containsExactly(first.getId(), second.getId());
        }
    }

    private Member newMember(String email) {
        return entityManager.persist(Member.register(email, "Test Member", "{noop}secret"));
    }

    private Order save(OrderBuilder order) {
        return entityManager.persist(order.build());
    }

    /** Five pending orders of the member, one per day from June 1 to 5; returned newest first. */
    private List<Order> saveOrdersOnFiveDays(Member member) {
        List<Order> saved = new ArrayList<>();
        for (int day = 1; day <= 5; day++) {
            saved.addFirst(save(anOrder(member, effectiveJava).placedAt("2026-06-0%dT00:00:00Z".formatted(day))));
        }
        return saved;
    }

    private static List<Long> idsOf(List<Order> orders) {
        return orders.stream().map(Order::getId).toList();
    }
}
