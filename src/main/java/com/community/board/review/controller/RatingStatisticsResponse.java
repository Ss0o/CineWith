package com.community.board.review.controller;

import com.community.board.review.service.RatingStatisticsView;

import java.math.BigDecimal;
import java.util.Map;

public record RatingStatisticsResponse(
        BigDecimal averageRating,
        long reviewCount,
        Map<String, Long> ratingDistribution
) {

    static RatingStatisticsResponse from(RatingStatisticsView statistics) {
        return new RatingStatisticsResponse(
                statistics.averageRating(), statistics.reviewCount(), statistics.ratingDistribution()
        );
    }
}
