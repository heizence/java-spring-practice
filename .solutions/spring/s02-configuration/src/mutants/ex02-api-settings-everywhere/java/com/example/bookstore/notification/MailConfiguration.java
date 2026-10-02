package com.example.bookstore.notification;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Mutant: @EnableConfigurationProperties sits on the outer class, which has no profile, so the API settings (and their
 * validation) exist in every profile: a developer's machine needs an API key to start.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MailApiProperties.class)
public class MailConfiguration {

    @Bean
    @Profile("local")
    FakeMailSender fakeMailSender() {
        return new FakeMailSender();
    }

    @Configuration(proxyBeanMethods = false)
    @Profile("!local")
    static class ApiMailConfiguration {

        @Bean
        ApiMailSender apiMailSender(MailApiProperties properties) {
            return new ApiMailSender(properties);
        }
    }
}
