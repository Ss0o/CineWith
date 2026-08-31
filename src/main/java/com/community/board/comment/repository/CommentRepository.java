package com.community.board.comment.repository;

import com.community.board.comment.domain.Comment;
import com.community.board.review.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    void deleteByReview(Review review);

    Page<Comment> findByReviewId(Long reviewId, Pageable pageable);
}
