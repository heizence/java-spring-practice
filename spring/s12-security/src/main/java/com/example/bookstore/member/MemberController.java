package com.example.bookstore.member;

import java.net.URI;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Members: signing up ({@code POST /api/members}) and the signed-in member ({@code GET /api/members/me}). */
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    /**
     * Signs up a new customer: {@code 201 Created}, with the new member's address in the {@code Location} header.
     * Members are read through the admin API, so that's where the address points.
     */
    @PostMapping
    public ResponseEntity<MemberResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        MemberResponse member = memberService.signUp(request);
        return ResponseEntity.created(URI.create("/api/admin/members/" + member.id())).body(member);
    }

    /**
     * The signed-in member: {@code GET /api/members/me}. The resource server has checked the access token and
     * stored it as the request's principal; {@link AuthenticationPrincipal @AuthenticationPrincipal} passes it in.
     * Its subject ({@code sub}) is the member's id. A valid token of a member who doesn't exist (anymore) gets
     * {@code 404} with code {@code MEMBER_NOT_FOUND}, like {@link MemberService#findMember}.
     */
    @GetMapping("/me")
    public MemberResponse me(@AuthenticationPrincipal Jwt jwt) {
        // TODO: S12-04
        throw new UnsupportedOperationException("TODO: S12-04");
    }
}
