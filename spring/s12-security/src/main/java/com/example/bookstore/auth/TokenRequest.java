package com.example.bookstore.auth;

import jakarta.validation.constraints.NotBlank;

/** The body of {@code POST /api/auth/token}: a member's email and password. */
public record TokenRequest(@NotBlank String email, @NotBlank String password) {

    /** Leaves the password out, so that it can't end up in a log. */
    @Override
    public String toString() {
        return "TokenRequest[email=%s, password=(hidden)]".formatted(email);
    }
}
