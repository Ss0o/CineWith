package com.community.board.comment.repository;

import com.community.board.comment.domain.Comment;
import com.community.board.review.domain.Review;
import com.community.board.comment.service.MemberCommentItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    void deleteByReview(Review review);

    Page<Comment> findByReviewId(Long reviewId, Pageable pageable);

    @Query(value = """
            select new com.community.board.comment.service.MemberCommentItem(
                c.id, r.id, m.tmdbId, m.title, r.title, c.content, c.createdAt, c.updatedAt
            )
            from Comment c
            join c.review r
            join r.movie m
            where c.member.id = :memberId
            order by c.createdAt desc, c.id desc
            """, countQuery = "select count(c.id) from Comment c where c.member.id = :memberId")
    Page<MemberCommentItem> findActivityByMemberId(@Param("memberId") Long memberId, Pageable pageable);
}
