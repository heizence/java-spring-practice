package com.example.bookstore.warehouse;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Binds {@link WarehouseProperties}, so that {@link WarehouseClient} can inject them. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(WarehouseProperties.class)
public class WarehouseConfiguration {
}
