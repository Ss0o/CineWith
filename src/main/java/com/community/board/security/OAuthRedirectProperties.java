package com.community.board.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.security.oauth2.redirect")
public record OAuthRedirectProperties(
        String member,
        String signupRequired
) {
}
