package com.community.board.integration.tmdb.exception;

public class MovieClientAuthenticationException extends MovieClientException {

    public MovieClientAuthenticationException() {
        super("TMDB 인증에 실패했습니다.");
    }
}
