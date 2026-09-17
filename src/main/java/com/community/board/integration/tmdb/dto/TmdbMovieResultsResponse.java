package com.community.board.integration.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record TmdbMovieResultsResponse(
        List<TmdbMovieResult> results,
        Integer page,
        @JsonProperty("total_results") Long totalResults,
        @JsonProperty("total_pages") Integer totalPages
) {
}
