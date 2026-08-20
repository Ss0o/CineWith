package com.community.board.movie.client.model;

import java.time.LocalDate;

public record MovieSummary(
        Long tmdbId,
        String title,
        String posterPath,
        LocalDate releaseDate
) {
}
