package com.community.board.recommendation.repository;

import com.community.board.comment.domain.Comment;
import com.community.board.comment.service.MemberCommentItem;
import com.community.board.recommendation.domain.CommentRecommendation;
import com.community.board.review.domain.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRecommendationRepository extends JpaRepository<CommentRecommendation, Long> {
    boolean existsByMemberIdAndCommentId(Long memberId, Long commentId);
    long countByCommentId(Long commentId);

    @Modifying
    void deleteByComment(Comment comment);

    @Modifying
    @Query("delete from CommentRecommendation recommendation where recommendation.comment.review = :review")
    void deleteByReview(@Param("review") Review review);

    @Query(value = """
            select new com.community.board.comment.service.MemberCommentItem(
                c.id, r.id, m.tmdbId, m.title, r.title, c.content, c.createdAt, c.updatedAt
            ) from CommentRecommendation recommendation
            join recommendation.comment c join c.review r join r.movie m
            where recommendation.member.id = :memberId
            order by recommendation.createdAt desc, recommendation.id desc
            """, countQuery = "select count(recommendation.id) from CommentRecommendation recommendation where recommendation.member.id = :memberId")
    Page<MemberCommentItem> findRecommendedByMemberId(@Param("memberId") Long memberId, Pageable pageable);
}
