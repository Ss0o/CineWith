package com.community.board.integration.kofic;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("kofic")
public record KoficProperties(
        String baseUrl,
        String apiKey,
        Duration connectTimeout,
        Duration readTimeout
) {
}
