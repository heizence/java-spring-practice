package com.example.bookstore.member;

/**
 * A member as the API shows it. It has no password hash on purpose: a hash never leaves the server, because anyone
 * who has it can try billions of guesses offline.
 */
public record MemberResponse(Long id, String email, String name, Role role) {

    static MemberResponse from(Member member) {
        return new MemberResponse(member.getId(), member.getEmail(), member.getName(), member.getRole());
    }
}
