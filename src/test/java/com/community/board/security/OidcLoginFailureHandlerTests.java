package com.community.board.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class OidcLoginFailureHandlerTests {

    @Test
    void hidesAuthenticationExceptionDetails() throws Exception {
        OidcLoginFailureHandler handler = new OidcLoginFailureHandler(
                new SecurityErrorResponseWriter(new JsonMapper())
        );
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(
                new MockHttpServletRequest(),
                response,
                new BadCredentialsException("sensitive token and stack details")
        );

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(response.getContentAsString())
                .contains("OAUTH_LOGIN_FAILED")
                .contains("Google 로그인에 실패했습니다.")
                .doesNotContain("sensitive token")
                .doesNotContain("BadCredentialsException");
    }
}
