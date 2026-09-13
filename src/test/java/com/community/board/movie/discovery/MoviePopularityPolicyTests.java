package com.community.board.movie.discovery;

import com.community.board.movie.client.model.DiscoveryMovie;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MoviePopularityPolicyTests {
    private final MoviePopularityPolicy policy = new MoviePopularityPolicy();

    @Test
    void ordersNowPlayingCandidatesByTmdbPopularityThenNewestReleaseDate() {
        DiscoveryMovie mostPopular = movie(1L, 100, LocalDate.of(2026, 9, 1));
        DiscoveryMovie newerTie = movie(2L, 50, LocalDate.of(2026, 9, 3));
        DiscoveryMovie olderTie = movie(3L, 50, LocalDate.of(2026, 9, 2));

        assertThat(policy.rank(List.of(olderTie, newerTie, mostPopular), false))
                .extracting(DiscoveryMovie::tmdbId)
                .containsExactly(1L, 2L, 3L);
    }

    @Test
    void ordersUpcomingCandidatesByTmdbPopularityThenNearestReleaseDate() {
        DiscoveryMovie mostPopular = movie(1L, 100, LocalDate.of(2026, 10, 2));
        DiscoveryMovie laterTie = movie(2L, 50, LocalDate.of(2026, 10, 3));
        DiscoveryMovie soonerTie = movie(3L, 50, LocalDate.of(2026, 10, 1));

        assertThat(policy.rank(List.of(laterTie, mostPopular, soonerTie), true))
                .extracting(DiscoveryMovie::tmdbId)
                .containsExactly(1L, 3L, 2L);
    }

    private DiscoveryMovie movie(long id, double popularity, LocalDate releaseDate) {
        return new DiscoveryMovie(id, "movie" + id, null, releaseDate, 8, 100, popularity);
    }
}
