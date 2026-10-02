package com.example.bookstore.notification;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Mutant: the ApiMailSender exists only in "prod", so a server without that profile has no mail sender at all. */
@Configuration(proxyBeanMethods = false)
public class MailConfiguration {

    @Bean
    @Profile("local")
    FakeMailSender fakeMailSender() {
        return new FakeMailSender();
    }

    @Configuration(proxyBeanMethods = false)
    @Profile("prod")
    @EnableConfigurationProperties(MailApiProperties.class)
    static class ApiMailConfiguration {

        @Bean
        ApiMailSender apiMailSender(MailApiProperties properties) {
            return new ApiMailSender(properties);
        }
    }
}
