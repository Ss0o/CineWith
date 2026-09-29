package com.community.board.movie.client.model;

import java.util.List;

public record DiscoveryMoviePage(
        List<DiscoveryMovie> movies,
        int page,
        long totalResults,
        int totalPages
) {
    public DiscoveryMoviePage {
        movies = List.copyOf(movies);
    }
}
