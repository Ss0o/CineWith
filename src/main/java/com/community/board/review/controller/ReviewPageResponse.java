package com.community.board.review.controller;

import com.community.board.review.service.ReviewPage;

import java.util.List;

public record ReviewPageResponse(
        List<ReviewResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    static ReviewPageResponse from(ReviewPage reviews) {
        return new ReviewPageResponse(
                reviews.content().stream().map(ReviewResponse::from).toList(),
                reviews.page(),
                reviews.size(),
                reviews.totalElements(),
                reviews.totalPages()
        );
    }
}
