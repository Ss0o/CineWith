package com.community.board.integration.tmdb.exception;

public class MovieClientUnavailableException extends MovieClientException {

    public MovieClientUnavailableException() {
        super("TMDB 서비스가 정상 응답하지 않았습니다.");
    }
}
