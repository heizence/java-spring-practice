package com.example.bookstore.support;

import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

import com.example.bookstore.common.security.JwtProperties;
import com.example.bookstore.member.Role;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;

/**
 * Real access tokens for tests that send them through the resource server, and a check for the tokens that the
 * bookstore issues:
 *
 * <pre>{@code
 * var tokens = new TestTokens(jwtProperties, clock);
 * mvc.get().uri("/api/members/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.validFor(1, Role.USER));
 * }</pre>
 *
 * <p>The tokens are signed like the bookstore signs its own (HS256 with the configured secret), but with the Nimbus
 * JOSE + JWT library directly, not with the application's {@code JwtEncoder}. So these tests show what the
 * application's {@code JwtDecoder} accepts, whatever the encoder does. Times come from the clock passed in, which in
 * integration tests is the {@link TestClock}.
 */
public final class TestTokens {

    private final JwtProperties properties;
    private final Clock clock;

    public TestTokens(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    /** A valid token for a member, issued now and expiring after the configured time to live. */
    public String validFor(long memberId, Role role) {
        return sign(claimsFor(memberId, role).build());
    }

    /**
     * The claims of a valid token, as a builder: change one, then {@link #sign} them. For example, a token that
     * expired an hour ago: {@code tokens.sign(tokens.claimsFor(1, Role.USER).expirationTime(anHourAgo).build())}.
     */
    public JWTClaimsSet.Builder claimsFor(long memberId, Role role) {
        Instant now = clock.instant();
        return new JWTClaimsSet.Builder()
                .issuer(properties.issuer())
                .subject(String.valueOf(memberId))
                .claim("role", role.name())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(properties.timeToLive())));
    }

    /** Signs claims with the bookstore's secret (HS256). */
    public String sign(JWTClaimsSet claims) {
        return sign(claims, properties.secretKey().getEncoded());
    }

    /**
     * Signs claims with any secret of at least 32 bytes (HS256). A secret that isn't the bookstore's makes a
     * forgery.
     */
    public static String sign(JWTClaimsSet claims, byte[] secret) {
        try {
            var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(secret));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Could not sign the test token", e);
        }
    }

    /** A token without a signature ({@code "alg": "none"}): anyone can write one. */
    public static String unsigned(JWTClaimsSet claims) {
        return new PlainJWT(claims).serialize();
    }

    /**
     * Parses a token and checks that the bookstore's secret signed it.
     *
     * @return the parsed token, to read its header and claims
     * @throws AssertionError if the token is malformed or its signature doesn't match the secret
     */
    public SignedJWT verify(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!jwt.verify(new MACVerifier(properties.secretKey().getEncoded()))) {
                throw new AssertionError("The token's signature doesn't match the configured secret: " + token);
            }
            return jwt;
        } catch (ParseException | JOSEException e) {
            throw new AssertionError("Not a signed JWT: " + token, e);
        }
    }
}
