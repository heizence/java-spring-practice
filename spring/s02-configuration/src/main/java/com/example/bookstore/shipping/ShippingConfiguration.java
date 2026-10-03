package com.example.bookstore.shipping;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registers {@link ShippingProperties}, so that Spring Boot binds and validates them at startup, and other beans can
 * inject them.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>A {@code @Configuration(proxyBeanMethods = false)} class that component scanning finds, annotated
 *       {@code @EnableConfigurationProperties(ShippingProperties.class)}.</li>
 * </ul>
 */
// The registration lives in the shipping feature, next to the record. @ConfigurationPropertiesScan on the application
// class would also work, but it would register every properties record in the application wherever any part of it
// starts, including tests that need none of them.
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ShippingProperties.class)
public class ShippingConfiguration {
}
