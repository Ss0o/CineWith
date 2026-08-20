package com.community.board.integration.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TmdbMovieResponse(
        Long id,
        String title,
        @JsonProperty("poster_path") String posterPath,
        @JsonProperty("release_date") String releaseDate
) {
}
