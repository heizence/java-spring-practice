package com.example.bookstore.order;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.example.bookstore.book.Book;
import com.example.bookstore.common.BaseTimeEntity;
import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import com.example.bookstore.member.Member;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

/**
 * A member's order. It owns its {@link OrderLine}s: they are saved, updated and deleted with the order, and the
 * total is recalculated whenever they change.
 *
 * <p>Status changes follow {@link OrderStatus#canChangeTo}: {@code PENDING → PAID → SHIPPED}, or
 * {@code CANCELLED} before shipping. Lines can only change while the order is {@code PENDING}.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "orders")
public class Order extends BaseTimeEntity {

    // equals() and hashCode() are inherited from Object on purpose; BaseTimeEntity explains why.

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private Member member;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    /** The sum of the lines' amounts in won, kept up to date by {@link #addLine} and {@link #removeLine}. */
    private long totalAmount;

    /** When the member placed the order (a business time, unlike {@code createdAt}, which records the insert). */
    private Instant orderedAt;

    /**
     * In the order they were added. The order owns its lines: saving the order saves its new lines, and a line
     * removed from this list is deleted from the database.
     */
    // mappedBy: OrderLine.order owns the foreign key (order_lines.order_id); this side only mirrors it.
    // cascade = ALL: saving (or deleting) the order does the same to its lines; the service never saves a line.
    // orphanRemoval: a line that leaves this list has no order anymore, so Hibernate deletes its row.
    @OneToMany(mappedBy = "order", orphanRemoval = true)
    @OrderBy("id")
    private List<OrderLine> lines = new ArrayList<>();

    private Order(Member member, Instant orderedAt) {
        Assert.notNull(member, "member must not be null");
        Assert.notNull(orderedAt, "orderedAt must not be null");
        this.member = member;
        this.orderedAt = orderedAt;
        this.status = OrderStatus.PENDING;
    }

    /** Starts a {@code PENDING} order without lines; add them with {@link #addLine}. */
    public static Order place(Member member, Instant orderedAt) {
        return new Order(member, orderedAt);
    }

    /** The lines, read-only: change them through {@link #addLine} and {@link #removeLine}. */
    public List<OrderLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    /**
     * Adds copies of a book at its current price, and updates the total. Each book appears in at most one line:
     * adding a book that is already in the order increases that line's quantity. A new line belongs to this order
     * ({@link OrderLine#getOrder()}). If the method throws, the order doesn't change.
     *
     * @throws IllegalArgumentException if {@code book} is {@code null} or {@code quantity} is less than 1
     * @throws BusinessException        {@link ErrorCode#INVALID_ORDER_STATUS} unless the order is {@code PENDING}
     */
    public void addLine(Book book, int quantity) {
        Assert.notNull(book, "book must not be null");
        Assert.isTrue(quantity > 0, () -> "quantity must be positive: " + quantity);
        checkLinesCanChange();
        // A new line gets "this" as its order. JPA writes order_lines.order_id from OrderLine.order (the owning
        // side), so a line that was only added to this list would be saved without its order.
        lines.stream()
                .filter(line -> line.isFor(book))
                .findFirst()
                .ifPresentOrElse(line -> line.addQuantity(quantity),
                        () -> lines.add(new OrderLine(this, book, quantity)));
        recalculateTotal();
    }

    /**
     * Removes the line of a book, and updates the total. Does nothing if the book isn't in the order. If the method
     * throws, the order doesn't change.
     *
     * @throws IllegalArgumentException if {@code book} is {@code null}
     * @throws BusinessException        {@link ErrorCode#INVALID_ORDER_STATUS} unless the order is {@code PENDING}
     */
    public void removeLine(Book book) {
        Assert.notNull(book, "book must not be null");
        checkLinesCanChange();
        // No repository call: orphanRemoval deletes the removed line's row when the transaction is flushed.
        lines.removeIf(line -> line.isFor(book));
        recalculateTotal();
    }

    /** @throws BusinessException {@link ErrorCode#INVALID_ORDER_STATUS} unless the order is {@code PENDING} */
    public void markPaid() {
        changeStatus(OrderStatus.PAID);
    }

    /** @throws BusinessException {@link ErrorCode#INVALID_ORDER_STATUS} unless the order is {@code PAID} */
    public void ship() {
        changeStatus(OrderStatus.SHIPPED);
    }

    /**
     * Cancels a pending or paid order. Refunding a paid order and returning the copies to stock are the caller's
     * job, because they change other aggregates.
     *
     * @throws BusinessException {@link ErrorCode#INVALID_ORDER_STATUS} if the order was shipped or already cancelled
     */
    public void cancel() {
        changeStatus(OrderStatus.CANCELLED);
    }

    private void changeStatus(OrderStatus next) {
        if (!status.canChangeTo(next)) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS,
                    "%s is %s, so it can't become %s.".formatted(describe(), status, next));
        }
        this.status = next;
    }

    private void checkLinesCanChange() {
        if (status != OrderStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS,
                    "%s is %s; its lines can only change while it is PENDING.".formatted(describe(), status));
        }
    }

    private void recalculateTotal() {
        this.totalAmount = lines.stream().mapToLong(OrderLine::getAmount).reduce(0L, Math::addExact);
    }

    private String describe() {
        return id == null ? "The new order" : "Order " + id;
    }
}
