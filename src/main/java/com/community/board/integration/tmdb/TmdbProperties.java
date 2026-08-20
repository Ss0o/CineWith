package com.community.board.integration.tmdb;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("tmdb")
public record TmdbProperties(
        String baseUrl,
        String accessToken,
        String language,
        String region,
        Duration connectTimeout,
        Duration readTimeout
) {
}
