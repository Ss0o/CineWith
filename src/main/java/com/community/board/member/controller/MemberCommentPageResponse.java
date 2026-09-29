package com.community.board.member.controller;

import com.community.board.comment.service.MemberCommentItem;
import com.community.board.comment.service.MemberCommentPage;

import java.time.Instant;
import java.util.List;

public record MemberCommentPageResponse(
        List<MemberCommentItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    static MemberCommentPageResponse from(MemberCommentPage comments) {
        return new MemberCommentPageResponse(
                comments.content().stream().map(MemberCommentItemResponse::from).toList(),
                comments.page(), comments.size(), comments.totalElements(), comments.totalPages()
        );
    }

    public record MemberCommentItemResponse(
            Long commentId,
            Long reviewId,
            Long tmdbId,
            String movieTitle,
            String reviewTitle,
            String content,
            Instant createdAt,
            Instant updatedAt
    ) {
        static MemberCommentItemResponse from(MemberCommentItem comment) {
            return new MemberCommentItemResponse(
                    comment.commentId(), comment.reviewId(), comment.tmdbId(), comment.movieTitle(),
                    comment.reviewTitle(), comment.content(), comment.createdAt(), comment.updatedAt()
            );
        }
    }
}
