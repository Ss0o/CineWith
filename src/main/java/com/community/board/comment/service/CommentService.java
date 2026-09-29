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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
/**
 * 리뷰에 속한 댓글의 유스케이스를 담당한다.
 *
 * <p>쓰기 요청은 현재 회원을 다시 조회한 뒤 댓글 작성자와 비교하여 소유권을 검증한다.
 * 따라서 리뷰 작성자라고 해서 다른 회원의 댓글을 수정하거나 삭제할 수 없다.</p>
 */
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
    /** 회원과 대상 리뷰가 모두 존재할 때 댓글을 생성한다. */
    public CommentView create(Long memberId, Long reviewId, String content) {
        Member member = getMember(memberId);
        Review review = getReview(reviewId);
        return CommentView.from(commentRepository.save(Comment.create(member, review, content)));
    }

    @Transactional(readOnly = true)
    /** 존재하는 리뷰의 댓글을 작성일 오름차순으로 페이지 조회한다. */
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

    @Transactional(readOnly = true)
    /** 현재 회원이 작성한 댓글을 최신순으로 페이지 조회한다. */
    public MemberCommentPage getMyComments(Long memberId, int page, int size) {
        getMember(memberId);
        Page<MemberCommentItem> comments = commentRepository.findActivityByMemberId(
                memberId,
                PageRequest.of(page, size)
        );
        return MemberCommentPage.from(comments);
    }

    @Transactional
    /** 댓글 작성자만 내용을 변경할 수 있다. */
    public CommentView update(Long memberId, Long commentId, String content) {
        Member member = getMember(memberId);
        Comment comment = getComment(commentId);
        verifyOwner(member, comment);
        comment.update(content);
        return CommentView.from(comment);
    }

    @Transactional
    /** 댓글 작성자만 댓글을 물리적으로 삭제할 수 있다. */
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
        // Principal에서 얻은 현재 회원 ID와 영속 댓글의 작성자 ID를 비교한다.
        if (!comment.getMember().getId().equals(member.getId())) {
            throw new CommentOwnershipException();
        }
    }
}
