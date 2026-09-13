package com.community.board.movie.discovery;
import java.util.Map;
public record DiscoveryHome(DiscoverySection nowPlayingRecommendations, DiscoverySection recommendedMovies, DiscoverySection upcomingRecommendations, Map<String, DiscoverySection> genres) {
 public boolean hasUnavailableSections() { return nowPlayingRecommendations.status().equals("UNAVAILABLE") || recommendedMovies.status().equals("UNAVAILABLE") || upcomingRecommendations.status().equals("UNAVAILABLE") || genres.values().stream().anyMatch(section -> section.status().equals("UNAVAILABLE")); }
}
