package com.example.bookstore.common.web;

import java.util.List;

import com.example.bookstore.member.CurrentMemberArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the web layer's cross-cutting parts: the request-id filter, the admin token interceptor and the
 * {@code @CurrentMember} argument resolver.
 *
 * <p>{@link WebMvcConfigurer} is Spring MVC's hook for such settings: Spring Boot keeps its own MVC setup and calls
 * these methods once, at startup, to add ours.
 */
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class WebConfiguration implements WebMvcConfigurer {

    private final AdminTokenInterceptor adminTokenInterceptor;
    private final CurrentMemberArgumentResolver currentMemberArgumentResolver;

    /**
     * The request-id filter, at the front of the filter chain (the highest precedence): the filters after it, such
     * as Spring Security's in S12, and every log line can then use the id. A {@link FilterRegistrationBean} sets the
     * order and the URL patterns. A filter that is a {@code @Component} instead runs for every URL, at the lowest
     * precedence unless it has an {@code @Order}.
     */
    @Bean
    public FilterRegistrationBean<RequestIdFilter> requestIdFilter() {
        var registration = new FilterRegistrationBean<>(new RequestIdFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");
        return registration;
    }

    /** The admin token check, only for the admin API. */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminTokenInterceptor).addPathPatterns("/api/admin/**");
    }

    /** Lets controller methods declare a {@code @CurrentMember Member} parameter. */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentMemberArgumentResolver);
    }
}
