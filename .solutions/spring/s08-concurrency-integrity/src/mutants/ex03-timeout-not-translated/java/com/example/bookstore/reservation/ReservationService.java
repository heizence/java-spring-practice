package com.example.bookstore.reservation;

import java.time.Clock;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import com.example.bookstore.inventory.Inventory;
import com.example.bookstore.inventory.InventoryRepository;
import com.example.bookstore.member.Member;
import com.example.bookstore.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Use cases for store pickup reservations. */
@Service
@RequiredArgsConstructor
public class ReservationService {

    /** How many copies of one book a member may hold in reservations at the same time. */
    public static final int MAX_COPIES_PER_MEMBER = 2;

    private final ReservationRepository reservationRepository;
    private final InventoryRepository inventoryRepository;
    private final MemberRepository memberRepository;
    private final Clock clock;

    /**
     * Reserves copies of a book for a member, who picks them up in the store. The copies leave the stock right away.
     *
     * <h2>Rules</h2>
     * <ul>
     *   <li>The first thing the transaction does is lock the book's stock row ({@code SELECT … FOR UPDATE}), and it
     *       holds the lock until it ends. So reservations of the same book run one after another, and each one sees
     *       the stock and the reservations that the ones before it committed. Read nothing before taking the lock:
     *       in MySQL's default isolation level, a transaction's plain reads keep seeing the data as it was at its
     *       first read.</li>
     *   <li>A member holds at most {@link #MAX_COPIES_PER_MEMBER} copies of a book in reservations, counting this
     *       one. Asking for more fails with {@link ErrorCode#RESERVATION_LIMIT_EXCEEDED}.</li>
     *   <li>The copies come out of the book's stock. If fewer are left, the reservation fails with
     *       {@link ErrorCode#OUT_OF_STOCK}.</li>
     *   <li>When another transaction holds the stock row's lock for longer than MySQL's lock wait timeout (2 seconds,
     *       {@code innodb_lock_wait_timeout} in {@code application.yml}), the reservation fails with
     *       {@link ErrorCode#STOCK_BUSY}, which tells the client to try again in a moment.</li>
     *   <li>A reservation that fails changes nothing.</li>
     * </ul>
     *
     * @param quantity how many copies; the request has already checked that it is between 1 and 10
     * @return the new reservation
     * @throws BusinessException {@link ErrorCode#BOOK_NOT_FOUND} or {@link ErrorCode#MEMBER_NOT_FOUND} if the book
     *                           or the member doesn't exist, and the codes in the rules above
     */
    @Transactional
    public ReservationResponse reserve(long bookId, long memberId, int quantity) {
        // Lock first. Until this transaction ends, other reservations of this book wait here. Because nothing was
        // read before, the reads below see everything that the reservations before this one committed.
        Inventory stock = lockStockOf(bookId);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND,
                        "Member %d was not found.".formatted(memberId)));

        long held = reservationRepository.sumQuantity(bookId, memberId);
        if (held + quantity > MAX_COPIES_PER_MEMBER) {
            throw new BusinessException(ErrorCode.RESERVATION_LIMIT_EXCEEDED,
                    "A member can hold at most %d copies of a book. Member %d holds %d of book %d and asked for %d."
                            .formatted(MAX_COPIES_PER_MEMBER, memberId, held, bookId, quantity));
        }
        stock.decrease(quantity); // OUT_OF_STOCK if fewer are left; Hibernate writes the change at commit

        Reservation reservation = Reservation.of(stock.getBook(), member, quantity, clock.instant());
        return ReservationResponse.from(reservationRepository.save(reservation));
    }

    /** @throws BusinessException {@link ErrorCode#NOT_FOUND} if no reservation has this id */
    @Transactional(readOnly = true)
    public ReservationResponse findReservation(long id) {
        return reservationRepository.findById(id)
                .map(ReservationResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "Reservation %d was not found.".formatted(id)));
    }

    private Inventory lockStockOf(long bookId) {
        // If the lock isn't free within MySQL's lock wait timeout, GlobalExceptionHandler answers 409.
        return inventoryRepository.findForUpdate(bookId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_NOT_FOUND,
                        "Book %d was not found.".formatted(bookId)));
    }
}
