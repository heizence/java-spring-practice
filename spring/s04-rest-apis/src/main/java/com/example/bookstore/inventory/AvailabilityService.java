package com.example.bookstore.inventory;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.example.bookstore.book.BookRepository;
import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Tells customers whether a book is in stock and when it would arrive.
 *
 * <p>The warehouse is in Korea and works in Korean time: an order placed before 14:00 there ships the same day, a
 * later one the next day, and a shipped book arrives one day later. (Weekends and holidays don't exist here yet.)
 */
@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private static final ZoneId WAREHOUSE_ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalTime SAME_DAY_CUTOFF = LocalTime.of(14, 0);

    private final BookRepository bookRepository;
    private final InventoryRepository inventoryRepository;
    private final Clock clock;

    /** @throws BusinessException {@link ErrorCode#BOOK_NOT_FOUND} if no book has this id */
    public AvailabilityResponse check(long bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND, "Book %d was not found.".formatted(bookId));
        }
        int quantity = inventoryRepository.findById(bookId).map(Inventory::quantity).orElse(0);
        Instant now = clock.instant();
        Availability status = Availability.of(quantity);
        if (status == Availability.OUT_OF_STOCK) {
            return new AvailabilityResponse(bookId, status, quantity, null, null, now);
        }
        ZonedDateTime warehouseNow = now.atZone(WAREHOUSE_ZONE);
        LocalDate shippingDay = warehouseNow.toLocalTime().isBefore(SAME_DAY_CUTOFF)
                ? warehouseNow.toLocalDate()
                : warehouseNow.toLocalDate().plusDays(1);
        ZonedDateTime orderBy = shippingDay.atTime(SAME_DAY_CUTOFF).atZone(WAREHOUSE_ZONE);
        return new AvailabilityResponse(bookId, status, quantity, orderBy, shippingDay.plusDays(1), now);
    }
}
