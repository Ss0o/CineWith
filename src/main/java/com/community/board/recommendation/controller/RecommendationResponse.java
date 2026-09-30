package com.community.board.recommendation.controller;

import com.community.board.recommendation.service.RecommendationView;

public record RecommendationResponse(Long targetId, long recommendationCount, boolean recommendedByMe) {
    public static RecommendationResponse from(RecommendationView recommendation) {
        return new RecommendationResponse(recommendation.targetId(), recommendation.recommendationCount(), true);
    }
}
