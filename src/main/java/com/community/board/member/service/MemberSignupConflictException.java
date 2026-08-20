package com.community.board.member.service;

public class MemberSignupConflictException extends RuntimeException {

    public MemberSignupConflictException() {
        super("이미 처리된 회원가입이거나 가입 정보가 중복되었습니다.");
    }

    public MemberSignupConflictException(Throwable cause) {
        super("이미 처리된 회원가입이거나 가입 정보가 중복되었습니다.", cause);
    }
}
