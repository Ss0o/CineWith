package com.community.board.review.service;

import com.community.board.review.domain.Review;

import java.math.BigDecimal;
import java.time.Instant;

public record ReviewView(
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

    public static ReviewView from(Review review) {
        return new ReviewView(
                review.getId(),
                review.getMovie().getTmdbId(),
                review.getMovie().getTitle(),
                review.getMember().getNickname(),
                review.getTitle(),
                review.getContent(),
                review.getRating(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}
