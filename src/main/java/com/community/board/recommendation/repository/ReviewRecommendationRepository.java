package com.community.board.recommendation.repository;

import com.community.board.recommendation.domain.ReviewRecommendation;
import com.community.board.review.domain.Review;
import com.community.board.review.service.ReviewFeedItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRecommendationRepository extends JpaRepository<ReviewRecommendation, Long> {
    boolean existsByMemberIdAndReviewId(Long memberId, Long reviewId);
    long countByReviewId(Long reviewId);

    @Modifying
    void deleteByReview(Review review);

    @Query(value = """
            select new com.community.board.review.service.ReviewFeedItem(
                r.id, m.tmdbId, m.title, m.posterPath, member.nickname, r.rating, r.title, r.content, r.createdAt
            ) from ReviewRecommendation recommendation
            join recommendation.review r join r.movie m join r.member member
            where recommendation.member.id = :memberId
            order by recommendation.createdAt desc, recommendation.id desc
            """, countQuery = "select count(recommendation.id) from ReviewRecommendation recommendation where recommendation.member.id = :memberId")
    Page<ReviewFeedItem> findRecommendedByMemberId(@Param("memberId") Long memberId, Pageable pageable);
}
