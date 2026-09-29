package com.community.board.movie.discovery;

import java.util.List;

public record DiscoveryPage(
        List<MovieDiscoveryView> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
