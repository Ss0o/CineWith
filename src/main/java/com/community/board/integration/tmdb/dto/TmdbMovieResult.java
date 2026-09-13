package com.community.board.integration.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TmdbMovieResult(
        Long id,
        String title,
        @JsonProperty("poster_path") String posterPath,
        @JsonProperty("release_date") String releaseDate,
        @JsonProperty("vote_average") Double voteAverage,
        @JsonProperty("vote_count") Long voteCount,
        Double popularity
) {
}
