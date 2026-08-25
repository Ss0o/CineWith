package com.community.board.review.domain;

public class InvalidReviewRatingException extends IllegalArgumentException {

    public InvalidReviewRatingException() {
        super("평점은 0.5부터 5.0까지 0.5 단위여야 합니다.");
    }
}
