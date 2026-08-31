package com.community.board.comment.controller;

import com.community.board.comment.service.CommentService;
import com.community.board.comment.service.CommentView;
import com.community.board.security.CommunityOidcPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
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
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/reviews/{reviewId}/comments")
    public ResponseEntity<CommentResponse> create(
            @PathVariable Long reviewId,
            @Valid @RequestBody CommentCreateRequest request,
            @AuthenticationPrincipal CommunityOidcPrincipal principal
    ) {
        CommentView comment = commentService.create(memberId(principal), reviewId, request.content());
        return ResponseEntity.created(URI.create("/api/comments/" + comment.commentId()))
                .body(CommentResponse.from(comment));
    }

    @GetMapping("/reviews/{reviewId}/comments")
    public CommentPageResponse getByReview(
            @PathVariable Long reviewId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return CommentPageResponse.from(commentService.getByReview(reviewId, page, size));
    }

    @PatchMapping("/comments/{commentId}")
    public CommentResponse update(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest request,
            @AuthenticationPrincipal CommunityOidcPrincipal principal
    ) {
        return CommentResponse.from(commentService.update(memberId(principal), commentId, request.content()));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long commentId,
            @AuthenticationPrincipal CommunityOidcPrincipal principal
    ) {
        commentService.delete(memberId(principal), commentId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    private Long memberId(CommunityOidcPrincipal principal) {
        return principal.getMemberId().orElseThrow();
    }
}
