package com.community.board.member.controller;

import jakarta.validation.constraints.NotNull;

public record MemberSignupRequest(
        @NotNull String nickname
) {
}
