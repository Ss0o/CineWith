package com.community.board.review.service;

import org.springframework.data.domain.Page;

import java.util.List;

public record ReviewFeedPage(
        List<ReviewFeedItem> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    static ReviewFeedPage from(Page<ReviewFeedItem> reviews) {
        return new ReviewFeedPage(
                reviews.getContent(),
                reviews.getNumber(),
                reviews.getSize(),
                reviews.getTotalElements(),
                reviews.getTotalPages()
        );
    }
}
