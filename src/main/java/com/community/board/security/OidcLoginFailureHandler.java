package com.community.board.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OidcLoginFailureHandler implements AuthenticationFailureHandler {

    private final SecurityErrorResponseWriter errorResponseWriter;

    public OidcLoginFailureHandler(SecurityErrorResponseWriter errorResponseWriter) {
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        errorResponseWriter.write(
                response,
                HttpServletResponse.SC_UNAUTHORIZED,
                "OAUTH_LOGIN_FAILED",
                "Google 로그인에 실패했습니다."
        );
    }
}
