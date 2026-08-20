package com.community.board.security;

import com.community.board.member.domain.Member;
import com.community.board.member.domain.OAuthProvider;
import com.community.board.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleOidcUserServiceTests {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private OidcUserService delegate;

    private GoogleOidcUserService userService;

    @BeforeEach
    void setUp() {
        userService = new GoogleOidcUserService(memberRepository, delegate);
    }

    @Test
    void identifiesExistingMemberByGoogleSub() {
        OidcUser googleUser = googleUser("google-sub-1", "member@example.com");
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(42L);
        when(delegate.loadUser(any())).thenReturn(googleUser);
        when(memberRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, "google-sub-1"))
                .thenReturn(Optional.of(member));

        CommunityOidcPrincipal principal = (CommunityOidcPrincipal) userService.loadUser(
                mock(OidcUserRequest.class)
        );

        assertThat(principal.getAuthenticationState()).isEqualTo(AuthenticationState.MEMBER);
        assertThat(principal.getMemberId()).contains(42L);
        assertThat(principal.getAuthorities())
                .extracting("authority")
                .containsExactly(CommunityOidcPrincipal.MEMBER_AUTHORITY);
    }

    @Test
    void marksNewGoogleUserAsSignupRequiredWithoutCreatingMember() {
        OidcUser googleUser = googleUser("new-google-sub", "new@example.com");
        when(delegate.loadUser(any())).thenReturn(googleUser);
        when(memberRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, "new-google-sub"))
                .thenReturn(Optional.empty());

        CommunityOidcPrincipal principal = (CommunityOidcPrincipal) userService.loadUser(
                mock(OidcUserRequest.class)
        );

        assertThat(principal.getAuthenticationState()).isEqualTo(AuthenticationState.SIGNUP_REQUIRED);
        assertThat(principal.getMemberId()).isEmpty();
        assertThat(principal.getProvider()).isEqualTo(OAuthProvider.GOOGLE);
        assertThat(principal.getProviderId()).isEqualTo("new-google-sub");
        assertThat(principal.getEmail()).isEqualTo("new@example.com");
        assertThat(principal.getAuthorities())
                .extracting("authority")
                .containsExactly(CommunityOidcPrincipal.SIGNUP_REQUIRED_AUTHORITY);
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    void identifiesSameMemberWhenEmailChangesButGoogleSubDoesNot() {
        OidcUser googleUser = googleUser("stable-google-sub", "changed@example.com");
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(7L);
        when(delegate.loadUser(any())).thenReturn(googleUser);
        when(memberRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, "stable-google-sub"))
                .thenReturn(Optional.of(member));

        CommunityOidcPrincipal principal = (CommunityOidcPrincipal) userService.loadUser(
                mock(OidcUserRequest.class)
        );

        assertThat(principal.getAuthenticationState()).isEqualTo(AuthenticationState.MEMBER);
        assertThat(principal.getMemberId()).contains(7L);
        assertThat(principal.getEmail()).isEqualTo("changed@example.com");
        verify(memberRepository).findByProviderAndProviderId(
                OAuthProvider.GOOGLE,
                "stable-google-sub"
        );
    }

    private OidcUser googleUser(String subject, String email) {
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
