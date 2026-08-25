package com.community.board.review.service;

import org.springframework.data.domain.Page;

import java.util.List;

public record ReviewPage(
        List<ReviewView> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    static ReviewPage from(Page<ReviewView> reviews) {
        return new ReviewPage(
                reviews.getContent(),
                reviews.getNumber(),
                reviews.getSize(),
                reviews.getTotalElements(),
                reviews.getTotalPages()
        );
    }
}
