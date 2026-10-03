package com.example.bookstore.member;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The back office's view of members: {@code GET /api/admin/members/{id}}. Like everything under {@code /api/admin},
 * it is for administrators only; the URL rules in {@code SecurityConfiguration} make sure of that.
 */
@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
public class AdminMemberController {

    private final MemberService memberService;

    @GetMapping("/{id}")
    public MemberResponse getMember(@PathVariable long id) {
        return memberService.findMember(id);
    }
}
