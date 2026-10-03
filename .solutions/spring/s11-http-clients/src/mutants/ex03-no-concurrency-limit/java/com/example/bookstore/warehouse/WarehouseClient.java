package com.example.bookstore.warehouse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

/**
 * Asks the central warehouse how many copies of a book it has.
 *
 * <p>The warehouse's API is shared by every shop of the company. When it is overloaded, it answers
 * {@code 503 Service Unavailable} for a moment, and it allows each client at most 4 requests at the same time.
 */
@Component
public class WarehouseClient {

    /** How many calls to the warehouse may run at the same time; more calls wait for a free place. */
    public static final int MAX_CONCURRENT_CALLS = 4;

    private final RestClient restClient;

    public WarehouseClient(RestClient.Builder builder, WarehouseProperties properties) {
        this.restClient = builder.baseUrl(properties.baseUrl()).build();
    }

    /**
     * How many copies of a book the warehouse can send right away.
     *
     * <h2>Rules</h2>
     * <ul>
     *   <li>The request is {@code GET /v1/stock/{isbn}} on the base URL, with {@code Accept: application/json}. The
     *       answer is a {@link StockLevel}.</li>
     *   <li>{@code 503}: the warehouse is busy for a moment. Throw a {@link WarehouseUnavailableException}, and try
     *       again: at most 3 more times (4 attempts in total), waiting 100 ms before the first retry, 200 ms before
     *       the second and 400 ms before the third. If the last attempt gets {@code 503} too, the
     *       {@link WarehouseUnavailableException} reaches the caller.</li>
     *   <li>Any other error status is not tried again. A {@code 4xx} means that our request was wrong, and a
     *       {@code 500} usually means a bug on the warehouse's side that the same request triggers again. They
     *       reach the caller as RestClient's {@link HttpClientErrorException} and {@link HttpServerErrorException},
     *       after one attempt.</li>
     *   <li>At most {@link #MAX_CONCURRENT_CALLS} calls of this method run at the same time. Further calls wait until
     *       one of them has finished.</li>
     * </ul>
     *
     * @param isbn the book's 13-digit ISBN
     * @throws WarehouseUnavailableException if the warehouse still answers {@code 503} after the last attempt
     */
    // Retry only the failure that a later attempt can fix. A GET changes nothing on the warehouse's side, so sending
    // it again is safe; a POST that creates something would need an idempotency key first (S08-06).
    // The HTTP client's connection pool already limits how many requests run at the same time.
    @Retryable(includes = WarehouseUnavailableException.class, maxRetries = 3, delay = 100, multiplier = 2)
    public int availableCopies(String isbn) {
        StockLevel stock = restClient.get()
                .uri("/v1/stock/{isbn}", isbn)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(status -> status.isSameCodeAs(HttpStatus.SERVICE_UNAVAILABLE), (request, response) -> {
                    throw new WarehouseUnavailableException("The warehouse answered 503 for book " + isbn + ".");
                })
                .body(StockLevel.class);
        return stock.available();
    }
}
