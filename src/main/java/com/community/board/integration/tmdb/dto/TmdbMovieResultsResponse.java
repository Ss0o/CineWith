package com.community.board.integration.tmdb.dto;

import java.util.List;

public record TmdbMovieResultsResponse(
        List<TmdbMovieResult> results
) {
}
