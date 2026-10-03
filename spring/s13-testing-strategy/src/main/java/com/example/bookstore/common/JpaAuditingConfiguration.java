package com.example.bookstore.common;

import java.time.Clock;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Turns on Spring Data JPA auditing: when an entity is saved, Spring Data fills its {@code @CreatedDate} and
 * {@code @LastModifiedDate} fields, such as those of {@link BaseTimeEntity}.
 *
 * <p>This is a separate class on purpose. Web slice tests ({@code @WebMvcTest}) skip {@code @Configuration} classes,
 * so they start without JPA. If {@code @EnableJpaAuditing} sat on the application class, every web slice test would
 * fail because it has no JPA metamodel.
 */
@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class JpaAuditingConfiguration {

    /**
     * Takes "now" from the {@link Clock} bean. The default provider calls {@code LocalDateTime.now()}, which uses the
     * JVM's time zone and can't be controlled by tests.
     */
    @Bean
    public DateTimeProvider auditingDateTimeProvider(Clock clock) {
        return () -> Optional.of(clock.instant());
    }
}
