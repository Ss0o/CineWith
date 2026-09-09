package com.community.board.review.integration;

import com.community.board.TestcontainersConfiguration;
import com.community.board.member.domain.Member;
import com.community.board.member.domain.OAuthProvider;
import com.community.board.member.repository.MemberRepository;
import com.community.board.movie.client.MovieClient;
import com.community.board.movie.domain.Movie;
import com.community.board.movie.repository.MovieRepository;
import com.community.board.review.repository.ReviewRepository;
import com.community.board.review.service.RatingStatisticsService;
import com.community.board.review.service.RatingStatisticsView;
import com.community.board.review.service.ReviewService;
import com.community.board.review.service.ReviewUpdateCommand;
import com.community.board.review.service.ReviewView;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.session_factory.statement_inspector=com.community.board.review.integration.RatingStatisticsTests$SqlCapture")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RatingStatisticsTests {

    private static final AtomicLong IDS = new AtomicLong(1000000);
    @Autowired RatingStatisticsService statisticsService;
    @Autowired ReviewService reviewService;
    @Autowired ReviewRepository reviewRepository;
    @Autowired MovieRepository movieRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired MockMvc mockMvc;
    @MockitoBean MovieClient movieClient;
    private final List<Long> movieIds = new ArrayList<>();
    private final List<Long> memberIds = new ArrayList<>();
    private final List<Long> reviewIds = new ArrayList<>();

    @AfterEach
    void cleanUpCommittedFixtures() {
        SqlCapture.beforeDistribution.remove();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            reviewRepository.deleteAllById(reviewIds);
            movieRepository.deleteAllById(movieIds);
            memberRepository.deleteAllById(memberIds);
        });
        SqlCapture.statements.remove();
    }

    @Test
    void returnsNullAverageAndTenZeroBucketsWithoutReviewsOrLocalMovie() throws Exception {
        Movie movie = movie();
        for (Long tmdbId : List.of(movie.getTmdbId(), IDS.incrementAndGet())) {
            RatingStatisticsView result = statisticsService.getByMovie(tmdbId);
            assertThat(result.averageRating()).isNull();
            assertThat(result.reviewCount()).isZero();
            assertThat(result.ratingDistribution()).hasSize(10);
            assertThat(result.ratingDistribution().values()).containsOnly(0L);
            mockMvc.perform(get("/api/movies/{id}/rating-statistics", tmdbId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.averageRating").value(nullValue()))
                    .andExpect(jsonPath("$.reviewCount").value(0))
                    .andExpect(jsonPath("$.ratingDistribution['0.5']").value(0))
                    .andExpect(jsonPath("$.ratingDistribution['5.0']").value(0));
        }
        verifyNoInteractions(movieClient);
    }

    @Test
    void aggregatesOneFiveStarReview() {
        Movie movie = movie();
        review(movie, "5.0");
        assertStatistics(movie, "5.00", 1, "5.0", 1);
    }

    @Test
    void aggregatesMultipleReviewsAndKeepsMoviesIndependent() throws Exception {
        Movie movie = movie();
        for (String rating : List.of("5.0", "5.0", "4.5", "4.0", "3.0")) review(movie, rating);
        Movie other = movie();
        review(other, "0.5");
        RatingStatisticsView result = statisticsService.getByMovie(movie.getTmdbId());
        assertThat(result.averageRating()).isEqualByComparingTo("4.30");
        assertThat(result.reviewCount()).isEqualTo(5);
        assertThat(result.ratingDistribution()).containsEntry("5.0", 2L)
                .containsEntry("4.5", 1L).containsEntry("4.0", 1L).containsEntry("3.0", 1L)
                .containsEntry("0.5", 0L);
        assertThat(result.ratingDistribution().values().stream().mapToLong(Long::longValue).sum()).isEqualTo(5);
        assertStatistics(other, "0.50", 1, "0.5", 1);
        mockMvc.perform(get("/api/movies/{id}/rating-statistics", movie.getTmdbId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(4.3))
                .andExpect(jsonPath("$.reviewCount").value(5))
                .andExpect(jsonPath("$.ratingDistribution['5.0']").value(2));
        verifyNoInteractions(movieClient);
    }

    @Test
    void reflectsCommittedCreationRatingUpdateAndDeletionWithoutStoredStatistics() {
        Movie movie = movie();
        ReviewView first = review(movie, "3.0");
        review(movie, "4.0");
        Long authorId = memberIds.getFirst();
        assertStatistics(movie, "3.50", 2, "3.0", 1);

        reviewService.update(authorId, first.reviewId(),
                new ReviewUpdateCommand(false, null, false, null, true, new BigDecimal("5.0")));
        assertStatistics(movie, "4.50", 2, "3.0", 0);
        assertStatistics(movie, "4.50", 2, "5.0", 1);

        reviewService.delete(authorId, first.reviewId());
        assertStatistics(movie, "4.00", 1, "5.0", 0);
        reviewService.delete(memberIds.get(1), reviewIds.get(1));
        RatingStatisticsView empty = statisticsService.getByMovie(movie.getTmdbId());
        assertThat(empty.averageRating()).isNull();
        assertThat(empty.reviewCount()).isZero();
        assertThat(empty.ratingDistribution().values()).containsOnly(0L);
    }

    @Test
    void roundsRepeatingAverageToTwoDecimalPlaces() {
        Movie movie = movie();
        for (String rating : List.of("4.0", "4.0", "5.0")) review(movie, rating);
        assertStatistics(movie, "4.33", 3, "4.0", 2);
    }

    @Test
    void returnsAllTenHalfStarBuckets() {
        Movie movie = movie();
        for (int step = 1; step <= 10; step++) review(movie, BigDecimal.valueOf(step * 5L, 1).toPlainString());
        RatingStatisticsView result = statisticsService.getByMovie(movie.getTmdbId());
        assertThat(result.averageRating()).isEqualByComparingTo("2.75");
        assertThat(result.ratingDistribution()).hasSize(10);
        assertThat(result.ratingDistribution().values()).containsOnly(1L);
    }

    @Test
    void executesOnlyTwoAggregateSelectsWithoutHydratingReviewEntities() {
        Movie movie = movie();
        review(movie, "4.5");
        SqlCapture.statements.get().clear();
        statisticsService.getByMovie(movie.getTmdbId());
        List<String> statements = SqlCapture.statements.get();
        assertThat(statements).hasSize(2);
        assertThat(statements.getFirst()).contains("avg(", "count(", "tmdb_id");
        assertThat(statements.getLast()).contains("count(", "group by", "rating");
        assertThat(statements).allSatisfy(sql -> assertThat(sql).doesNotContain("content", "nickname"));
        statements.forEach(sql -> System.out.println("RATING_STATISTICS_SQL: " + sql));
        verifyNoInteractions(movieClient);
    }

    @Test
    void keepsSummaryAndDistributionConsistentWhenAnotherTransactionCommitsBetweenQueries() {
        Movie movie = movie();
        ReviewView review = review(movie, "3.0");
        Long authorId = memberIds.getFirst();
        SqlCapture.beforeDistribution.set(() -> {
            TransactionTemplate writer = new TransactionTemplate(transactionManager);
            writer.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            writer.executeWithoutResult(status -> reviewService.update(authorId, review.reviewId(),
                    new ReviewUpdateCommand(false, null, false, null, true, new BigDecimal("5.0"))));
        });
        // The writer commits after AVG/COUNT, before GROUP BY executes on the reader's connection.
        assertStatistics(movie, "3.00", 1, "3.0", 1);
        assertStatistics(movie, "5.00", 1, "5.0", 1);
    }

    @Test
    void rejectsNonPositiveAndMalformedTmdbIds() throws Exception {
        for (String id : List.of("0", "-1", "invalid")) {
            mockMvc.perform(get("/api/movies/{id}/rating-statistics", id))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        }
        verifyNoInteractions(movieClient);
    }

    private void assertStatistics(Movie movie, String average, long total, String bucket, long count) {
        RatingStatisticsView result = statisticsService.getByMovie(movie.getTmdbId());
        assertThat(result.averageRating()).isEqualByComparingTo(average);
        assertThat(result.reviewCount()).isEqualTo(total);
        assertThat(result.ratingDistribution()).hasSize(10).containsEntry(bucket, count);
        assertThat(result.ratingDistribution().values().stream().mapToLong(Long::longValue).sum()).isEqualTo(total);
    }

    private Movie movie() {
        Movie movie = movieRepository.saveAndFlush(Movie.create(IDS.incrementAndGet(), "Statistics movie", null, null));
        movieIds.add(movie.getId());
        return movie;
    }

    private ReviewView review(Movie movie, String rating) {
        long id = IDS.incrementAndGet();
        Member member = memberRepository.saveAndFlush(Member.create(OAuthProvider.GOOGLE, "stats-" + id, null, "stats-" + id));
        memberIds.add(member.getId());
        ReviewView result = reviewService.create(member.getId(), movie.getTmdbId(), "Title", "Content", new BigDecimal(rating));
        reviewIds.add(result.reviewId());
        return result;
    }

    public static class SqlCapture implements StatementInspector {
        static final ThreadLocal<List<String>> statements = ThreadLocal.withInitial(ArrayList::new);
        static final ThreadLocal<Runnable> beforeDistribution = new ThreadLocal<>();

        @Override
        public String inspect(String sql) {
            statements.get().add(sql);
            Runnable action = beforeDistribution.get();
            if (action != null && sql.contains("group by")) {
                beforeDistribution.remove();
                action.run();
            }
            return sql;
        }
    }
}
