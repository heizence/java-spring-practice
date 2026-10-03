package com.example.bookstore.reservation;

import java.net.URI;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Store pickup reservations: {@code POST /api/books/{bookId}/reservations} and {@code GET /api/reservations/{id}}. */
@RestController
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    /** Reserves copies: {@code 201 Created} with the reservation and its {@code Location}. */
    @PostMapping("/api/books/{bookId}/reservations")
    public ResponseEntity<ReservationResponse> reserve(@PathVariable long bookId,
                                                       @Valid @RequestBody ReserveRequest request) {
        ReservationResponse reservation = reservationService.reserve(bookId, request.memberId(), request.quantity());
        return ResponseEntity.created(URI.create("/api/reservations/" + reservation.id())).body(reservation);
    }

    @GetMapping("/api/reservations/{id}")
    public ReservationResponse getReservation(@PathVariable long id) {
        return reservationService.findReservation(id);
    }
}
