package com.community.board.comment.controller;

import com.community.board.comment.service.CommentPage;

import java.util.List;

public record CommentPageResponse(
        List<CommentResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    static CommentPageResponse from(CommentPage comments) {
        return new CommentPageResponse(
                comments.content().stream().map(CommentResponse::from).toList(),
                comments.page(),
                comments.size(),
                comments.totalElements(),
                comments.totalPages()
        );
    }
}
