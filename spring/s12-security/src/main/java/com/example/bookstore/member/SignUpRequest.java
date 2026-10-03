package com.example.bookstore.member;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The body of {@code POST /api/members}. There is no {@code role} field on purpose: everyone who signs up becomes a
 * customer ({@code USER}), whatever JSON they send.
 *
 * @param password the password in plain text, 8 to 72 characters. {@code MemberService} also checks that it fits
 *                 into 72 bytes, the most BCrypt can use.
 */
public record SignUpRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(min = 8, max = 72) String password) {

    /** Leaves the password out, so that it can't end up in a log. */
    @Override
    public String toString() {
        return "SignUpRequest[email=%s, name=%s, password=(hidden)]".formatted(email, name);
    }
}
