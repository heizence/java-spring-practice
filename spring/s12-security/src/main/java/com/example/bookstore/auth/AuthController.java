package com.example.bookstore.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Signing in: {@code POST /api/auth/token} exchanges an email and password for an access token. It answers
 * {@code 200 OK}, not {@code 201 Created}: a token isn't a resource that the client can fetch again later.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final TokenService tokenService;

    @PostMapping("/token")
    public TokenResponse issueToken(@Valid @RequestBody TokenRequest request) {
        return tokenService.issueToken(request);
    }
}
