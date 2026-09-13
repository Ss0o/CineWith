package com.community.board.movie.client.model;

public record MovieCastMember(
        Long personId,
        String name,
        String character,
        String profilePath
) {
}
