package com.example.bookstore.publisher;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * Registers the HTTP interface {@link PublisherCatalogClient} as a bean, in the HTTP service group
 * {@code publisher-catalog}. Spring Boot configures each group's client from {@code spring.http.serviceclient.<group>}
 * in {@code application.yml}.
 */
// One group per remote service: all interfaces of a group share one RestClient, with one base URL and one set of
// timeouts and headers.
@Configuration(proxyBeanMethods = false)
@ImportHttpServices(group = "publisher-catalog", types = PublisherCatalogClient.class)
public class PublisherClientConfiguration {
}
