package com.community.board.member.service;

import com.community.board.member.domain.Member;
import com.community.board.member.domain.OAuthProvider;
import com.community.board.member.repository.MemberRepository;
import com.community.board.security.CommunityOidcPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberSignupServiceTests {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberSignupService memberSignupService;

    @Test
    void translatesDatabaseConstraintViolationToSignupConflict() {
        CommunityOidcPrincipal principal = signupRequiredPrincipal();
        when(memberRepository.existsByNickname("movieFan")).thenReturn(false);
        when(memberRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, "signup-sub"))
                .thenReturn(Optional.empty());
        when(memberRepository.saveAndFlush(any(Member.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));

        assertThatThrownBy(() -> memberSignupService.signup(principal, "movieFan"))
                .isInstanceOf(MemberSignupConflictException.class);
    }

    private CommunityOidcPrincipal signupRequiredPrincipal() {
        Instant issuedAt = Instant.now();
        OidcIdToken idToken = new OidcIdToken(
                "id-token-value",
                issuedAt,
                issuedAt.plusSeconds(300),
                Map.of(
                        StandardClaimNames.SUB, "signup-sub",
                        StandardClaimNames.EMAIL, "signup@example.com"
                )
        );
        return CommunityOidcPrincipal.signupRequired(new DefaultOidcUser(List.of(), idToken));
    }
}
