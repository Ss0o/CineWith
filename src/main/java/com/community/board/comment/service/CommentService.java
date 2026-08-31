package com.community.board.comment.service;

import com.community.board.comment.domain.Comment;
import com.community.board.comment.repository.CommentRepository;
import com.community.board.member.domain.Member;
import com.community.board.member.repository.MemberRepository;
import com.community.board.member.service.MemberNotFoundException;
import com.community.board.review.domain.Review;
import com.community.board.review.repository.ReviewRepository;
import com.community.board.review.service.ReviewNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

    private final MemberRepository memberRepository;
    private final ReviewRepository reviewRepository;
    private final CommentRepository commentRepository;

    public CommentService(
            MemberRepository memberRepository,
            ReviewRepository reviewRepository,
            CommentRepository commentRepository
    ) {
        this.memberRepository = memberRepository;
        this.reviewRepository = reviewRepository;
        this.commentRepository = commentRepository;
    }

    @Transactional
    public CommentView create(Long memberId, Long reviewId, String content) {
        Member member = getMember(memberId);
        Review review = getReview(reviewId);
        return CommentView.from(commentRepository.save(Comment.create(member, review, content)));
    }

    @Transactional(readOnly = true)
    public CommentPage getByReview(Long reviewId, int page, int size) {
        getReview(reviewId);
        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "createdAt")
        );
        return CommentPage.from(
                commentRepository.findByReviewId(reviewId, pageRequest).map(CommentView::from)
        );
    }

    @Transactional
    public CommentView update(Long memberId, Long commentId, String content) {
        Member member = getMember(memberId);
        Comment comment = getComment(commentId);
        verifyOwner(member, comment);
        comment.update(content);
        return CommentView.from(comment);
    }

    @Transactional
    public void delete(Long memberId, Long commentId) {
        Member member = getMember(memberId);
        Comment comment = getComment(commentId);
        verifyOwner(member, comment);
        commentRepository.delete(comment);
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);
    }

    private Review getReview(Long reviewId) {
        return reviewRepository.findById(reviewId).orElseThrow(ReviewNotFoundException::new);
    }

    private Comment getComment(Long commentId) {
        return commentRepository.findById(commentId).orElseThrow(CommentNotFoundException::new);
    }

    private void verifyOwner(Member member, Comment comment) {
        if (!comment.getMember().getId().equals(member.getId())) {
            throw new CommentOwnershipException();
        }
    }
}
