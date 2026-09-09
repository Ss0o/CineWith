package com.community.board.review.service;

import java.math.BigDecimal;
import java.util.Map;

public record RatingStatisticsView(
        BigDecimal averageRating,
        long reviewCount,
        Map<String, Long> ratingDistribution
) {
}
