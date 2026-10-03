package com.example.bookstore.shipping;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * What the carrier charges to ship a parcel, and how long it takes. The carrier's JSON looks like this:
 *
 * <pre>{@code
 * { "carrier": "CJ Logistics", "fee": 3000, "currency": "KRW", "estimated_days": 2 }
 * }</pre>
 *
 * <p>The carrier writes its property names in snake case ({@code estimated_days}), the bookstore in camel case. The
 * record keeps the bookstore's names and maps the carrier's one. Properties the record doesn't have, such as
 * {@code currency}, are ignored.
 *
 * @param carrier       the carrier's name
 * @param fee           the fee in won
 * @param estimatedDays how many days the delivery takes; the carrier calls it {@code estimated_days}
 */
public record ShippingEstimate(String carrier, long fee, @JsonProperty("estimated_days") int estimatedDays) {
}
