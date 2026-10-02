package com.example.bookstore.book;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Operations on whole categories for the purchasing team. (S12 restricts them to administrators.) */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final PriceChangeService priceChangeService;

    /** {@code POST /api/categories/{categoryId}/price-changes} with {@code { "percent": 10 }}. */
    @PostMapping("/{categoryId}/price-changes")
    public PriceChangeResponse changePrices(@PathVariable long categoryId,
                                            @Valid @RequestBody PriceChangeRequest request) {
        return priceChangeService.changePrices(categoryId, request.percent());
    }
}
