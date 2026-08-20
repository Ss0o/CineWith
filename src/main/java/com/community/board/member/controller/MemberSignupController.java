package com.community.board.member.controller;

import com.community.board.member.domain.Member;
import com.community.board.member.service.MemberSignupService;
import com.community.board.security.CommunityOidcPrincipal;
import com.community.board.security.SessionAuthenticationUpdater;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
public class MemberSignupController {

    private final MemberSignupService memberSignupService;
    private final SessionAuthenticationUpdater authenticationUpdater;

    public MemberSignupController(
            MemberSignupService memberSignupService,
            SessionAuthenticationUpdater authenticationUpdater
    ) {
        this.memberSignupService = memberSignupService;
        this.authenticationUpdater = authenticationUpdater;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public void signup(
            @Valid @RequestBody MemberSignupRequest request,
            @AuthenticationPrincipal CommunityOidcPrincipal principal,
            Authentication authentication,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        Member member = memberSignupService.signup(principal, request.nickname());
        authenticationUpdater.changeToMember(
                principal,
                member,
                authentication,
                httpRequest,
                httpResponse
        );
    }
}
