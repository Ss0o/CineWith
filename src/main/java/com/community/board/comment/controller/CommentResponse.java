package com.community.board.comment.controller;

import com.community.board.comment.service.CommentView;

import java.time.Instant;

public record CommentResponse(
        Long commentId,
        Long reviewId,
        String authorNickname,
        String content,
        Instant createdAt,
        Instant updatedAt
) {

    static CommentResponse from(CommentView comment) {
        return new CommentResponse(
                comment.commentId(),
                comment.reviewId(),
                comment.authorNickname(),
                comment.content(),
                comment.createdAt(),
                comment.updatedAt()
        );
    }
}
