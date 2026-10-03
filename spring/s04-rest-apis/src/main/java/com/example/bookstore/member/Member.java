package com.example.bookstore.member;

import java.util.Locale;

import org.springframework.util.Assert;

/**
 * A customer or an administrator of the bookstore. The email address is unique. From S06 on this is a JPA entity
 * that also holds the password hash; this module has no passwords yet.
 *
 * @param id    assigned by {@link MemberRepository#save}; {@code null} before that
 * @param email stored in lower case, so {@code Minji@Example.com} and {@code minji@example.com} are the same member
 * @param name  the member's name, without surrounding whitespace
 * @param role  what the member may do
 */
public record Member(Long id, String email, String name, Role role) {

    public Member {
        Assert.hasText(email, "email must not be blank");
        Assert.isTrue(email.contains("@"), "email must contain '@': " + email);
        Assert.hasText(name, "name must not be blank");
        Assert.notNull(role, "role must not be null");
        email = email.strip().toLowerCase(Locale.ROOT);
        name = name.strip();
    }

    /** A member that isn't saved yet. */
    public static Member create(String email, String name, Role role) {
        return new Member(null, email, name, role);
    }
}
