package com.community.board.review.controller;

import com.community.board.review.service.ReviewService;
import com.community.board.review.service.ReviewView;
import com.community.board.security.CommunityOidcPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Validated
@RestController
@RequestMapping("/api")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/reviews")
    public ResponseEntity<ReviewResponse> create(
            @Valid @RequestBody ReviewCreateRequest request,
            @AuthenticationPrincipal CommunityOidcPrincipal principal
    ) {
        ReviewView review = reviewService.create(
                memberId(principal),
                request.tmdbId(),
                request.title(),
                request.content(),
                request.rating()
        );
        return ResponseEntity.created(URI.create("/api/reviews/" + review.reviewId()))
                .body(ReviewResponse.from(review));
    }

    @GetMapping("/reviews/{reviewId}")
    public ReviewResponse get(@PathVariable Long reviewId) {
        return ReviewResponse.from(reviewService.get(reviewId));
    }

    @GetMapping("/movies/{tmdbId}/reviews")
    public ReviewPageResponse getByMovie(
            @PathVariable Long tmdbId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size
    ) {
        return ReviewPageResponse.from(reviewService.getByMovie(tmdbId, page, size));
    }

    @PatchMapping("/reviews/{reviewId}")
    public ReviewResponse update(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewUpdateRequest request,
            @AuthenticationPrincipal CommunityOidcPrincipal principal
    ) {
        return ReviewResponse.from(reviewService.update(
                memberId(principal),
                reviewId,
                request.toCommand()
        ));
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal CommunityOidcPrincipal principal
    ) {
        reviewService.delete(memberId(principal), reviewId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    private Long memberId(CommunityOidcPrincipal principal) {
        return principal.getMemberId().orElseThrow();
    }
}
