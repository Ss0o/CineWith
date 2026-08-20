package com.community.board.integration.tmdb.exception;

public class MovieClientCommunicationException extends MovieClientException {

    public MovieClientCommunicationException(Throwable cause) {
        super("TMDB 서비스와 통신할 수 없습니다.", cause);
    }
}
