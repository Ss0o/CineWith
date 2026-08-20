package com.community.board.integration.tmdb.exception;

public class MovieClientException extends RuntimeException {

    public MovieClientException(String message) {
        super(message);
    }

    public MovieClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
