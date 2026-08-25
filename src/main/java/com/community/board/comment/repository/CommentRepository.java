package com.community.board.comment.repository;

import com.community.board.comment.domain.Comment;
import com.community.board.review.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    void deleteByReview(Review review);
}
