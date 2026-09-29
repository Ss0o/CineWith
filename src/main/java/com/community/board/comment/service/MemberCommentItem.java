package com.community.board.comment.service;

import java.time.Instant;

/** 마이페이지 댓글 목록에 필요한 댓글과 대상 리뷰·영화의 요약 정보다. */
public record MemberCommentItem(
        Long commentId,
        Long reviewId,
        Long tmdbId,
        String movieTitle,
        String reviewTitle,
        String content,
        Instant createdAt,
        Instant updatedAt
) {
}
