package com.community.board.movie.controller;

import com.community.board.movie.client.model.MovieDetail;
import com.community.board.movie.client.model.MovieCastMember;
import com.community.board.movie.client.model.MovieCrewMember;
import com.community.board.movie.client.model.MovieSummary;

import java.time.LocalDate;
import java.util.List;

public record MovieResponse(
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

    static MovieResponse from(MovieSummary movie) {
        return new MovieResponse(movie.tmdbId(), movie.title(), null, null, movie.posterPath(), null, movie.releaseDate(),
                List.of(), List.of(), null, List.of(), List.of());
    }

    static MovieResponse from(MovieDetail movie) {
        return new MovieResponse(movie.tmdbId(), movie.title(), movie.originalTitle(), movie.overview(), movie.posterPath(),
                movie.backdropPath(), movie.releaseDate(), movie.genres(), movie.productionCountries(), movie.runtimeMinutes(),
                movie.cast(), movie.crew());
    }
}
