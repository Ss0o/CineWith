package com.community.board.movie.controller;

import com.community.board.movie.kofic.KoreanTheatricalInfo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record KoreanTheatricalResponse(
        String status,
        LocalDate asOfDate,
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
    static KoreanTheatricalResponse from(KoreanTheatricalInfo info) {
        return new KoreanTheatricalResponse(info.status(), info.asOfDate(), info.title(), info.titleEnglish(),
                info.genres(), info.nations(), info.runningTimeMinutes(), info.watchGrade(), info.domesticReleaseDate(),
                info.daysSinceRelease(), info.boxOfficeRank(), info.salesShare(), info.accumulatedAudience());
    }
}
