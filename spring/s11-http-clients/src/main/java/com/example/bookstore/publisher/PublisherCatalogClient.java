package com.example.bookstore.publisher;

import java.util.List;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * The publishers' shared catalog: book data straight from the publishers, before the bookstore lists a book.
 *
 * <p>This is an HTTP interface: Spring generates the implementation from the annotations on the interface and on its
 * methods.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>Spring registers the client as a bean in the HTTP service group {@code publisher-catalog} (see
 *       {@link PublisherClientConfiguration}). Its settings come from {@code spring.http.serviceclient
 *       .publisher-catalog.*} in {@code application.yml}: the base URL {@code https://catalog.publisher.example}, a
 *       connect timeout of 1 second and a read timeout of 2 seconds.</li>
 *   <li>Every request carries the bookstore's API key in the header {@code X-Api-Key}. The key is a secret: it
 *       comes from the environment variable {@code PUBLISHER_CATALOG_API_KEY}, and outside production it falls back
 *       to {@code local-development-key}.</li>
 *   <li>Every request asks for JSON: {@code Accept: application/json}.</li>
 *   <li>Error statuses arrive as RestClient's exceptions, such as {@link HttpClientErrorException.NotFound} for an
 *       unknown ISBN. A catalog that doesn't answer within the read timeout fails with a
 *       {@link ResourceAccessException}.</li>
 * </ul>
 */
// TODO: S11-02
public interface PublisherCatalogClient {

    /** {@code GET /v1/books/{isbn}}: one book. */
    PublisherBook findBook(String isbn);

    /** {@code GET /v1/books?author={author}&limit={limit}}: books by an author, at most {@code limit} of them. */
    List<PublisherBook> findBooksByAuthor(String author, int limit);
}
