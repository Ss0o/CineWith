package com.community.board.review.domain;

public class InvalidReviewUpdateException extends IllegalArgumentException {

    public InvalidReviewUpdateException() {
        super("수정할 title, content, rating 중 하나 이상의 유효한 값이 필요합니다.");
    }
}
