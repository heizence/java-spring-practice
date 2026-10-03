package com.example.bookstore.common.web;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the web layer's cross-cutting parts: the request-id filter, the admin token interceptor and the
 * {@code @CurrentMember} argument resolver.
 *
 * <p>{@link WebMvcConfigurer} is Spring MVC's hook for such settings: Spring Boot keeps its own MVC setup and calls
 * these methods once, at startup, to add ours.
 *
 * <p>S04-05: Spring calls these methods while the application starts, so they must never throw: a failure here
 * would stop the whole application. That's why they are empty instead of throwing like the other stubs.
 * <ul>
 *   <li>TODO: S04-05: register {@link RequestIdFilter} for every URL, at the front of the filter chain (the
 *       highest precedence). A {@code FilterRegistrationBean} sets the order and the URL patterns.</li>
 *   <li>TODO: S04-05: register {@link AdminTokenInterceptor} for {@code /api/admin/**} only.</li>
 *   <li>TODO: S04-05: register {@code CurrentMemberArgumentResolver}.</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
public class WebConfiguration implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // TODO: S04-05
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        // TODO: S04-05
    }
}
