package com.example.bookstore.shipping;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Binds {@link ShippingProperties}, so that {@link ShippingClient} can inject them. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ShippingProperties.class)
public class ShippingConfiguration {
}
