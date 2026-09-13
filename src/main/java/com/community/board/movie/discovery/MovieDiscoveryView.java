package com.community.board.movie.discovery;
import com.community.board.movie.client.model.DiscoveryMovie;
import java.time.LocalDate;
public record MovieDiscoveryView(Long tmdbId, String title, String posterPath, LocalDate releaseDate, double voteAverage) {
 static MovieDiscoveryView from(DiscoveryMovie movie) { return new MovieDiscoveryView(movie.tmdbId(), movie.title(), movie.posterPath(), movie.releaseDate(), movie.voteAverage()); }
}
