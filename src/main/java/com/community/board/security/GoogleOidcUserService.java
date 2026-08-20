package com.community.board.security;

import com.community.board.member.domain.Member;
import com.community.board.member.domain.OAuthProvider;
import com.community.board.member.repository.MemberRepository;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
public class GoogleOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final MemberRepository memberRepository;
    private final OidcUserService delegate;

    public GoogleOidcUserService(
            MemberRepository memberRepository,
            OidcUserService delegate
    ) {
        this.memberRepository = memberRepository;
        this.delegate = delegate;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = delegate.loadUser(userRequest);
        String providerId = Objects.requireNonNull(oidcUser.getSubject(), "Google sub must not be null");

        Optional<Member> member = memberRepository.findByProviderAndProviderId(
                OAuthProvider.GOOGLE,
                providerId
        );

        return member
                .<OidcUser>map(existingMember -> CommunityOidcPrincipal.member(oidcUser, existingMember))
                .orElseGet(() -> CommunityOidcPrincipal.signupRequired(oidcUser));
    }
}
