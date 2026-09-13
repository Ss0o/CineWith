package com.community.board.movie.discovery;
import java.util.List;
public record DiscoverySection(List<MovieDiscoveryView> movies, String status) {
 static DiscoverySection available(List<MovieDiscoveryView> movies) { return new DiscoverySection(movies, "AVAILABLE"); }
 static DiscoverySection unavailable() { return new DiscoverySection(List.of(), "UNAVAILABLE"); }
}
