package com.community.board.review.domain;

import com.community.board.member.domain.Member;
import com.community.board.member.domain.OAuthProvider;
import com.community.board.movie.domain.Movie;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

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
        Review review = Review.create(member, movie, "Great movie", "My review", rating("4.5"));

        assertThat(review.getMember()).isSameAs(member);
        assertThat(review.getMovie()).isSameAs(movie);
        assertThat(review.getTitle()).isEqualTo("Great movie");
        assertThat(review.getContent()).isEqualTo("My review");
        assertThat(review.getRating()).isEqualByComparingTo("4.5");
        assertThat(review.getCreatedAt()).isNotNull();
        assertThat(review.getUpdatedAt()).isNull();
    }

    @Test
    void acceptsHalfPointRating() {
        assertThat(createWithRating("0.5").getRating()).isEqualByComparingTo("0.5");
    }

    @Test
    void acceptsOnePointRating() {
        assertThat(createWithRating("1.0").getRating()).isEqualByComparingTo("1.0");
    }

    @Test
    void acceptsFourAndHalfPointRating() {
        assertThat(createWithRating("4.5").getRating()).isEqualByComparingTo("4.5");
    }

    @Test
    void acceptsFivePointRating() {
        assertThat(createWithRating("5.0").getRating()).isEqualByComparingTo("5.0");
    }

    @Test
    void rejectsZeroRating() {
        assertInvalidRating("0.0");
    }

    @Test
    void rejectsRatingBelowMinimum() {
        assertInvalidRating("0.4");
    }

    @Test
    void rejectsRatingThatIsNotInHalfPointStepsNearMinimum() {
        assertInvalidRating("0.6");
    }

    @Test
    void rejectsRatingThatIsNotInHalfPointSteps() {
        assertInvalidRating("4.6");
    }

    @Test
    void rejectsRatingAboveMaximum() {
        assertInvalidRating("5.1");
    }

    @Test
    void updatesEditableFieldsAndUpdatedAt() {
        Review review = Review.create(member, movie, "Before", "Before content", rating("4.0"));

        review.update("After", "After content", rating("4.5"));

        assertThat(review.getTitle()).isEqualTo("After");
        assertThat(review.getContent()).isEqualTo("After content");
        assertThat(review.getRating()).isEqualByComparingTo("4.5");
        assertThat(review.getUpdatedAt()).isNotNull();
    }

    private Review createWithRating(String rating) {
        return Review.create(member, movie, "Title", "Content", rating(rating));
    }

    private void assertInvalidRating(String rating) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> createWithRating(rating))
                .withMessage("평점은 0.5부터 5.0까지 0.5 단위여야 합니다.");
    }

    private BigDecimal rating(String value) {
        return new BigDecimal(value);
    }
}
