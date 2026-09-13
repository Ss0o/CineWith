package com.community.board.movie.client.model;

public record MovieCrewMember(
        Long personId,
        String name,
        String job,
        String department,
        String profilePath
) {
}
