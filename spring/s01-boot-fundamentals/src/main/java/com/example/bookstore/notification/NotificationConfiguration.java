package com.example.bookstore.notification;

/**
 * Chooses how the bookstore notifies members, from the configuration property
 * {@code bookstore.notifications.channel}.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>A {@code @Configuration(proxyBeanMethods = false)} class that component scanning finds. Its {@code @Bean}
 *       methods are named {@code emailNotifier}, {@code noOpNotifier} and {@code mailSender}. The notifier and mail
 *       sender classes have no {@code @Component}: this class decides which of them become beans.</li>
 *   <li>Channel {@code email} (in any letter case): the {@code Notifier} bean is an {@link EmailNotifier} that sends
 *       through the {@link MailSender} bean.</li>
 *   <li>Channel {@code none}, or no channel property at all: the {@code Notifier} bean is a {@link NoOpNotifier}.</li>
 *   <li>Any other channel, such as the typo {@code emial}, registers no {@code Notifier} at all, so that a bean that
 *       needs one stops the application at startup. A silent no-op would hide the typo until customers ask where
 *       their emails are.</li>
 *   <li>The {@code MailSender} bean is a {@link LoggingMailSender}, unless the application has a {@code MailSender}
 *       bean of its own: then this default backs off.</li>
 * </ul>
 */
public class NotificationConfiguration {

    // TODO: S01-05
}
