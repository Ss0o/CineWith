package com.community.board.integration.tmdb.exception;

public class MovieNotFoundException extends MovieClientException {

    public MovieNotFoundException(long tmdbId) {
        super("TMDB 영화를 찾을 수 없습니다: " + tmdbId);
    }
}
