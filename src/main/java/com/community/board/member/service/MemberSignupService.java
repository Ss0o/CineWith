package com.community.board.member.service;

import com.community.board.member.domain.Member;
import com.community.board.member.repository.MemberRepository;
import com.community.board.security.CommunityOidcPrincipal;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberSignupService {

    private final MemberRepository memberRepository;

    public MemberSignupService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional
    public Member signup(CommunityOidcPrincipal principal, String nickname) {
        if (memberRepository.existsByNickname(nickname)) {
            throw new DuplicateNicknameException();
        }
        if (memberRepository.findByProviderAndProviderId(
                principal.getProvider(),
                principal.getProviderId()
        ).isPresent()) {
            throw new MemberSignupConflictException();
        }

        Member member = Member.create(
                principal.getProvider(),
                principal.getProviderId(),
                principal.getEmail(),
                nickname
        );

        try {
            return memberRepository.saveAndFlush(member);
        } catch (DataIntegrityViolationException exception) {
            throw new MemberSignupConflictException(exception);
        }
    }
}
