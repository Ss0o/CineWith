package com.community.board.comment.service;

import com.community.board.comment.domain.Comment;
import com.community.board.comment.domain.InvalidCommentException;
import com.community.board.comment.repository.CommentRepository;
import com.community.board.member.domain.Member;
import com.community.board.member.repository.MemberRepository;
import com.community.board.review.domain.Review;
import com.community.board.review.repository.ReviewRepository;
import com.community.board.review.service.ReviewNotFoundException;
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
class CommentServiceTests {

    @Mock MemberRepository memberRepository;
    @Mock ReviewRepository reviewRepository;
    @Mock CommentRepository commentRepository;
    private CommentService commentService;

    @BeforeEach
    void setUp() {
        commentService = new CommentService(memberRepository, reviewRepository, commentRepository);
    }

    @Test
    void createsCommentForCurrentMemberAndReview() {
        Member member = member(1L, "author");
        Review review = review(10L, member);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(reviewRepository.findById(10L)).thenReturn(Optional.of(review));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        commentService.create(1L, 10L, "Comment");

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(captor.capture());
        assertThat(captor.getValue().getMember()).isSameAs(member);
        assertThat(captor.getValue().getReview()).isSameAs(review);
    }

    @Test
    void rejectsMissingReviewWithoutSaving() {
        Member author = member(1L, "author");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(author));
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.create(1L, 99L, "Comment"))
                .isInstanceOf(ReviewNotFoundException.class);
        verify(commentRepository, never()).save(any());
    }

    @Test
    void updatesCommentByAuthor() {
        Member author = member(1L, "author");
        Comment comment = comment(20L, author, review(10L, author));
        prepareComment(author, comment);

        CommentView result = commentService.update(1L, 20L, "Changed");

        assertThat(result.content()).isEqualTo("Changed");
        assertThat(result.updatedAt()).isNotNull();
    }

    @Test
    void rejectsUpdateByAnotherMember() {
        Member author = member(1L, "author");
        Member other = member(2L, "other");
        Comment comment = comment(20L, author, review(10L, other));
        prepareComment(other, comment);

        assertThatThrownBy(() -> commentService.update(2L, 20L, "Changed"))
                .isInstanceOf(CommentOwnershipException.class);
    }

    @Test
    void deletesCommentByAuthor() {
        Member author = member(1L, "author");
        Comment comment = comment(20L, author, review(10L, author));
        prepareComment(author, comment);

        commentService.delete(1L, 20L);

        verify(commentRepository).delete(comment);
    }

    @Test
    void rejectsDeleteByAnotherMember() {
        Member author = member(1L, "author");
        Member other = member(2L, "other");
        Comment comment = comment(20L, author, review(10L, other));
        prepareComment(other, comment);

        assertThatThrownBy(() -> commentService.delete(2L, 20L))
                .isInstanceOf(CommentOwnershipException.class);
        verify(commentRepository, never()).delete(any());
    }

    @Test
    void reviewAuthorCannotUpdateAnotherMembersComment() {
        Member reviewAuthor = member(1L, "reviewAuthor");
        Comment comment = comment(20L, member(2L, "commentAuthor"), review(10L, reviewAuthor));
        prepareComment(reviewAuthor, comment);

        assertThatThrownBy(() -> commentService.update(1L, 20L, "Changed"))
                .isInstanceOf(CommentOwnershipException.class);
    }

    @Test
    void reviewAuthorCannotDeleteAnotherMembersComment() {
        Member reviewAuthor = member(1L, "reviewAuthor");
        Comment comment = comment(20L, member(2L, "commentAuthor"), review(10L, reviewAuthor));
        prepareComment(reviewAuthor, comment);

        assertThatThrownBy(() -> commentService.delete(1L, 20L))
                .isInstanceOf(CommentOwnershipException.class);
    }

    @Test
    void rejectsBlankContent() {
        Member author = member(1L, "author");
        Comment comment = comment(20L, author, review(10L, author));
        prepareComment(author, comment);

        assertThatThrownBy(() -> commentService.update(1L, 20L, " "))
                .isInstanceOf(InvalidCommentException.class);
    }

    @Test
    void listsCommentsUsingAscendingCreatedAtPagination() {
        Member author = member(1L, "author");
        Review review = review(10L, author);
        Comment comment = comment(20L, author, review);
        when(reviewRepository.findById(10L)).thenReturn(Optional.of(review));
        when(commentRepository.findByReviewId(any(), any()))
                .thenReturn(new PageImpl<>(List.of(comment)));

        CommentPage result = commentService.getByReview(10L, 0, 20);

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
