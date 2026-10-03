package com.example.bookstore.common.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Who may call which endpoint, and how requests prove who they are.
 *
 * <p>The bookstore's API is <em>stateless</em>: every request that needs a member carries an access token
 * ({@code Authorization: Bearer …}) from {@code POST /api/auth/token}. There is no login session and no cookie.
 * {@link EnableMethodSecurity @EnableMethodSecurity} switches on {@code @PreAuthorize} checks on bean methods.
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class SecurityConfiguration {

    /**
     * The rules for every request, checked in this order (the first rule that matches decides):
     *
     * <ol>
     *   <li>{@code GET /api/books/**}: anyone. Only reading is public; changing a book needs a token.</li>
     *   <li>{@code POST /api/members} (sign-up) and {@code POST /api/auth/token} (sign-in): anyone, because nobody
     *       has a token yet when they call them.</li>
     *   <li>{@code /api/admin/**}, any method: only members with the role {@code ADMIN}.</li>
     *   <li>Everything else, including paths that no controller handles: any signed-in member. A new endpoint is
     *       protected until someone decides otherwise (deny by default).</li>
     * </ol>
     *
     * <p>Errors are problem details, like every other error of the API: a request without a valid token gets
     * {@code 401} with code {@code UNAUTHORIZED}, a member without the required role gets {@code 403} with code
     * {@code FORBIDDEN}. The chain creates no HTTP session, and it doesn't ask for CSRF tokens: requests carry
     * their token in a header, which browsers never add on their own.
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver)
            throws Exception {
        // The filter chain runs before Spring MVC, so @RestControllerAdvice never sees its errors. Hand them to MVC's
        // exception resolver: GlobalExceptionHandler then writes them like every other error.
        AuthenticationEntryPoint unauthorized = (request, response, exception) ->
                resolver.resolveException(request, response, null, exception);
        AccessDeniedHandler forbidden = (request, response, exception) ->
                resolver.resolveException(request, response, null, exception);

        return http
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HttpMethod.GET, "/api/books/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/members", "/api/auth/token").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                // Bearer tokens, checked by the JwtDecoder bean. The resource server answers an invalid token itself,
                // so it needs the entry point too; otherwise those 401s would have an empty body.
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(unauthorized))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(unauthorized)
                        .accessDeniedHandler(forbidden))
                // CSRF attacks need a browser that sends credentials by itself, such as a session cookie. A bearer
                // token is never sent automatically, so a stateless token API turns CSRF protection off.
                .csrf(csrf -> csrf.disable())
                .build();
    }

    /**
     * Hashes passwords for storage and checks them at sign-in. Spring Security's delegating encoder writes BCrypt
     * hashes with the algorithm as a prefix ({@code {bcrypt}$2a$10$…}), and checks hashes of every algorithm it
     * knows. When a stronger algorithm becomes the default, old hashes keep working.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
