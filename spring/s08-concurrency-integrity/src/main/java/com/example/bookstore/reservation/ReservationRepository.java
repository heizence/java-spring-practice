package com.example.bookstore.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Stores {@link Reservation}s. */
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /** How many copies of a book a member holds in reservations: 0 if none. */
    @Query("""
            select coalesce(sum(r.quantity), 0) from Reservation r
            where r.book.id = :bookId and r.member.id = :memberId""")
    long sumQuantity(long bookId, long memberId);
}
