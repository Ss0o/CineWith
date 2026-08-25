package com.community.board.review.controller;

import com.community.board.review.service.ReviewView;

import java.math.BigDecimal;
import java.time.Instant;

public record ReviewResponse(
        Long reviewId,
        Long tmdbId,
        String movieTitle,
        String authorNickname,
        String title,
        String content,
        BigDecimal rating,
        Instant createdAt,
        Instant updatedAt
) {

    static ReviewResponse from(ReviewView review) {
        return new ReviewResponse(
                review.reviewId(),
                review.tmdbId(),
                review.movieTitle(),
                review.authorNickname(),
                review.title(),
                review.content(),
                review.rating(),
                review.createdAt(),
                review.updatedAt()
        );
    }
}
