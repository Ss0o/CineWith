package com.community.board.comment.service;

import org.springframework.data.domain.Page;

import java.util.List;

/** 현재 회원이 작성한 댓글 목록의 페이지 정보다. */
public record MemberCommentPage(
        List<MemberCommentItem> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static MemberCommentPage from(Page<MemberCommentItem> comments) {
        return new MemberCommentPage(
                comments.getContent(), comments.getNumber(), comments.getSize(),
                comments.getTotalElements(), comments.getTotalPages()
        );
    }
}
