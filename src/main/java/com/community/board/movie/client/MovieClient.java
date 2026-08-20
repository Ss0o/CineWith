package com.community.board.movie.client;

import com.community.board.movie.client.model.MovieDetail;
import com.community.board.movie.client.model.MovieSummary;

import java.util.List;

public interface MovieClient {

    List<MovieSummary> searchMovies(String query);

    MovieDetail getMovie(long tmdbId);

    List<MovieSummary> getRecommendations(long tmdbId);
}
