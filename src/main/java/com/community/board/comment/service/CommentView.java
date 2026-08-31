package com.community.board.comment.service;

import com.community.board.comment.domain.Comment;

import java.time.Instant;

public record CommentView(
        Long commentId,
        Long reviewId,
        String authorNickname,
        String content,
        Instant createdAt,
        Instant updatedAt
) {

    public static CommentView from(Comment comment) {
        return new CommentView(
                comment.getId(),
                comment.getReview().getId(),
                comment.getMember().getNickname(),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
