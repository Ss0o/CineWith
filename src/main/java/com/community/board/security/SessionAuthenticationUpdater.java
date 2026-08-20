package com.community.board.security;

import com.community.board.member.domain.Member;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

@Component
public class SessionAuthenticationUpdater {

    private final SecurityContextRepository securityContextRepository;
    private final SecurityContextHolderStrategy securityContextHolderStrategy =
            SecurityContextHolder.getContextHolderStrategy();

    public SessionAuthenticationUpdater(SecurityContextRepository securityContextRepository) {
        this.securityContextRepository = securityContextRepository;
    }

    public void changeToMember(
            CommunityOidcPrincipal signupPrincipal,
            Member member,
            Authentication currentAuthentication,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        OAuth2AuthenticationToken currentToken = (OAuth2AuthenticationToken) currentAuthentication;
        CommunityOidcPrincipal memberPrincipal = CommunityOidcPrincipal.member(signupPrincipal, member);
        OAuth2AuthenticationToken memberAuthentication = new OAuth2AuthenticationToken(
                memberPrincipal,
                memberPrincipal.getAuthorities(),
                currentToken.getAuthorizedClientRegistrationId()
        );
        memberAuthentication.setDetails(currentToken.getDetails());

        SecurityContext memberContext = securityContextHolderStrategy.createEmptyContext();
        memberContext.setAuthentication(memberAuthentication);
        securityContextHolderStrategy.setContext(memberContext);
        securityContextRepository.saveContext(memberContext, request, response);
    }
}
