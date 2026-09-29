package com.community.board.member.controller;

import com.community.board.comment.service.CommentService;
import com.community.board.comment.service.MemberCommentPage;
import com.community.board.review.service.ReviewFeedPage;
import com.community.board.review.service.ReviewService;
import com.community.board.security.CommunityOidcPrincipal;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/members/me")
public class MemberActivityController {

    private final ReviewService reviewService;
    private final CommentService commentService;

    public MemberActivityController(ReviewService reviewService, CommentService commentService) {
        this.reviewService = reviewService;
        this.commentService = commentService;
    }

    @GetMapping("/reviews")
    public MemberReviewPageResponse reviews(
            @AuthenticationPrincipal CommunityOidcPrincipal principal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        ReviewFeedPage reviews = reviewService.getMyFeed(memberId(principal), page, size);
        return MemberReviewPageResponse.from(reviews);
    }

    @GetMapping("/comments")
    public MemberCommentPageResponse comments(
            @AuthenticationPrincipal CommunityOidcPrincipal principal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        MemberCommentPage comments = commentService.getMyComments(memberId(principal), page, size);
        return MemberCommentPageResponse.from(comments);
    }

    private Long memberId(CommunityOidcPrincipal principal) {
        return principal.getMemberId().orElseThrow();
    }
}
