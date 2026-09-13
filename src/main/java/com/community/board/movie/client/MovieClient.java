package com.community.board.movie.client;

import com.community.board.movie.client.model.MovieDetail;
import com.community.board.movie.client.model.MovieSummary;
import com.community.board.movie.client.model.DiscoveryMovie;
import com.community.board.movie.client.model.MovieCategory;

import java.util.List;

public interface MovieClient {

    List<MovieSummary> searchMovies(String query);

    List<MovieSummary> getNowPlaying();

    MovieDetail getMovie(long tmdbId);

    List<MovieSummary> getRecommendations(long tmdbId);

    List<DiscoveryMovie> getTopRated();

    List<DiscoveryMovie> getNowPlayingDiscovery();

    List<DiscoveryMovie> getUpcoming();

    List<DiscoveryMovie> getByCategory(MovieCategory category);
}
