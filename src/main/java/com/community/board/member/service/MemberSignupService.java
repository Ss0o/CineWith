package com.community.board.member.service;

import com.community.board.member.domain.Member;
import com.community.board.member.repository.MemberRepository;
import com.community.board.security.CommunityOidcPrincipal;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
/**
 * Google 인증은 끝났지만 아직 서비스 회원이 아닌 사용자의 가입을 완료한다.
 * OAuth identity는 요청 본문이 아니라 인증 Principal에서만 가져온다.
 */
public class MemberSignupService {

    private final MemberRepository memberRepository;

    public MemberSignupService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional
    /**
     * 닉네임과 OAuth identity의 중복을 확인한 뒤 회원을 저장한다.
     * 동시 요청으로 DB 제약이 위반된 경우도 동일한 가입 충돌로 변환한다.
     */
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
