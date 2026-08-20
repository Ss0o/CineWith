package com.community.board.member.repository;

import com.community.board.member.domain.Member;
import com.community.board.member.domain.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByProviderAndProviderId(OAuthProvider provider, String providerId);

    boolean existsByNickname(String nickname);
}
