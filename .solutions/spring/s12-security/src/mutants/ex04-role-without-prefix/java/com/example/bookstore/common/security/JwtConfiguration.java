package com.example.bookstore.common.security;

import java.time.Clock;
import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/**
 * The bookstore's access tokens: JSON Web Tokens signed with HMAC-SHA256 ({@code HS256}) and the secret from
 * {@link JwtProperties}. The bookstore issues them itself ({@code POST /api/auth/token}), and the resource server in
 * {@link SecurityConfiguration} checks them on every request.
 *
 * <p>A token's claims: {@code iss} (the bookstore), {@code sub} (the member's id), {@value #ROLE_CLAIM} (the member's
 * role, {@code USER} or {@code ADMIN}), {@code iat} (issued at) and {@code exp} (expires at).
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfiguration {

    /** The claim that holds the member's role, such as {@code "role": "ADMIN"}. */
    public static final String ROLE_CLAIM = "role";

    /** Signs the tokens that {@code TokenService} issues: HS256 with the configured secret. */
    @Bean
    JwtEncoder jwtEncoder(JwtProperties properties) {
        return NimbusJwtEncoder.withSecretKey(properties.secretKey())
                .algorithm(MacAlgorithm.HS256)
                .build();
    }

    /**
     * Checks the token of every request that has one ({@code Authorization: Bearer …}). It accepts a token only if
     * all of these hold, and otherwise the request gets {@code 401}:
     *
     * <ul>
     *   <li>it is signed with HS256 and the configured secret (no other algorithm, and never {@code alg: none});</li>
     *   <li>its {@code iss} is the configured issuer;</li>
     *   <li>it has an {@code exp}, and that moment hasn't passed by the application's {@link Clock} bean (with
     *       Spring Security's default leeway of 60 seconds).</li>
     * </ul>
     */
    @Bean
    JwtDecoder jwtDecoder(JwtProperties properties, Clock clock) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(properties.secretKey())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        // Spring Security reads the system clock unless you hand it one. Use the application's clock, like all other
        // code that needs "now": tokens are issued with it, so they must also expire by it (and tests control it).
        var timestamps = new JwtTimestampValidator();
        timestamps.setClock(clock);
        // By default, a token without "exp" is valid forever. Ours always have one, so refuse those that don't.
        timestamps.setAllowEmptyExpiryClaim(false);
        decoder.setJwtValidator(JwtValidators.createDefaultWithValidators(
                List.of(timestamps, new JwtIssuerValidator(properties.issuer()))));
        return decoder;
    }

    /**
     * Turns the {@value #ROLE_CLAIM} claim into the authorities of the request: {@code "role": "ADMIN"} becomes
     * {@code ROLE_ADMIN}, so that {@code hasRole("ADMIN")} works in URL rules and {@code @PreAuthorize}. The
     * resource server finds this bean by its type.
     */
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        var authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(ROLE_CLAIM);
        authorities.setAuthorityPrefix("");
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
