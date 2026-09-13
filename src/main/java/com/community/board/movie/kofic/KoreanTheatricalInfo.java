package com.community.board.movie.kofic;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record KoreanTheatricalInfo(
        String status,
        LocalDate asOfDate,
        String koficMovieCode,
        String title,
        String titleEnglish,
        List<String> genres,
        List<String> nations,
        Integer runningTimeMinutes,
        String watchGrade,
        LocalDate domesticReleaseDate,
        Long daysSinceRelease,
        Integer boxOfficeRank,
        BigDecimal salesShare,
        Long accumulatedAudience
) {
    public static KoreanTheatricalInfo notAvailable() {
        return new KoreanTheatricalInfo("NOT_AVAILABLE", null, null, null, null, List.of(), List.of(), null,
                null, null, null, null, null, null);
    }

    public static KoreanTheatricalInfo unavailable() {
        return new KoreanTheatricalInfo("UNAVAILABLE", null, null, null, null, List.of(), List.of(), null,
                null, null, null, null, null, null);
    }
}
