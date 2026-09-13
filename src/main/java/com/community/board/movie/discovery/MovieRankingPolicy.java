package com.community.board.movie.discovery;
import com.community.board.movie.client.model.DiscoveryMovie;
import org.springframework.stereotype.Component;
import java.util.Comparator;
import java.util.List;

@Component
public class MovieRankingPolicy {
    private static final long MINIMUM_VOTE_COUNT = 100;
    private static final double RATING_WEIGHT = .55, VOTE_COUNT_WEIGHT = .30, POPULARITY_WEIGHT = .15;
    public List<DiscoveryMovie> rank(List<DiscoveryMovie> movies) {
        List<DiscoveryMovie> eligible = movies.stream().filter(movie -> movie.voteCount() >= MINIMUM_VOTE_COUNT).distinct().toList();
        double maxVotes = eligible.stream().mapToDouble(movie -> Math.log1p(movie.voteCount())).max().orElse(1);
        double maxPopularity = eligible.stream().mapToDouble(DiscoveryMovie::popularity).max().orElse(1);
        return eligible.stream().sorted(Comparator.comparingDouble((DiscoveryMovie movie) ->
                RATING_WEIGHT * movie.voteAverage() / 10 + VOTE_COUNT_WEIGHT * Math.log1p(movie.voteCount()) / maxVotes + POPULARITY_WEIGHT * movie.popularity() / maxPopularity).reversed()
                .thenComparing(DiscoveryMovie::tmdbId)).toList();
    }
}
