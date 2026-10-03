package com.example.bookstore.member;

/** A member as the API shows it. */
public record MemberResponse(long id, String email, String name, Role role) {

    static MemberResponse from(Member member) {
        return new MemberResponse(member.id(), member.email(), member.name(), member.role());
    }
}
