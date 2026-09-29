package com.community.board.member.controller;

import com.community.board.review.controller.ReviewFeedItemResponse;
import com.community.board.review.service.ReviewFeedPage;

import java.util.List;

public record MemberReviewPageResponse(
        List<ReviewFeedItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    static MemberReviewPageResponse from(ReviewFeedPage reviews) {
        return new MemberReviewPageResponse(
                reviews.content().stream().map(ReviewFeedItemResponse::from).toList(),
                reviews.page(), reviews.size(), reviews.totalElements(), reviews.totalPages()
        );
    }
}
