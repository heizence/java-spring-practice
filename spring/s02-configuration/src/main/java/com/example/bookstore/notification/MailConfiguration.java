package com.example.bookstore.notification;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Chooses the bookstore's {@link MailSender} by profile.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>A {@code @Configuration(proxyBeanMethods = false)} class that component scanning finds.</li>
 *   <li>In the {@code local} profile, the {@code MailSender} bean is a {@link FakeMailSender}. It needs no API
 *       settings.</li>
 *   <li>In every other case (the {@code prod} profile, any other profile, or no profile at all), the
 *       {@code MailSender} bean is an {@link ApiMailSender} built from {@link MailApiProperties}. Only {@code local}
 *       gets the fake: a server started without a profile must not switch real mail off without a word.</li>
 *   <li>{@code MailApiProperties} are registered with {@code @EnableConfigurationProperties} only where the
 *       {@code ApiMailSender} is, so that the {@code local} profile doesn't need an API key, and every other case
 *       can't start without one.</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
public class MailConfiguration {

    @Bean
    @Profile("local")
    FakeMailSender fakeMailSender() {
        return new FakeMailSender();
    }

    // "!local", not "prod": the safe default is the real sender. Forgetting SPRING_PROFILES_ACTIVE on a server then
    // stops the application for want of an API key, instead of quietly sending no mail at all.
    //
    // A nested configuration class, because @Profile and @EnableConfigurationProperties must apply together: the
    // properties (and their validation) exist only where the ApiMailSender does.
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
