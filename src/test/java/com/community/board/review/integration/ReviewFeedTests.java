package com.community.board.review.integration;

import com.community.board.TestcontainersConfiguration;
import com.community.board.member.domain.Member;
import com.community.board.member.domain.OAuthProvider;
import com.community.board.member.repository.MemberRepository;
import com.community.board.movie.domain.Movie;
import com.community.board.movie.repository.MovieRepository;
import com.community.board.movie.client.MovieClient;
import com.community.board.review.domain.Review;
import com.community.board.review.repository.ReviewRepository;
import com.community.board.review.service.ReviewFeedPage;
import com.community.board.review.service.ReviewService;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.session_factory.statement_inspector=com.community.board.review.integration.ReviewFeedTests$SqlCapture")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class ReviewFeedTests {

    @Autowired ReviewService reviewService;
    @Autowired ReviewRepository reviewRepository;
    @Autowired MovieRepository movieRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired MockMvc mockMvc;
    @MockitoBean MovieClient movieClient;

    @Test
    void returnsEmptyPageWhenNoReviewsExist() {
        ReviewFeedPage page = reviewService.getFeed(null, 0, 20);

        assertThat(page.content()).isEmpty();
        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(20);
        assertThat(page.totalElements()).isZero();
        assertThat(page.totalPages()).isZero();
    }

    @Test
    void returnsFeedFieldsWithoutCallingTmdb() throws Exception {
        Movie movie = movie(550L, "Interstellar", "/interstellar.jpg");
        review(movie, "writer", "A great review", "A long but useful review body", "4.5");

        mockMvc.perform(get("/api/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].tmdbId").value(550))
                .andExpect(jsonPath("$.content[0].movieTitle").value("Interstellar"))
                .andExpect(jsonPath("$.content[0].posterPath").value("/interstellar.jpg"))
                .andExpect(jsonPath("$.content[0].authorNickname").value("feed-writer"))
                .andExpect(jsonPath("$.content[0].rating").value(4.5))
                .andExpect(jsonPath("$.content[0].contentPreview").value("A long but useful review body"));
        verifyNoInteractions(movieClient);
    }

    @Test
    void ordersNewestReviewsFirstAndUsesIdAsStableTieBreaker() {
        Movie movie = movie(550L, "Movie", null);
        Review first = review(movie, "first", "first", "body", "3.0");
        Review second = review(movie, "second", "second", "body", "4.0");
        Instant sameTime = Instant.parse("2026-01-01T00:00:00Z");
        jdbcTemplate.update("update review set created_at = ? where id in (?, ?)", Timestamp.from(sameTime), first.getId(), second.getId());
        reviewRepository.flush();

        ReviewFeedPage page = reviewService.getFeed(null, 0, 20);

        assertThat(page.content()).extracting(item -> item.reviewId())
                .containsExactly(second.getId(), first.getId());
    }

    @Test
    void paginatesMixedMovieReviewsAndKeepsNewestFirst() {
        Movie firstMovie = movie(550L, "First", null);
        Movie secondMovie = movie(551L, "Second", null);
        review(firstMovie, "one", "one", "body", "3.0");
        Review newest = review(secondMovie, "two", "two", "body", "4.0");
        review(firstMovie, "three", "three", "body", "5.0");

        ReviewFeedPage page = reviewService.getFeed(null, 1, 2);

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().getFirst().reviewId()).isNotEqualTo(newest.getId());
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(2);
    }

    @Test
    void searchesReviewTitleContentAndMovieTitleIgnoringCaseAndWhitespace() {
        Movie movie = movie(550L, "Interstellar", null);
        Review title = review(movie, "title", "Space Journey", "ordinary", "3.0");
        Review content = review(movie, "content", "ordinary", "Family Through Time", "4.0");
        Review movieTitle = review(movie, "movie", "ordinary", "ordinary", "5.0");

        assertThat(reviewService.getFeed("  space  ", 0, 20).content())
                .extracting(item -> item.reviewId()).containsExactly(title.getId());
        assertThat(reviewService.getFeed("FAMILY", 0, 20).content())
                .extracting(item -> item.reviewId()).containsExactly(content.getId());
        assertThat(reviewService.getFeed("interstellar", 0, 20).content())
                .extracting(item -> item.reviewId()).containsExactly(movieTitle.getId(), content.getId(), title.getId());
        assertThat(reviewService.getFeed("does not exist", 0, 20).content()).isEmpty();
    }

    @Test
    void treatsBlankQueryAsTheWholeFeedAndRejectsOversizedPageOrQuery() throws Exception {
        Movie movie = movie(550L, "Movie", null);
        review(movie, "writer", "title", "body", "4.0");

        assertThat(reviewService.getFeed("   ", 0, 20).totalElements()).isEqualTo(1);
        mockMvc.perform(get("/api/reviews?size=101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        mockMvc.perform(get("/api/reviews?query={query}", "x".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void executesContentAndCountQueriesOnlyWithoutPerReviewMemberOrMovieQueries() {
        Movie movie = movie(550L, "Movie", "/poster.jpg");
        for (int number = 0; number < 3; number++) {
            review(movie, "writer" + number, "title" + number, "body" + number, "4.0");
        }
        for (int pageSize : List.of(1, 2)) {
            SqlCapture.statements.get().clear();
            ReviewFeedPage result = reviewService.getFeed("title", 0, pageSize);
            assertThat(result.totalElements()).isEqualTo(3);
            List<String> statements = SqlCapture.statements.get();
            assertThat(statements).hasSize(2);
            assertThat(statements.getFirst()).contains("join member", "join movie", "order by", "fetch first");
            assertThat(statements.getLast()).contains("count(", "join movie").doesNotContain("nickname", "poster_path");
            statements.forEach(sql -> System.out.println("REVIEW_FEED_SQL: " + sql));
        }
    }

    @Test
    void searchesWithTheSameContentAndCountQueryShape() {
        Movie movie = movie(550L, "Interstellar", null);
        review(movie, "writer", "Title", "Content", "4.0");
        SqlCapture.statements.get().clear();

        reviewService.getFeed("inter", 0, 1).totalElements();

        assertThat(SqlCapture.statements.get()).hasSize(2);
        assertThat(SqlCapture.statements.get().getFirst()).contains("lower(", "like", "or");
        assertThat(SqlCapture.statements.get().getLast()).contains("lower(", "like", "or");
    }

    private Movie movie(long tmdbId, String title, String posterPath) {
        return movieRepository.saveAndFlush(Movie.create(tmdbId, title, posterPath, null));
    }

    private Review review(Movie movie, String suffix, String title, String content, String rating) {
        Member member = memberRepository.saveAndFlush(Member.create(
                OAuthProvider.GOOGLE, "feed-" + suffix, null, "feed-" + suffix
        ));
        return reviewRepository.saveAndFlush(Review.create(member, movie, title, content, new BigDecimal(rating)));
    }

    public static class SqlCapture implements StatementInspector {
        static final ThreadLocal<List<String>> statements = ThreadLocal.withInitial(ArrayList::new);

        @Override
        public String inspect(String sql) {
            statements.get().add(sql);
            return sql;
        }
    }
}
