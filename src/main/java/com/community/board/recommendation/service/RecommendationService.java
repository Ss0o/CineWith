package com.community.board.recommendation.service;

import com.community.board.comment.domain.Comment;
import com.community.board.comment.repository.CommentRepository;
import com.community.board.comment.service.CommentNotFoundException;
import com.community.board.member.domain.Member;
import com.community.board.member.repository.MemberRepository;
import com.community.board.member.service.MemberNotFoundException;
import com.community.board.recommendation.domain.CommentRecommendation;
import com.community.board.recommendation.domain.ReviewRecommendation;
import com.community.board.recommendation.repository.CommentRecommendationRepository;
import com.community.board.recommendation.repository.ReviewRecommendationRepository;
import com.community.board.review.domain.Review;
import com.community.board.review.repository.ReviewRepository;
import com.community.board.review.service.ReviewNotFoundException;
import com.community.board.review.service.ReviewFeedPage;
import com.community.board.comment.service.MemberCommentPage;
import org.springframework.data.domain.PageRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecommendationService {
    private final MemberRepository memberRepository;
    private final ReviewRepository reviewRepository;
    private final CommentRepository commentRepository;
    private final ReviewRecommendationRepository reviewRecommendationRepository;
    private final CommentRecommendationRepository commentRecommendationRepository;

    public RecommendationService(MemberRepository memberRepository, ReviewRepository reviewRepository,
                                 CommentRepository commentRepository, ReviewRecommendationRepository reviewRecommendationRepository,
                                 CommentRecommendationRepository commentRecommendationRepository) {
        this.memberRepository = memberRepository;
        this.reviewRepository = reviewRepository;
        this.commentRepository = commentRepository;
        this.reviewRecommendationRepository = reviewRecommendationRepository;
        this.commentRecommendationRepository = commentRecommendationRepository;
    }

    @Transactional
    public RecommendationView recommendReview(Long memberId, Long reviewId) {
        Member member = member(memberId);
        Review review = review(reviewId);
        if (reviewRecommendationRepository.existsByMemberIdAndReviewId(memberId, reviewId)) throw new DuplicateRecommendationException();
        try {
            reviewRecommendationRepository.saveAndFlush(ReviewRecommendation.create(member, review));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateRecommendationException(exception);
        }
        return new RecommendationView(reviewId, reviewRecommendationRepository.countByReviewId(reviewId));
    }

    @Transactional
    public RecommendationView recommendComment(Long memberId, Long commentId) {
        Member member = member(memberId);
        Comment comment = comment(commentId);
        if (commentRecommendationRepository.existsByMemberIdAndCommentId(memberId, commentId)) throw new DuplicateRecommendationException();
        try {
            commentRecommendationRepository.saveAndFlush(CommentRecommendation.create(member, comment));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateRecommendationException(exception);
        }
        return new RecommendationView(commentId, commentRecommendationRepository.countByCommentId(commentId));
    }

    public long reviewCount(Long reviewId) { return reviewRecommendationRepository.countByReviewId(reviewId); }
    public long commentCount(Long commentId) { return commentRecommendationRepository.countByCommentId(commentId); }
    public boolean recommendedReviewBy(Long memberId, Long reviewId) { return memberId != null && reviewRecommendationRepository.existsByMemberIdAndReviewId(memberId, reviewId); }
    public boolean recommendedCommentBy(Long memberId, Long commentId) { return memberId != null && commentRecommendationRepository.existsByMemberIdAndCommentId(memberId, commentId); }

    @Transactional(readOnly = true)
    public ReviewFeedPage getRecommendedReviews(Long memberId, int page, int size) {
        member(memberId);
        return ReviewFeedPage.from(reviewRecommendationRepository.findRecommendedByMemberId(memberId, PageRequest.of(page, size)));
    }

    @Transactional(readOnly = true)
    public MemberCommentPage getRecommendedComments(Long memberId, int page, int size) {
        member(memberId);
        return MemberCommentPage.from(commentRecommendationRepository.findRecommendedByMemberId(memberId, PageRequest.of(page, size)));
    }

    private Member member(Long id) { return memberRepository.findById(id).orElseThrow(MemberNotFoundException::new); }
    private Review review(Long id) { return reviewRepository.findById(id).orElseThrow(ReviewNotFoundException::new); }
    private Comment comment(Long id) { return commentRepository.findById(id).orElseThrow(CommentNotFoundException::new); }
}
