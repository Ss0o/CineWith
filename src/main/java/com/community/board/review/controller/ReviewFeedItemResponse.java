package com.community.board.review.controller;

import com.community.board.review.service.ReviewFeedItem;

import java.math.BigDecimal;
import java.time.Instant;

public record ReviewFeedItemResponse(
        Long reviewId,
        Long tmdbId,
        String movieTitle,
        String posterPath,
        String authorNickname,
        BigDecimal rating,
        String title,
        String contentPreview,
        Instant createdAt
) {

    static ReviewFeedItemResponse from(ReviewFeedItem review) {
        return new ReviewFeedItemResponse(
                review.reviewId(), review.tmdbId(), review.movieTitle(), review.posterPath(),
                review.authorNickname(), review.rating(), review.title(), preview(review.content()), review.createdAt()
        );
    }

    private static String preview(String content) {
        int previewLength = 200;
        return content.length() <= previewLength ? content : content.substring(0, previewLength);
    }
}
