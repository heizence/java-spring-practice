package com.example.bookstore.support;

import org.flywaydb.core.Flyway;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * The test helpers that {@link IntegrationTest} adds to the application context. They are beans, so each
 * application context has its own and tests get them with {@code @Autowired}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class IntegrationTestConfiguration {

    /** Replaces the application's clock wherever a {@code Clock} is injected, including auditing. */
    @Bean
    @Primary
    TestClock testClock() {
        return new TestClock();
    }

    /** Replaces the application's {@code cacheTicker} wherever Caffeine's {@code Ticker} is injected. */
    @Bean
    @Primary
    FakeTicker fakeTicker() {
        return new FakeTicker();
    }

    @Bean
    SqlStatementCounter sqlStatementCounter() {
        return new SqlStatementCounter();
    }

    /** Hands the counter to Hibernate without touching {@code application.yml}. */
    @Bean
    HibernatePropertiesCustomizer sqlStatementCounterRegistration(SqlStatementCounter counter) {
        return properties -> properties.put(AvailableSettings.STATEMENT_INSPECTOR, counter);
    }

    @Bean
    SeedData seedData(Flyway flyway) {
        return new SeedData(flyway);
    }
}
