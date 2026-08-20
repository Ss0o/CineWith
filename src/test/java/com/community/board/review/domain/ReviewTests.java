package com.community.board.review.domain;

import com.community.board.member.domain.Member;
import com.community.board.member.domain.OAuthProvider;
import com.community.board.movie.domain.Movie;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ReviewTests {

    private final Member member = Member.create(
            OAuthProvider.GOOGLE,
            "domain-test-sub",
            null,
            "domainReviewer"
    );

    private final Movie movie = Movie.create(550L, "Fight Club", null, null);

    @Test
    void createsReview() {
        Review review = Review.create(member, movie, "Great movie", "My review", 8);

        assertThat(review.getMember()).isSameAs(member);
        assertThat(review.getMovie()).isSameAs(movie);
        assertThat(review.getTitle()).isEqualTo("Great movie");
        assertThat(review.getContent()).isEqualTo("My review");
        assertThat(review.getRating()).isEqualTo(8);
        assertThat(review.getCreatedAt()).isNotNull();
        assertThat(review.getUpdatedAt()).isNull();
    }

    @Test
    void acceptsMinimumRating() {
        Review review = Review.create(member, movie, "Minimum", "Minimum rating", 1);

        assertThat(review.getRating()).isEqualTo(1);
    }

    @Test
    void acceptsMaximumRating() {
        Review review = Review.create(member, movie, "Maximum", "Maximum rating", 10);

        assertThat(review.getRating()).isEqualTo(10);
    }

    @Test
    void rejectsRatingBelowMinimum() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Review.create(member, movie, "Invalid", "Too low", 0))
                .withMessage("rating must be between 1 and 10");
    }

    @Test
    void rejectsRatingAboveMaximum() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Review.create(member, movie, "Invalid", "Too high", 11))
                .withMessage("rating must be between 1 and 10");
    }
}
