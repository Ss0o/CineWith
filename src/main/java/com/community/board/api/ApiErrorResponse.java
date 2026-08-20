package com.community.board.api;

public record ApiErrorResponse(
        String code,
        String message
) {
}
