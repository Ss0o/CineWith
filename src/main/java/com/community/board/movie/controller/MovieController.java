package com.community.board.movie.controller;

import com.community.board.movie.client.MovieClient;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/movies")
public class MovieController {

    private final MovieClient movieClient;

    public MovieController(MovieClient movieClient) {
        this.movieClient = movieClient;
    }

    @GetMapping("/search")
    public List<MovieResponse> search(@RequestParam @NotBlank String query) {
        return movieClient.searchMovies(query).stream().map(MovieResponse::from).toList();
    }

    @GetMapping("/now-playing")
    public List<MovieResponse> nowPlaying() {
        return movieClient.getNowPlaying().stream().map(MovieResponse::from).toList();
    }

    @GetMapping("/{tmdbId}")
    public MovieResponse get(@PathVariable Long tmdbId) {
        return MovieResponse.from(movieClient.getMovie(tmdbId));
    }

    @GetMapping("/{tmdbId}/recommendations")
    public List<MovieResponse> recommendations(@PathVariable Long tmdbId) {
        return movieClient.getRecommendations(tmdbId).stream().map(MovieResponse::from).toList();
    }
}
