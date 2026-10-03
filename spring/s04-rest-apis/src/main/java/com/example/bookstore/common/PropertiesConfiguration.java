package com.example.bookstore.common;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Configuration;

/**
 * Makes every {@code @ConfigurationProperties} record of the application a bean, such as {@code AdminProperties},
 * filled from {@code application.yml} and checked at startup.
 *
 * <p>A class of its own, like {@link ClockConfiguration}: settings on {@code BookstoreApplication} would also apply
 * to every test slice that starts from it.
 */
@Configuration(proxyBeanMethods = false)
@ConfigurationPropertiesScan("com.example.bookstore")
public class PropertiesConfiguration {
}
