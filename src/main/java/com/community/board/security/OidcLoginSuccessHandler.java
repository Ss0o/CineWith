package com.community.board.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OidcLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuthRedirectProperties redirectProperties;
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    public OidcLoginSuccessHandler(OAuthRedirectProperties redirectProperties) {
        this.redirectProperties = redirectProperties;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        CommunityOidcPrincipal principal = (CommunityOidcPrincipal) authentication.getPrincipal();
        String redirectUrl = principal.getAuthenticationState() == AuthenticationState.MEMBER
                ? redirectProperties.member()
                : redirectProperties.signupRequired();
        redirectStrategy.sendRedirect(request, response, redirectUrl);
    }
}
