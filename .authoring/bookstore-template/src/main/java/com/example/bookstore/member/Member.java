package com.example.bookstore.member;

import java.util.Locale;

import com.example.bookstore.common.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

/**
 * A customer or an administrator of the bookstore. The email address is unique and identifies the member when they
 * sign in.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "members")
public class Member extends BaseTimeEntity {

    // equals() and hashCode() are inherited from Object on purpose; BaseTimeEntity explains why.

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Stored in lower case, so {@code Minji@Example.com} and {@code minji@example.com} are the same member. */
    private String email;

    private String name;

    /** The password hash, never the password itself; Spring Security's format, such as {@code {bcrypt}$2a$10$…}. */
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    private Role role;

    private Member(String email, String name, String passwordHash, Role role) {
        Assert.hasText(email, "email must not be blank");
        Assert.isTrue(email.contains("@"), () -> "email must contain '@': " + email);
        Assert.hasText(name, "name must not be blank");
        Assert.hasText(passwordHash, "passwordHash must not be blank");
        Assert.notNull(role, "role must not be null");
        this.email = email.strip().toLowerCase(Locale.ROOT);
        this.name = name.strip();
        this.passwordHash = passwordHash;
        this.role = role;
    }

    /** Creates a customer (role {@link Role#USER}). */
    public static Member register(String email, String name, String passwordHash) {
        return new Member(email, name, passwordHash, Role.USER);
    }

    /** Creates an administrator (role {@link Role#ADMIN}). */
    public static Member admin(String email, String name, String passwordHash) {
        return new Member(email, name, passwordHash, Role.ADMIN);
    }
}
