package com.example.bookstore.notification;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Mutant: the FakeMailSender has no @Profile, so it exists in production too, next to the ApiMailSender. */
@Configuration(proxyBeanMethods = false)
public class MailConfiguration {

    @Bean
    FakeMailSender fakeMailSender() {
        return new FakeMailSender();
    }

    @Configuration(proxyBeanMethods = false)
    @Profile("!local")
    @EnableConfigurationProperties(MailApiProperties.class)
    static class ApiMailConfiguration {

        @Bean
        ApiMailSender apiMailSender(MailApiProperties properties) {
            return new ApiMailSender(properties);
        }
    }
}
