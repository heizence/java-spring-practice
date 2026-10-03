package com.example.bookstore.inventory;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** {@code GET /api/books/{id}/availability}: whether a book is in stock, and when it would arrive. */
@RestController
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @GetMapping("/api/books/{id}/availability")
    public AvailabilityResponse getAvailability(@PathVariable long id) {
        return availabilityService.check(id);
    }
}
