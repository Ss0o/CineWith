package com.community.board.comment.domain;

import com.community.board.member.domain.Member;
import com.community.board.member.domain.OAuthProvider;
import com.community.board.movie.domain.Movie;
import com.community.board.review.domain.Review;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Comment의 생성·내용 검증·수정 규칙을 단위 검증한다. */
class CommentTests {

    private final Member member = Member.create(
            OAuthProvider.GOOGLE,
            "comment-domain-sub",
            null,
            "commentDomainMember"
    );
    private final Movie movie = Movie.create(550L, "Fight Club", null, null);
    private final Review review = Review.create(
            member,
            movie,
            "Review",
            "Review content",
            new java.math.BigDecimal("4.0")
    );

    @Test
    void createsComment() {
        // Given: 유효한 회원과 리뷰가 있다.
        // When: 내용을 가진 댓글을 생성한다.
        Comment comment = Comment.create(member, review, "Comment content");

        // Then: 소속 관계와 생성 시각이 설정된다.
        assertThat(comment.getMember()).isSameAs(member);
        assertThat(comment.getReview()).isSameAs(review);
        assertThat(comment.getContent()).isEqualTo("Comment content");
        assertThat(comment.getCreatedAt()).isNotNull();
        assertThat(comment.getUpdatedAt()).isNull();
    }

    @Test
    void rejectsMissingMember() {
        // Given / When: 작성자 없이 댓글을 생성할 때
        // Then: 필수 관계 누락을 거부한다.
        assertThatNullPointerException()
                .isThrownBy(() -> Comment.create(null, review, "Comment content"))
                .withMessage("member must not be null");
    }

    @Test
    void rejectsMissingReview() {
        // Given / When: 대상 리뷰 없이 댓글을 생성할 때
        // Then: 필수 관계 누락을 거부한다.
        assertThatNullPointerException()
                .isThrownBy(() -> Comment.create(member, null, "Comment content"))
                .withMessage("review must not be null");
    }

    @Test
    void rejectsBlankContent() {
        // Given / When: 공백뿐인 내용으로 댓글을 생성할 때
        // Then: 도메인 검증 예외가 발생한다.
        assertThatThrownBy(() -> Comment.create(member, review, "  "))
                .isInstanceOf(InvalidCommentException.class);
    }

    @Test
    void updatesContentAndUpdatedAt() {
        // Given: 기존 댓글이 있다.
        Comment comment = Comment.create(member, review, "Before");

        // When: 내용을 수정한다.
        comment.update("After");

        // Then: 내용과 수정 시각이 갱신된다.
        assertThat(comment.getContent()).isEqualTo("After");
        assertThat(comment.getUpdatedAt()).isNotNull();
    }
}
