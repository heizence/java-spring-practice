package com.example.bookstore.shipping;

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
public class ShippingConfiguration {

    // TODO: S02-01
}
