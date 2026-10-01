package com.community.board.comment.service;

import com.community.board.comment.domain.Comment;

import java.time.Instant;

public record CommentView(
        Long commentId,
        Long reviewId,
        String authorNickname,
        String content,
        Instant createdAt,
        Instant updatedAt,
        long recommendationCount,
        boolean recommendedByMe
) {

    public static CommentView from(Comment comment) {
        return from(comment, 0, false);
    }

    public static CommentView from(Comment comment, long recommendationCount, boolean recommendedByMe) {
        return new CommentView(
                comment.getId(),
                comment.getReview().getId(),
                comment.getMember().getNickname(),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(), recommendationCount, recommendedByMe
        );
    }
}
