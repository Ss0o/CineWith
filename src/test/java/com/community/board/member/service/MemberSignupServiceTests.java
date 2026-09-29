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
/** 가입 경합으로 DB 유니크 제약이 충돌했을 때의 예외 변환을 검증한다. */
class MemberSignupServiceTests {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberSignupService memberSignupService;

    @Test
    void translatesDatabaseConstraintViolationToSignupConflict() {
        // Given: 아직 중복이 없어 보이지만 저장 시 유니크 제약이 충돌하는 가입 요청이 있다.
        CommunityOidcPrincipal principal = signupRequiredPrincipal();
        when(memberRepository.existsByNickname("movieFan")).thenReturn(false);
        when(memberRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, "signup-sub"))
                .thenReturn(Optional.empty());
        when(memberRepository.saveAndFlush(any(Member.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));

        // When / Then: 가입을 시도하면 DB 구현 예외 대신 서비스의 가입 충돌 예외를 반환한다.
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
