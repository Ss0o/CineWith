package com.community.board.recommendation.controller;

import com.community.board.recommendation.service.RecommendationService;
import com.community.board.recommendation.service.RecommendationView;
import com.community.board.security.CommunityOidcPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api")
public class RecommendationController {
    private final RecommendationService recommendationService;
    public RecommendationController(RecommendationService recommendationService) { this.recommendationService = recommendationService; }

    @PostMapping("/reviews/{reviewId}/recommendations")
    public ResponseEntity<RecommendationResponse> recommendReview(@PathVariable Long reviewId,
            @AuthenticationPrincipal CommunityOidcPrincipal principal) {
        RecommendationView recommendation = recommendationService.recommendReview(memberId(principal), reviewId);
        return ResponseEntity.created(URI.create("/api/reviews/" + reviewId + "/recommendations"))
                .body(RecommendationResponse.from(recommendation));
    }

    @PostMapping("/comments/{commentId}/recommendations")
    public ResponseEntity<RecommendationResponse> recommendComment(@PathVariable Long commentId,
            @AuthenticationPrincipal CommunityOidcPrincipal principal) {
        RecommendationView recommendation = recommendationService.recommendComment(memberId(principal), commentId);
        return ResponseEntity.created(URI.create("/api/comments/" + commentId + "/recommendations"))
                .body(RecommendationResponse.from(recommendation));
    }

    private Long memberId(CommunityOidcPrincipal principal) { return principal.getMemberId().orElseThrow(); }
}
