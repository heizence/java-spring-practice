package com.example.bookstore.auth;

/**
 * An access token, as {@code POST /api/auth/token} returns it:
 * {@code {"accessToken": "eyJ…", "tokenType": "Bearer", "expiresIn": 900}}.
 *
 * @param accessToken the signed JWT. The client sends it with every request: {@code Authorization: Bearer eyJ…}.
 * @param tokenType   always {@code Bearer}
 * @param expiresIn   how many seconds the token is valid
 */
public record TokenResponse(String accessToken, String tokenType, long expiresIn) {

    static TokenResponse bearer(String accessToken, long expiresIn) {
        return new TokenResponse(accessToken, "Bearer", expiresIn);
    }
}
