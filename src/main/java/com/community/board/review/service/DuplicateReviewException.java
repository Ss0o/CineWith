package com.community.board.review.service;

public class DuplicateReviewException extends RuntimeException {

    public DuplicateReviewException() {
        super("이미 해당 영화에 리뷰를 작성했습니다.");
    }

    public DuplicateReviewException(Throwable cause) {
        super("이미 해당 영화에 리뷰를 작성했습니다.", cause);
    }
}
