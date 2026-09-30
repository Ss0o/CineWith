package com.community.board.comment.service;

import com.community.board.comment.domain.Comment;
import com.community.board.comment.domain.InvalidCommentException;
import com.community.board.comment.repository.CommentRepository;
import com.community.board.member.domain.Member;
import com.community.board.member.repository.MemberRepository;
import com.community.board.review.domain.Review;
import com.community.board.review.repository.ReviewRepository;
import com.community.board.review.service.ReviewNotFoundException;
import com.community.board.recommendation.repository.CommentRecommendationRepository;
import com.community.board.recommendation.service.RecommendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
/** 댓글 서비스의 생성, 소유권, 삭제 및 페이지 정렬 흐름을 Mockito로 검증한다. */
class CommentServiceTests {

    @Mock MemberRepository memberRepository;
    @Mock ReviewRepository reviewRepository;
    @Mock CommentRepository commentRepository;
    @Mock RecommendationService recommendationService;
    @Mock CommentRecommendationRepository commentRecommendationRepository;
    private CommentService commentService;

    @BeforeEach
    void setUp() {
        commentService = new CommentService(memberRepository, reviewRepository, commentRepository, recommendationService, commentRecommendationRepository);
    }

    @Test
    void createsCommentForCurrentMemberAndReview() {
        // Given: 현재 회원과 대상 리뷰가 존재한다.
        Member member = member(1L, "author");
        Review review = review(10L, member);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(reviewRepository.findById(10L)).thenReturn(Optional.of(review));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When: 현재 회원이 댓글을 작성한다.
        commentService.create(1L, 10L, "Comment");

        // Then: 저장되는 댓글은 해당 회원과 리뷰를 참조한다.
        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(captor.capture());
        assertThat(captor.getValue().getMember()).isSameAs(member);
        assertThat(captor.getValue().getReview()).isSameAs(review);
    }

    @Test
    void rejectsMissingReviewWithoutSaving() {
        // Given: 회원은 존재하지만 대상 리뷰는 없다.
        Member author = member(1L, "author");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(author));
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        // When / Then: 작성 요청은 없음을 알리고 저장소를 호출하지 않는다.
        assertThatThrownBy(() -> commentService.create(1L, 99L, "Comment"))
                .isInstanceOf(ReviewNotFoundException.class);
        verify(commentRepository, never()).save(any());
    }

    @Test
    void updatesCommentByAuthor() {
        // Given: 작성자 본인의 댓글이 있다.
        Member author = member(1L, "author");
        Comment comment = comment(20L, author, review(10L, author));
        prepareComment(author, comment);

        // When: 작성자가 내용을 수정한다.
        CommentView result = commentService.update(1L, 20L, "Changed");

        // Then: 새 내용과 수정 시각이 반환된다.
        assertThat(result.content()).isEqualTo("Changed");
        assertThat(result.updatedAt()).isNotNull();
    }

    @Test
    void rejectsUpdateByAnotherMember() {
        // Given: 다른 회원이 작성한 댓글이 있다.
        Member author = member(1L, "author");
        Member other = member(2L, "other");
        Comment comment = comment(20L, author, review(10L, other));
        prepareComment(other, comment);

        // When / Then: 비작성자의 수정 요청은 거부된다.
        assertThatThrownBy(() -> commentService.update(2L, 20L, "Changed"))
                .isInstanceOf(CommentOwnershipException.class);
    }

    @Test
    void deletesCommentByAuthor() {
        // Given: 작성자 본인의 댓글이 있다.
        Member author = member(1L, "author");
        Comment comment = comment(20L, author, review(10L, author));
        prepareComment(author, comment);

        // When: 작성자가 삭제한다.
        commentService.delete(1L, 20L);

        // Then: 해당 댓글이 Hard Delete 된다.
        verify(commentRepository).delete(comment);
    }

    @Test
    void rejectsDeleteByAnotherMember() {
        // Given: 다른 회원이 작성한 댓글이 있다.
        Member author = member(1L, "author");
        Member other = member(2L, "other");
        Comment comment = comment(20L, author, review(10L, other));
        prepareComment(other, comment);

        // When / Then: 비작성자의 삭제 요청은 거부되고 저장소 삭제도 발생하지 않는다.
        assertThatThrownBy(() -> commentService.delete(2L, 20L))
                .isInstanceOf(CommentOwnershipException.class);
        verify(commentRepository, never()).delete(any());
    }

    @Test
    void reviewAuthorCannotUpdateAnotherMembersComment() {
        // Given: 리뷰 작성자와 댓글 작성자가 서로 다르다.
        Member reviewAuthor = member(1L, "reviewAuthor");
        Comment comment = comment(20L, member(2L, "commentAuthor"), review(10L, reviewAuthor));
        prepareComment(reviewAuthor, comment);

        // When / Then: 리뷰 작성자라도 타인의 댓글을 수정할 수 없다.
        assertThatThrownBy(() -> commentService.update(1L, 20L, "Changed"))
                .isInstanceOf(CommentOwnershipException.class);
    }

    @Test
    void reviewAuthorCannotDeleteAnotherMembersComment() {
        // Given: 리뷰 작성자와 댓글 작성자가 서로 다르다.
        Member reviewAuthor = member(1L, "reviewAuthor");
        Comment comment = comment(20L, member(2L, "commentAuthor"), review(10L, reviewAuthor));
        prepareComment(reviewAuthor, comment);

        // When / Then: 리뷰 작성자라도 타인의 댓글을 삭제할 수 없다.
        assertThatThrownBy(() -> commentService.delete(1L, 20L))
                .isInstanceOf(CommentOwnershipException.class);
    }

    @Test
    void rejectsBlankContent() {
        // Given: 작성자 본인의 댓글이 있다.
        Member author = member(1L, "author");
        Comment comment = comment(20L, author, review(10L, author));
        prepareComment(author, comment);

        // When / Then: 공백 내용으로 수정하면 도메인 검증에서 거부된다.
        assertThatThrownBy(() -> commentService.update(1L, 20L, " "))
                .isInstanceOf(InvalidCommentException.class);
    }

    @Test
    void listsCommentsUsingAscendingCreatedAtPagination() {
        // Given: 댓글이 있는 리뷰와 페이지 조회 결과가 있다.
        Member author = member(1L, "author");
        Review review = review(10L, author);
        Comment comment = comment(20L, author, review);
        when(reviewRepository.findById(10L)).thenReturn(Optional.of(review));
        when(commentRepository.findByReviewId(any(), any()))
                .thenReturn(new PageImpl<>(List.of(comment)));

        // When: 첫 페이지를 조회한다.
        CommentPage result = commentService.getByReview(10L, 0, 20);

        // Then: createdAt 오름차순의 요청과 조회 결과가 사용된다.
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(commentRepository).findByReviewId(org.mockito.ArgumentMatchers.eq(10L), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        assertThat(captor.getValue().getSort().getOrderFor("createdAt").isAscending()).isTrue();
        assertThat(result.content()).hasSize(1);
    }

    private void prepareComment(Member currentMember, Comment comment) {
        when(memberRepository.findById(currentMember.getId())).thenReturn(Optional.of(currentMember));
        when(commentRepository.findById(20L)).thenReturn(Optional.of(comment));
    }

    private Member member(Long id, String nickname) {
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(id);
        when(member.getNickname()).thenReturn(nickname);
        return member;
    }

    private Review review(Long id, Member author) {
        Review review = mock(Review.class);
        when(review.getId()).thenReturn(id);
        when(review.getMember()).thenReturn(author);
        return review;
    }

    private Comment comment(Long id, Member author, Review review) {
        return Comment.create(author, review, "Comment");
    }
}
