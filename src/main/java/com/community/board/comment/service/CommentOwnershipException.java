package com.community.board.comment.service;

public class CommentOwnershipException extends RuntimeException {

    public CommentOwnershipException() {
        super("댓글을 수정하거나 삭제할 권한이 없습니다.");
    }
}
