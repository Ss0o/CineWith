package com.community.board.member.controller;

import com.community.board.member.domain.OAuthProvider;

/** 가입이 아직 완료되지 않은 OIDC 사용자의 표시용 정보다. */
public record MemberSignupContextResponse(
        OAuthProvider provider,
        String email
) {
}
