package com.community.board.review.service;

public class ReviewOwnershipException extends RuntimeException {

    public ReviewOwnershipException() {
        super("리뷰를 수정하거나 삭제할 권한이 없습니다.");
    }
}
