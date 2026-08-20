package com.community.board.security;

import com.community.board.TestcontainersConfiguration;
import com.community.board.member.domain.Member;
import com.community.board.member.domain.OAuthProvider;
import com.community.board.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class AuthenticationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void rejectsAnonymousMemberMeRequestWithUnauthorized() throws Exception {
        mockMvc.perform(get("/api/members/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void returnsCurrentMemberInformationWithoutInternalIdentifiers() throws Exception {
        Member member = memberRepository.saveAndFlush(Member.create(
                OAuthProvider.GOOGLE,
                "member-me-sub",
                "member@example.com",
                "movieFan"
        ));
        CommunityOidcPrincipal principal = memberPrincipal(member);

        mockMvc.perform(get("/api/members/me")
                        .with(authentication(oauthAuthentication(principal))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("GOOGLE"))
                .andExpect(jsonPath("$.email").value("member@example.com"))
                .andExpect(jsonPath("$.nickname").value("movieFan"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.providerId").doesNotExist());
    }

    @Test
    void invalidatesServiceSessionOnLogout() throws Exception {
        Member member = memberRepository.saveAndFlush(Member.create(
                OAuthProvider.GOOGLE,
                "logout-sub",
                "logout@example.com",
                "logoutMember"
        ));
        MockHttpSession session = memberSession(member);

        mockMvc.perform(get("/api/members/me").session(session))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/logout").session(session).with(csrf()))
                .andExpect(status().isNoContent());

        if (!session.isInvalid()) {
            throw new AssertionError("logout must invalidate the service session");
        }
        mockMvc.perform(get("/api/members/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsSignupRequiredUserFromMemberOnlyApis() throws Exception {
        CommunityOidcPrincipal principal = CommunityOidcPrincipal.signupRequired(
                oidcUser("signup-required-sub", "signup-required@example.com")
        );

        mockMvc.perform(get("/api/members/me")
                        .with(authentication(oauthAuthentication(principal))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(post("/api/reviews")
                        .with(authentication(oauthAuthentication(principal)))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/reviews/1/comments")
                        .with(authentication(oauthAuthentication(principal)))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    private MockHttpSession memberSession(Member member) {
        OAuth2AuthenticationToken authentication = oauthAuthentication(memberPrincipal(member));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                context
        );
        return session;
    }

    private OAuth2AuthenticationToken oauthAuthentication(CommunityOidcPrincipal principal) {
        return new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "google");
    }

    private CommunityOidcPrincipal memberPrincipal(Member member) {
        return CommunityOidcPrincipal.member(
                oidcUser(member.getProviderId(), member.getEmail()),
                member
        );
    }

    private DefaultOidcUser oidcUser(String subject, String email) {
        Instant issuedAt = Instant.now();
        OidcIdToken idToken = new OidcIdToken(
                "id-token-value",
                issuedAt,
                issuedAt.plusSeconds(300),
                Map.of(
                        StandardClaimNames.SUB, subject,
                        StandardClaimNames.EMAIL, email
                )
        );
        return new DefaultOidcUser(List.of(), idToken);
    }
}
