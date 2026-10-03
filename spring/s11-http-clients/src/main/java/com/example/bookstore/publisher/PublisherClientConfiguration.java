package com.example.bookstore.publisher;

import org.springframework.context.annotation.Configuration;

/**
 * Registers the HTTP interface {@link PublisherCatalogClient} as a bean, in the HTTP service group
 * {@code publisher-catalog}. Spring Boot configures each group's client from {@code spring.http.serviceclient.<group>}
 * in {@code application.yml}.
 */
// TODO: S11-02
@Configuration(proxyBeanMethods = false)
public class PublisherClientConfiguration {
}
