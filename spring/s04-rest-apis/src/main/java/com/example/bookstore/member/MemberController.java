package com.example.bookstore.member;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code GET /api/members/me}: the member who sends the request. */
@RestController
public class MemberController {

    /**
     * The member named by the {@code X-Member-Id} header. {@link CurrentMemberArgumentResolver} reads the header and
     * loads the member before this method runs, so the method only maps it.
     */
    @GetMapping("/api/members/me")
    public MemberResponse me(@CurrentMember Member member) {
        return MemberResponse.from(member);
    }
}
