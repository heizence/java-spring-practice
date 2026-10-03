package com.example.bookstore.support;

import com.example.bookstore.member.Role;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;

/**
 * Signs MockMvc requests in as seed members, without real tokens:
 *
 * <pre>{@code
 * assertThat(mvc.get().uri("/api/wallet").with(TestUsers.minji())).hasStatusOk();
 * }</pre>
 *
 * <p>spring-security-test's {@code jwt()} puts an authenticated {@code JwtAuthenticationToken} straight into the
 * request's security context. No token is signed or decoded, so these requests work whatever the {@code JwtDecoder}
 * does, and they skip CSRF checks, as requests with a bearer token do. For the same reason the application's
 * claim-to-authority mapping doesn't run: each method below gives the authority ({@code ROLE_USER} or
 * {@code ROLE_ADMIN}) explicitly. To test the real token checks, send a real token from {@link TestTokens}.
 */
public final class TestUsers {

    /** Member 1: Minji Kim, a customer (role {@code USER}). */
    public static final long MINJI = 1;

    /** Member 20: the administrator (role {@code ADMIN}). */
    public static final long ADMIN = 20;

    private TestUsers() {
    }

    /** Signed in as Minji (member 1, role {@code USER}). */
    public static JwtRequestPostProcessor minji() {
        return member(MINJI, Role.USER);
    }

    /** Signed in as the administrator (member 20, role {@code ADMIN}). */
    public static JwtRequestPostProcessor admin() {
        return member(ADMIN, Role.ADMIN);
    }

    /**
     * Signed in as any member: a token whose subject is the member's id and whose {@code role} claim is the role,
     * and the authority {@code ROLE_<role>}, as the bookstore's tokens have them.
     */
    public static JwtRequestPostProcessor member(long id, Role role) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt
                        .subject(String.valueOf(id))
                        .claim("role", role.name())
                        .claims(claims -> claims.remove("scope"))) // jwt() adds "scope": "read" by default
                .authorities(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}
