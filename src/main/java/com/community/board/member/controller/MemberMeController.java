package com.community.board.member.controller;

import com.community.board.member.service.MemberQueryService;
import com.community.board.security.CommunityOidcPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
public class MemberMeController {

    private final MemberQueryService memberQueryService;

    public MemberMeController(MemberQueryService memberQueryService) {
        this.memberQueryService = memberQueryService;
    }

    @GetMapping("/me")
    public MemberMeResponse me(@AuthenticationPrincipal CommunityOidcPrincipal principal) {
        Long memberId = principal.getMemberId().orElseThrow();
        return MemberMeResponse.from(memberQueryService.getMember(memberId));
    }
}
