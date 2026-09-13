package com.community.board.movie.client.model;

import java.time.LocalDate;
import java.util.List;

public record MovieDetail(
        Long tmdbId,
        String title,
        String originalTitle,
        String overview,
        String posterPath,
        String backdropPath,
        LocalDate releaseDate,
        List<String> genres,
        List<String> productionCountries,
        Integer runtimeMinutes,
        List<MovieCastMember> cast,
        List<MovieCrewMember> crew
) {

    public MovieDetail {
        genres = genres == null ? List.of() : List.copyOf(genres);
        productionCountries = productionCountries == null ? List.of() : List.copyOf(productionCountries);
        cast = cast == null ? List.of() : List.copyOf(cast);
        crew = crew == null ? List.of() : List.copyOf(crew);
    }

    public MovieDetail(Long tmdbId, String title, String posterPath, LocalDate releaseDate) {
        this(tmdbId, title, null, null, posterPath, null, releaseDate, List.of(), List.of(), null, List.of(), List.of());
    }
}
