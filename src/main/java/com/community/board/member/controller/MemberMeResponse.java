package com.community.board.member.controller;

import com.community.board.member.domain.Member;
import com.community.board.member.domain.OAuthProvider;

import java.time.Instant;

public record MemberMeResponse(
        OAuthProvider provider,
        String email,
        String nickname,
        Instant createdAt
) {
    public static MemberMeResponse from(Member member) {
        return new MemberMeResponse(
                member.getProvider(),
                member.getEmail(),
                member.getNickname(),
                member.getCreatedAt()
        );
    }
}
