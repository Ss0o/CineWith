package com.community.board.movie.controller;

import com.community.board.movie.client.model.MovieDetail;
import com.community.board.movie.client.model.MovieSummary;

import java.time.LocalDate;

public record MovieResponse(
        Long tmdbId,
        String title,
        String posterPath,
        LocalDate releaseDate
) {

    static MovieResponse from(MovieSummary movie) {
        return new MovieResponse(movie.tmdbId(), movie.title(), movie.posterPath(), movie.releaseDate());
    }

    static MovieResponse from(MovieDetail movie) {
        return new MovieResponse(movie.tmdbId(), movie.title(), movie.posterPath(), movie.releaseDate());
    }
}
