package com.community.board.recommendation.service;

import com.community.board.comment.domain.Comment;
import com.community.board.comment.repository.CommentRepository;
import com.community.board.member.domain.Member;
import com.community.board.member.repository.MemberRepository;
import com.community.board.movie.domain.Movie;
import com.community.board.recommendation.domain.CommentRecommendation;
import com.community.board.recommendation.domain.ReviewRecommendation;
import com.community.board.recommendation.repository.CommentRecommendationRepository;
import com.community.board.recommendation.repository.ReviewRecommendationRepository;
import com.community.board.review.domain.Review;
import com.community.board.review.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTests {
    @Mock MemberRepository memberRepository;
    @Mock ReviewRepository reviewRepository;
    @Mock CommentRepository commentRepository;
    @Mock ReviewRecommendationRepository reviewRecommendationRepository;
    @Mock CommentRecommendationRepository commentRecommendationRepository;
    private RecommendationService service;

    @BeforeEach
    void setUp() {
        service = new RecommendationService(memberRepository, reviewRepository, commentRepository,
                reviewRecommendationRepository, commentRecommendationRepository);
    }

    @Test
    void recommendsReviewOnceAndReturnsNewCount() {
        Member member = org.mockito.Mockito.mock(Member.class);
        Review review = org.mockito.Mockito.mock(Review.class);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(reviewRepository.findById(2L)).thenReturn(Optional.of(review));
        when(reviewRecommendationRepository.countByReviewId(2L)).thenReturn(1L);

        RecommendationView result = service.recommendReview(1L, 2L);

        verify(reviewRecommendationRepository).saveAndFlush(any(ReviewRecommendation.class));
        assertThat(result.targetId()).isEqualTo(2L);
        assertThat(result.recommendationCount()).isEqualTo(1L);
    }

    @Test
    void rejectsDuplicateCommentRecommendationWithoutSaving() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(org.mockito.Mockito.mock(Member.class)));
        when(commentRepository.findById(2L)).thenReturn(Optional.of(org.mockito.Mockito.mock(Comment.class)));
        when(commentRecommendationRepository.existsByMemberIdAndCommentId(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> service.recommendComment(1L, 2L)).isInstanceOf(DuplicateRecommendationException.class);

        verify(commentRecommendationRepository, never()).saveAndFlush(any(CommentRecommendation.class));
    }
}
