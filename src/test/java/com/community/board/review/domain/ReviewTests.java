package com.community.board.review.domain;

import com.community.board.member.domain.Member;
import com.community.board.member.domain.OAuthProvider;
import com.community.board.movie.domain.Movie;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/** Review가 평점 규칙과 부분 수정 규칙을 스스로 지키는지 검증한다. */
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
        // Given: 유효한 작성자, 영화, 리뷰 입력이 있다.
        // When: 리뷰를 생성한다.
        Review review = Review.create(member, movie, "Great movie", "My review", rating("4.5"));

        // Then: 관계, 입력값, 생성 시각이 설정되고 수정 시각은 비어 있다.
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
        // Given / When: 최소 허용 평점으로 리뷰를 생성할 때
        // Then: 평점이 그대로 보존된다.
        assertThat(createWithRating("0.5").getRating()).isEqualByComparingTo("0.5");
    }

    @Test
    void acceptsOnePointRating() {
        // Given / When: 정수 평점으로 리뷰를 생성할 때
        // Then: 유효한 0.5 단위로 받아들인다.
        assertThat(createWithRating("1.0").getRating()).isEqualByComparingTo("1.0");
    }

    @Test
    void acceptsFourAndHalfPointRating() {
        // Given / When: 4.5 평점으로 리뷰를 생성할 때
        // Then: 평점이 그대로 보존된다.
        assertThat(createWithRating("4.5").getRating()).isEqualByComparingTo("4.5");
    }

    @Test
    void acceptsFivePointRating() {
        // Given / When: 최대 허용 평점으로 리뷰를 생성할 때
        // Then: 평점이 그대로 보존된다.
        assertThat(createWithRating("5.0").getRating()).isEqualByComparingTo("5.0");
    }

    @Test
    void rejectsZeroRating() {
        // Given / When: 허용 범위 밖의 0.0으로 생성할 때
        // Then: 도메인 검증 예외가 발생한다.
        assertInvalidRating("0.0");
    }

    @Test
    void rejectsRatingBelowMinimum() {
        // Given / When: 최소값보다 작은 평점으로 생성할 때
        // Then: 도메인 검증 예외가 발생한다.
        assertInvalidRating("0.4");
    }

    @Test
    void rejectsRatingThatIsNotInHalfPointStepsNearMinimum() {
        // Given / When: 0.5 단위가 아닌 평점으로 생성할 때
        // Then: 도메인 검증 예외가 발생한다.
        assertInvalidRating("0.6");
    }

    @Test
    void rejectsRatingThatIsNotInHalfPointSteps() {
        // Given / When: 0.5 단위가 아닌 평점으로 생성할 때
        // Then: 도메인 검증 예외가 발생한다.
        assertInvalidRating("4.6");
    }

    @Test
    void rejectsRatingAboveMaximum() {
        // Given / When: 최대값보다 큰 평점으로 생성할 때
        // Then: 도메인 검증 예외가 발생한다.
        assertInvalidRating("5.1");
    }

    @Test
    void updatesEditableFieldsAndUpdatedAt() {
        // Given: 수정 전 리뷰가 있다.
        Review review = Review.create(member, movie, "Before", "Before content", rating("4.0"));

        // When: 모든 수정 가능 필드를 변경한다.
        review.update("After", "After content", rating("4.5"));

        // Then: 새 값과 수정 시각이 반영된다.
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
