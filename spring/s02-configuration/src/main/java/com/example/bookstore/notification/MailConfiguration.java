package com.example.bookstore.notification;

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
public class MailConfiguration {

    // TODO: S02-02
}
