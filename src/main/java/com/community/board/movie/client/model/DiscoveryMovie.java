package com.community.board.movie.client.model;

import java.time.LocalDate;

public record DiscoveryMovie(Long tmdbId, String title, String posterPath, LocalDate releaseDate,
                             double voteAverage, long voteCount, double popularity) {
}
