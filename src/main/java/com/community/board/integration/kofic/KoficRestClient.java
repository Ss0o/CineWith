package com.community.board.integration.kofic;

import com.community.board.movie.client.model.MovieDetail;
import com.community.board.movie.kofic.KoficClient;
import com.community.board.movie.kofic.KoreanTheatricalInfo;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class KoficRestClient implements KoficClient {
    private static final DateTimeFormatter KOFIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final RestClient restClient;
    private final KoficProperties properties;
    private final Clock clock;

    public KoficRestClient(RestClient restClient, KoficProperties properties) {
        this(restClient, properties, Clock.system(java.time.ZoneId.of("Asia/Seoul")));
    }

    KoficRestClient(RestClient restClient, KoficProperties properties, Clock clock) {
        this.restClient = restClient;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public KoreanTheatricalInfo getKoreanTheatricalInfo(MovieDetail movie) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            return KoreanTheatricalInfo.unavailable();
        }
        try {
            Optional<MovieListItem> matchedMovie = findMatchedMovie(movie);
            if (matchedMovie.isEmpty()) {
                return KoreanTheatricalInfo.notAvailable();
            }

            MovieInfo info = getMovieInfo(matchedMovie.get().movieCd());
            if (info == null) {
                return KoreanTheatricalInfo.notAvailable();
            }
            LocalDate today = LocalDate.now(clock);
            LocalDate asOfDate = today.minusDays(1);
            DailyBoxOfficeItem boxOffice = getDailyBoxOffice(asOfDate).stream()
                    .filter(item -> info.movieCd().equals(item.movieCd()))
                    .findFirst()
                    .orElse(null);
            LocalDate releaseDate = parseDate(info.openDt());
            Long daysSinceRelease = releaseDate == null || releaseDate.isAfter(today)
                    ? null : ChronoUnit.DAYS.between(releaseDate, today) + 1;

            return new KoreanTheatricalInfo(
                    "AVAILABLE", asOfDate, info.movieCd(), info.movieNm(), info.movieNmEn(),
                    names(info.genres()), names(info.nations()), parseInteger(info.showTm()), firstWatchGrade(info.audits()),
                    releaseDate, daysSinceRelease,
                    boxOffice == null ? null : parseInteger(boxOffice.rank()),
                    boxOffice == null ? null : parseDecimal(boxOffice.salesShare()),
                    boxOffice == null ? null : parseLong(boxOffice.audiAcc())
            );
        } catch (RestClientException | IllegalArgumentException exception) {
            return KoreanTheatricalInfo.unavailable();
        }
    }

    private Optional<MovieListItem> findMatchedMovie(MovieDetail movie) {
        MovieListResponse response = restClient.get()
                .uri(builder -> builder.path("/movie/searchMovieList.json")
                        .queryParam("key", properties.apiKey())
                        .queryParam("movieNm", movie.title())
                        .build())
                .retrieve()
                .body(MovieListResponse.class);
        if (response == null || response.movieListResult() == null || response.movieListResult().movieList() == null) {
            return Optional.empty();
        }
        List<MovieListItem> titleMatches = response.movieListResult().movieList().stream()
                .filter(candidate -> normalized(candidate.movieNm()).equals(normalized(movie.title())))
                .toList();
        if (titleMatches.size() == 1) {
            return Optional.of(titleMatches.getFirst());
        }
        List<MovieListItem> sameReleaseDate = titleMatches.stream()
                .filter(candidate -> movie.releaseDate() != null && movie.releaseDate().equals(parseDate(candidate.openDt())))
                .toList();
        if (sameReleaseDate.size() == 1) {
            return Optional.of(sameReleaseDate.getFirst());
        }
        List<MovieListItem> sameReleaseYear = titleMatches.stream()
                .filter(candidate -> movie.releaseDate() != null)
                .filter(candidate -> {
                    LocalDate candidateReleaseDate = parseDate(candidate.openDt());
                    return candidateReleaseDate != null && candidateReleaseDate.getYear() == movie.releaseDate().getYear();
                })
                .toList();
        return sameReleaseYear.size() == 1 ? Optional.of(sameReleaseYear.getFirst()) : Optional.empty();
    }

    private MovieInfo getMovieInfo(String movieCode) {
        MovieInfoResponse response = restClient.get()
                .uri(builder -> builder.path("/movie/searchMovieInfo.json")
                        .queryParam("key", properties.apiKey())
                        .queryParam("movieCd", movieCode)
                        .build())
                .retrieve()
                .body(MovieInfoResponse.class);
        return response == null || response.movieInfoResult() == null ? null : response.movieInfoResult().movieInfo();
    }

    private List<DailyBoxOfficeItem> getDailyBoxOffice(LocalDate targetDate) {
        DailyBoxOfficeResponse response = restClient.get()
                .uri(builder -> builder.path("/boxoffice/searchDailyBoxOfficeList.json")
                        .queryParam("key", properties.apiKey())
                        .queryParam("targetDt", targetDate.format(KOFIC_DATE))
                        .build())
                .retrieve()
                .body(DailyBoxOfficeResponse.class);
        if (response == null || response.boxOfficeResult() == null || response.boxOfficeResult().dailyBoxOfficeList() == null) {
            return List.of();
        }
        return response.boxOfficeResult().dailyBoxOfficeList();
    }

    private String normalized(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "").trim().toLowerCase(Locale.ROOT);
    }

    private List<String> names(List<NameItem> items) {
        return items == null ? List.of() : items.stream().map(NameItem::name).filter(value -> value != null && !value.isBlank()).toList();
    }

    private String firstWatchGrade(List<AuditItem> audits) {
        return audits == null ? null : audits.stream().map(AuditItem::watchGradeNm)
                .filter(value -> value != null && !value.isBlank()).findFirst().orElse(null);
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value, KOFIC_DATE);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private Integer parseInteger(String value) {
        try {
            return value == null || value.isBlank() ? null : Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Long parseLong(String value) {
        try {
            return value == null || value.isBlank() ? null : Long.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private BigDecimal parseDecimal(String value) {
        try {
            return value == null || value.isBlank() ? null : new BigDecimal(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    record MovieListResponse(MovieListResult movieListResult) {
    }

    record MovieListResult(List<MovieListItem> movieList) {
    }

    record MovieListItem(String movieCd, String movieNm, String openDt) {
    }

    record MovieInfoResponse(MovieInfoResult movieInfoResult) {
    }

    record MovieInfoResult(MovieInfo movieInfo) {
    }

    record MovieInfo(String movieCd, String movieNm, String movieNmEn, String showTm, String openDt,
                     List<NameItem> genres, List<NameItem> nations, List<AuditItem> audits) {
    }

    record NameItem(String genreNm, String nationNm) {
        String name() {
            return genreNm != null ? genreNm : nationNm;
        }
    }

    record AuditItem(String watchGradeNm) {
    }

    record DailyBoxOfficeResponse(DailyBoxOfficeResult boxOfficeResult) {
    }

    record DailyBoxOfficeResult(List<DailyBoxOfficeItem> dailyBoxOfficeList) {
    }

    record DailyBoxOfficeItem(String movieCd, String rank, String salesShare, String audiAcc) {
    }
}
