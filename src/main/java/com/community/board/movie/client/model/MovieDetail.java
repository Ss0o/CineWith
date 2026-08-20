package com.community.board.movie.client.model;

import java.time.LocalDate;

public record MovieDetail(
        Long tmdbId,
        String title,
        String posterPath,
        LocalDate releaseDate
) {
}
