package com.community.board.security;

import com.community.board.member.domain.Member;
import com.community.board.member.domain.OAuthProvider;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OidcLoginSuccessHandlerTests {

    private final OidcLoginSuccessHandler handler = new OidcLoginSuccessHandler(
            new OAuthRedirectProperties(
                    "http://localhost:5173/",
                    "http://localhost:5173/signup"
            )
    );

    @Test
    void redirectsMemberToConfiguredMemberLocation() throws Exception {
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(1L);
        CommunityOidcPrincipal principal = CommunityOidcPrincipal.member(googleUser(), member);
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(
                new MockHttpServletRequest(),
                response,
                new TestingAuthenticationToken(principal, null, principal.getAuthorities())
        );

        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:5173/");
    }

    @Test
    void redirectsSignupRequiredUserToConfiguredSignupLocation() throws Exception {
        CommunityOidcPrincipal principal = CommunityOidcPrincipal.signupRequired(googleUser());
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(
                new MockHttpServletRequest(),
                response,
                new TestingAuthenticationToken(principal, null, principal.getAuthorities())
        );

        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:5173/signup");
    }

    private OidcUser googleUser() {
        Instant issuedAt = Instant.now();
        OidcIdToken idToken = new OidcIdToken(
                "id-token-value",
                issuedAt,
                issuedAt.plusSeconds(300),
                Map.of(
                        StandardClaimNames.SUB, "google-sub",
                        StandardClaimNames.EMAIL, "user@example.com"
                )
        );
        return new DefaultOidcUser(List.of(), idToken);
    }
}
