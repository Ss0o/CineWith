package com.community.board.integration.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TmdbMovieResponse(
        Long id,
        String title,
        @JsonProperty("original_title") String originalTitle,
        String overview,
        @JsonProperty("poster_path") String posterPath,
        @JsonProperty("backdrop_path") String backdropPath,
        @JsonProperty("release_date") String releaseDate,
        List<TmdbNamedValue> genres,
        @JsonProperty("production_countries") List<TmdbProductionCountry> productionCountries,
        Integer runtime,
        TmdbCredits credits
) {

    public record TmdbNamedValue(Long id, String name) {
    }

    public record TmdbProductionCountry(
            @JsonProperty("iso_3166_1") String iso31661,
            String name
    ) {
    }

    public record TmdbCredits(List<TmdbCastMember> cast, List<TmdbCrewMember> crew) {
    }

    public record TmdbCastMember(
            Long id,
            String name,
            String character,
            @JsonProperty("profile_path") String profilePath
    ) {
    }

    public record TmdbCrewMember(
            Long id,
            String name,
            String job,
            String department,
            @JsonProperty("profile_path") String profilePath
    ) {
    }
}
