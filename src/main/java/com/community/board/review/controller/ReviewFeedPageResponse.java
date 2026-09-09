package com.community.board.review.controller;

import com.community.board.review.service.ReviewFeedPage;

import java.util.List;

public record ReviewFeedPageResponse(
        List<ReviewFeedItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    static ReviewFeedPageResponse from(ReviewFeedPage reviews) {
        return new ReviewFeedPageResponse(
                reviews.content().stream().map(ReviewFeedItemResponse::from).toList(),
                reviews.page(), reviews.size(), reviews.totalElements(), reviews.totalPages()
        );
    }
}
