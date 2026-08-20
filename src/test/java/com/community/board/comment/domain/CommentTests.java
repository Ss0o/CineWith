package com.community.board.comment.domain;

import com.community.board.member.domain.Member;
import com.community.board.member.domain.OAuthProvider;
import com.community.board.movie.domain.Movie;
import com.community.board.review.domain.Review;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class CommentTests {

    private final Member member = Member.create(
            OAuthProvider.GOOGLE,
            "comment-domain-sub",
            null,
            "commentDomainMember"
    );
    private final Movie movie = Movie.create(550L, "Fight Club", null, null);
    private final Review review = Review.create(member, movie, "Review", "Review content", 8);

    @Test
    void createsComment() {
        Comment comment = Comment.create(member, review, "Comment content");

        assertThat(comment.getMember()).isSameAs(member);
        assertThat(comment.getReview()).isSameAs(review);
        assertThat(comment.getContent()).isEqualTo("Comment content");
        assertThat(comment.getCreatedAt()).isNotNull();
        assertThat(comment.getUpdatedAt()).isNull();
    }

    @Test
    void rejectsMissingMember() {
        assertThatNullPointerException()
                .isThrownBy(() -> Comment.create(null, review, "Comment content"))
                .withMessage("member must not be null");
    }

    @Test
    void rejectsMissingReview() {
        assertThatNullPointerException()
                .isThrownBy(() -> Comment.create(member, null, "Comment content"))
                .withMessage("review must not be null");
    }
}
