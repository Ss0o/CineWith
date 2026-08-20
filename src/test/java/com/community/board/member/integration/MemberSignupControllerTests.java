package com.community.board.member.integration;

import com.community.board.TestcontainersConfiguration;
import com.community.board.member.domain.Member;
import com.community.board.member.domain.OAuthProvider;
import com.community.board.member.repository.MemberRepository;
import com.community.board.security.AuthenticationState;
import com.community.board.security.CommunityOidcPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class MemberSignupControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void signupRequiredUserCreatesMemberAndBecomesMemberInSession() throws Exception {
        CommunityOidcPrincipal principal = signupRequiredPrincipal(
                "signup-success-sub",
                "signup-success@example.com"
        );

        MvcResult result = performSignup(principal, "{\"nickname\":\"movieFan\"}")
                .andExpect(status().isCreated())
                .andReturn();

        Member member = memberRepository.findByProviderAndProviderId(
                OAuthProvider.GOOGLE,
                "signup-success-sub"
        ).orElseThrow();
        assertThat(member.getEmail()).isEqualTo("signup-success@example.com");
        assertThat(member.getNickname()).isEqualTo("movieFan");

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        SecurityContext context = (SecurityContext) session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
        );
        CommunityOidcPrincipal memberPrincipal = (CommunityOidcPrincipal) context
                .getAuthentication()
                .getPrincipal();
        assertThat(memberPrincipal.getAuthenticationState()).isEqualTo(AuthenticationState.MEMBER);
        assertThat(memberPrincipal.getMemberId()).contains(member.getId());
        assertThat(memberPrincipal.getAuthorities())
                .extracting("authority")
                .containsExactly(CommunityOidcPrincipal.MEMBER_AUTHORITY);
    }

    @Test
    void rejectsDuplicateNicknameWithConflict() throws Exception {
        memberRepository.saveAndFlush(
                Member.create(OAuthProvider.GOOGLE, "existing-sub", null, "duplicateNickname")
        );
        CommunityOidcPrincipal principal = signupRequiredPrincipal(
                "new-sub",
                "new@example.com"
        );

        performSignup(principal, "{\"nickname\":\"duplicateNickname\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_NICKNAME"));
    }

    @Test
    void rejectsAnonymousUserWithUnauthorized() throws Exception {
        mockMvc.perform(post("/api/members/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"movieFan\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsExistingMemberWithForbidden() throws Exception {
        Member member = memberRepository.saveAndFlush(
                Member.create(OAuthProvider.GOOGLE, "member-sub", "member@example.com", "memberNickname")
        );
        CommunityOidcPrincipal principal = memberPrincipal(member);

        performSignup(principal, "{\"nickname\":\"anotherNickname\"}")
                .andExpect(status().isForbidden());
    }

    @Test
    void ignoresUntrustedIdentityFieldsAndUsesSessionIdentity() throws Exception {
        CommunityOidcPrincipal principal = signupRequiredPrincipal(
                "trusted-sub",
                "trusted@example.com"
        );

        performSignup(principal, """
                {
                  "nickname": "trustedNickname",
                  "memberId": 999,
                  "provider": "ATTACKER",
                  "providerId": "attacker-sub",
                  "email": "attacker@example.com"
                }
                """)
                .andExpect(status().isCreated());

        Member member = memberRepository.findByProviderAndProviderId(
                OAuthProvider.GOOGLE,
                "trusted-sub"
        ).orElseThrow();
        assertThat(member.getEmail()).isEqualTo("trusted@example.com");
        assertThat(member.getNickname()).isEqualTo("trustedNickname");
        assertThat(memberRepository.findByProviderAndProviderId(
                OAuthProvider.GOOGLE,
                "attacker-sub"
        )).isEmpty();
    }

    private org.springframework.test.web.servlet.ResultActions performSignup(
            CommunityOidcPrincipal principal,
            String content
    ) throws Exception {
        OAuth2AuthenticationToken authenticationToken = new OAuth2AuthenticationToken(
                principal,
                principal.getAuthorities(),
                "google"
        );
        return mockMvc.perform(post("/api/members/signup")
                .with(authentication(authenticationToken))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(content));
    }

    private CommunityOidcPrincipal signupRequiredPrincipal(String subject, String email) {
        return CommunityOidcPrincipal.signupRequired(oidcUser(subject, email));
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
