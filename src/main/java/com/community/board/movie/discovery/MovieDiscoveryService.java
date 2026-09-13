package com.community.board.movie.discovery;
import com.community.board.movie.client.MovieClient;
import com.community.board.movie.client.model.DiscoveryMovie;
import com.community.board.movie.client.model.MovieCategory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MovieDiscoveryService {
    private final MovieClient movieClient;
    private final MovieRankingPolicy rankingPolicy;
    private final MoviePopularityPolicy popularityPolicy;
    private final Clock clock;

    @Autowired
    public MovieDiscoveryService(
            MovieClient movieClient,
            MovieRankingPolicy rankingPolicy,
            MoviePopularityPolicy popularityPolicy
    ) {
        this(movieClient, rankingPolicy, popularityPolicy, Clock.system(java.time.ZoneId.of("Asia/Seoul")));
    }

    MovieDiscoveryService(
            MovieClient movieClient,
            MovieRankingPolicy rankingPolicy,
            MoviePopularityPolicy popularityPolicy,
            Clock clock
    ) {
        this.movieClient = movieClient;
        this.rankingPolicy = rankingPolicy;
        this.popularityPolicy = popularityPolicy;
        this.clock = clock;
    }

    @Cacheable(cacheNames = "movieDiscoveryHome", key = "'home'", unless = "#result.hasUnavailableSections()")
    public DiscoveryHome getHome() {
        DiscoverySection nowPlaying = popularitySection(
                () -> movieClient.getNowPlayingDiscovery().stream().limit(16).toList(), false);
        DiscoverySection recommended = plainSection(
                () -> rankingPolicy.rank(movieClient.getTopRated()).stream().limit(8).toList());
        LocalDate today = LocalDate.now(clock);
        DiscoverySection upcoming = popularitySection(
                () -> movieClient.getUpcoming().stream()
                        .filter(movie -> movie.releaseDate() != null && movie.releaseDate().isAfter(today))
                        .limit(16)
                        .toList(), true);
        Map<String, DiscoverySection> genres = new LinkedHashMap<>();
        for (MovieCategory category : MovieCategory.values()) {
            genres.put(category.key(), plainSection(
                    () -> movieClient.getByCategory(category).stream().limit(8).toList()));
        }
        return new DiscoveryHome(nowPlaying, recommended, upcoming, genres);
    }

    private DiscoverySection popularitySection(Source source, boolean upcoming) {
        try {
            return DiscoverySection.available(popularityPolicy.rank(source.get(), upcoming).stream()
                    .limit(8)
                    .map(MovieDiscoveryView::from)
                    .toList());
        } catch (RuntimeException exception) {
            return DiscoverySection.unavailable();
        }
    }

    private DiscoverySection plainSection(Source source) {
        try {
            return DiscoverySection.available(source.get().stream().map(MovieDiscoveryView::from).toList());
        } catch (RuntimeException exception) {
            return DiscoverySection.unavailable();
        }
    }

    @FunctionalInterface
    private interface Source {
        List<DiscoveryMovie> get();
    }
}
