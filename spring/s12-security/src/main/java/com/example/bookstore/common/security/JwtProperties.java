package com.example.bookstore.common.security;

import java.time.Duration;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

/**
 * Settings for the access tokens that the bookstore issues ({@code POST /api/auth/token}) and checks on every
 * request, bound from {@code bookstore.security.jwt.*} in {@code application.yml}.
 *
 * <p>The application doesn't start with an invalid value: every check below runs when Spring Boot binds the
 * properties.
 *
 * @param issuer     the {@code iss} claim of every token the bookstore issues. Tokens from another issuer are
 *                   rejected.
 * @param timeToLive how long a token is valid after it was issued, such as {@code 15m}
 * @param secret     the key that signs tokens and checks their signatures (HMAC-SHA256), Base64-encoded: at least
 *                   32 bytes (256 bits) once decoded
 */
@ConfigurationProperties(prefix = "bookstore.security.jwt")
public record JwtProperties(String issuer, Duration timeToLive, String secret) {

    /** HMAC-SHA256 needs a key of at least 256 bits; Spring Security refuses shorter ones. */
    static final int MIN_SECRET_BYTES = 32;

    public JwtProperties {
        Assert.hasText(issuer, "bookstore.security.jwt.issuer must not be blank");
        Assert.isTrue(timeToLive != null && timeToLive.isPositive(),
                () -> "bookstore.security.jwt.time-to-live must be positive: " + timeToLive);
        Assert.hasText(secret, "bookstore.security.jwt.secret must not be blank");
        // The message must not contain the secret: startup errors end up in logs.
        Assert.isTrue(Base64.getDecoder().decode(secret).length >= MIN_SECRET_BYTES,
                "bookstore.security.jwt.secret must be at least %d bytes long once Base64-decoded"
                        .formatted(MIN_SECRET_BYTES));
    }

    /** The decoded {@link #secret} as a key for HMAC-SHA256 ({@code HS256}). */
    public SecretKey secretKey() {
        return new SecretKeySpec(Base64.getDecoder().decode(secret), "HmacSHA256");
    }

    /** Leaves the secret out. A record's generated {@code toString()} would print it wherever the object is logged. */
    @Override
    public String toString() {
        return "JwtProperties[issuer=%s, timeToLive=%s, secret=(hidden)]".formatted(issuer, timeToLive);
    }
}
