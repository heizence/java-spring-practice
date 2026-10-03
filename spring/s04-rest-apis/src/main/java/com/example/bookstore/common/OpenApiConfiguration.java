package com.example.bookstore.common;

import org.springframework.context.annotation.Configuration;

/**
 * The general part of the API's OpenAPI document, which springdoc-openapi publishes at {@code /v3/api-docs} and
 * shows at {@code /swagger-ui.html}. springdoc builds the rest from the controllers, their annotations and the
 * request and response records.
 *
 * <p>TODO: S04-09: describe the API here: the title {@code Bookstore API} and the version {@code 1}. A {@code @Bean}
 * method runs while the application starts, so it must never throw: write it whole, not as a stub.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {
}
