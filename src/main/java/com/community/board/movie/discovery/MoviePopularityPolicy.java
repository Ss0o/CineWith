package com.community.board.movie.discovery;

import com.community.board.movie.client.model.DiscoveryMovie;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Component
public class MoviePopularityPolicy {
    public List<DiscoveryMovie> rank(List<DiscoveryMovie> movies, boolean upcoming) {
        Comparator<LocalDate> releaseDateOrder = upcoming ? Comparator.naturalOrder() : Comparator.reverseOrder();
        return movies.stream()
                .sorted(Comparator.comparingDouble(DiscoveryMovie::popularity).reversed()
                        .thenComparing(DiscoveryMovie::releaseDate, Comparator.nullsLast(releaseDateOrder))
                        .thenComparing(DiscoveryMovie::tmdbId))
                .toList();
    }
}
