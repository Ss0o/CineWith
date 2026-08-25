package com.community.board.review.integration;

import com.community.board.TestcontainersConfiguration;
import com.community.board.member.domain.Member;
import com.community.board.member.domain.OAuthProvider;
import com.community.board.member.repository.MemberRepository;
import com.community.board.movie.client.MovieClient;
import com.community.board.movie.client.model.MovieDetail;
import com.community.board.movie.repository.MovieRepository;
import com.community.board.review.domain.Review;
import com.community.board.review.repository.ReviewRepository;
import com.community.board.review.service.DuplicateReviewException;
import com.community.board.review.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ReviewCreationTransactionTests {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MovieRepository movieRepository;

    @MockitoSpyBean
    private ReviewRepository reviewRepository;

    @MockitoBean
    private MovieClient movieClient;

    @Test
    void rollsBackNewMovieWhenReviewInsertFails() {
        Member member = memberRepository.save(Member.create(
                OAuthProvider.GOOGLE,
                "rollback-review-sub",
                null,
                "rollbackReviewer"
        ));
        when(movieClient.getMovie(550L)).thenReturn(new MovieDetail(
                550L,
                "Fight Club",
                "/poster.jpg",
                LocalDate.of(1999, 10, 15)
        ));
        doThrow(new DataIntegrityViolationException("forced review insert failure"))
                .when(reviewRepository).saveAndFlush(any(Review.class));

        assertThatThrownBy(() -> reviewService.create(
                member.getId(),
                550L,
                "Title",
                "Content",
                new BigDecimal("4.5")
        )).isInstanceOf(DuplicateReviewException.class);

        assertThat(movieRepository.findByTmdbId(550L)).isEmpty();
        assertThat(reviewRepository.count()).isZero();
    }
}
