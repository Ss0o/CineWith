package com.community.board.comment.domain;

public class InvalidCommentException extends IllegalArgumentException {

    public InvalidCommentException() {
        super("댓글 내용은 비어 있을 수 없습니다.");
    }
}
