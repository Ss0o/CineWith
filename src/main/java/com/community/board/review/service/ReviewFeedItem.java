package com.community.board.review.service;

import java.math.BigDecimal;
import java.time.Instant;

public record ReviewFeedItem(
        Long reviewId,
        Long tmdbId,
        String movieTitle,
        String posterPath,
        String authorNickname,
        BigDecimal rating,
        String title,
        String content,
        Instant createdAt
) {
}
