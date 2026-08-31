package com.community.board.comment.service;

import org.springframework.data.domain.Page;

import java.util.List;

public record CommentPage(
        List<CommentView> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    static CommentPage from(Page<CommentView> comments) {
        return new CommentPage(
                comments.getContent(),
                comments.getNumber(),
                comments.getSize(),
                comments.getTotalElements(),
                comments.getTotalPages()
        );
    }
}
