package com.example.bookstore.reservation;

import java.time.Instant;

/** A reservation as the API shows it. */
public record ReservationResponse(Long id, Long bookId, Long memberId, int quantity, Instant reservedAt) {

    static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(reservation.getId(), reservation.getBook().getId(),
                reservation.getMember().getId(), reservation.getQuantity(), reservation.getReservedAt());
    }
}
