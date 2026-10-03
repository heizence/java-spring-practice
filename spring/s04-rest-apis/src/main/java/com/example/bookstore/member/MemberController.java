package com.example.bookstore.member;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code GET /api/members/me}: the member who sends the request. */
@RestController
public class MemberController {

    /**
     * The member named by the {@code X-Member-Id} header. {@link CurrentMemberArgumentResolver} reads the header and
     * loads the member before this method runs, so the method only maps it.
     */
    @Parameter(name = CurrentMemberArgumentResolver.HEADER, in = ParameterIn.HEADER, required = true,
            description = "The id of the member who sends the request", schema = @Schema(type = "integer"))
    @GetMapping("/api/members/me")
    public MemberResponse me(@Parameter(hidden = true) @CurrentMember Member member) {
        return MemberResponse.from(member);
    }
}
