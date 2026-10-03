package com.example.bookstore.member;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method parameter of type {@link Member} that should receive the member who sent the request:
 *
 * <pre>{@code
 * @GetMapping("/api/members/me")
 * public MemberResponse me(@CurrentMember Member member) { ... }
 * }</pre>
 *
 * <p>{@link CurrentMemberArgumentResolver} provides the value. Spring Security's {@code @AuthenticationPrincipal}
 * (S12) works the same way.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentMember {
}
