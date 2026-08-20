package com.community.board.security;

import com.community.board.member.domain.Member;
import com.community.board.member.domain.OAuthProvider;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.io.Serial;
import java.util.List;
import java.util.Optional;

public final class CommunityOidcPrincipal extends DefaultOidcUser {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String SIGNUP_REQUIRED_AUTHORITY = "ROLE_SIGNUP_REQUIRED";
    public static final String MEMBER_AUTHORITY = "ROLE_MEMBER";

    private final AuthenticationState authenticationState;
    private final Long memberId;
    private final OAuthProvider provider;
    private final String providerId;
    private final String email;

    private CommunityOidcPrincipal(OidcUser oidcUser, Member member) {
        super(
                List.of(new SimpleGrantedAuthority(
                        member == null ? SIGNUP_REQUIRED_AUTHORITY : MEMBER_AUTHORITY
                )),
                oidcUser.getIdToken(),
                oidcUser.getUserInfo(),
                StandardClaimNames.SUB
        );
        this.authenticationState = member == null
                ? AuthenticationState.SIGNUP_REQUIRED
                : AuthenticationState.MEMBER;
        this.memberId = member == null ? null : member.getId();
        this.provider = OAuthProvider.GOOGLE;
        this.providerId = oidcUser.getSubject();
        this.email = oidcUser.getEmail();
    }

    public static CommunityOidcPrincipal signupRequired(OidcUser oidcUser) {
        return new CommunityOidcPrincipal(oidcUser, null);
    }

    public static CommunityOidcPrincipal member(OidcUser oidcUser, Member member) {
        return new CommunityOidcPrincipal(oidcUser, member);
    }

    public AuthenticationState getAuthenticationState() {
        return authenticationState;
    }

    public Optional<Long> getMemberId() {
        return Optional.ofNullable(memberId);
    }

    public OAuthProvider getProvider() {
        return provider;
    }

    public String getProviderId() {
        return providerId;
    }

    public String getEmail() {
        return email;
    }
}
