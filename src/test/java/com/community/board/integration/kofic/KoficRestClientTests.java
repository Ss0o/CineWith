package com.community.board.integration.kofic;

import com.community.board.movie.client.model.MovieDetail;
import com.community.board.movie.kofic.KoreanTheatricalInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KoficRestClientTests {
    private MockRestServiceServer server;
    private KoficRestClient client;

    @BeforeEach
    void setUp() {
        KoficProperties properties = new KoficProperties("https://kofic.test", "fake-key", Duration.ofSeconds(1), Duration.ofSeconds(2));
        RestClient.Builder builder = RestClient.builder().baseUrl(properties.baseUrl());
        server = MockRestServiceServer.bindTo(builder).build();
        client = new KoficRestClient(builder.build(), properties,
                Clock.fixed(Instant.parse("2026-09-14T00:00:00Z"), ZoneId.of("Asia/Seoul")));
    }

    @Test
    void combinesMatchedMovieInformationWithPreviousDayBoxOffice() {
        server.expect(requestTo(containsString("/movie/searchMovieList.json?key=fake-key&movieNm=")))
                .andRespond(withSuccess("""
                        {"movieListResult":{"movieList":[
                          {"movieCd":"20265146","movieNm":"옵세션","openDt":"20260902"}
                        ]}}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://kofic.test/movie/searchMovieInfo.json?key=fake-key&movieCd=20265146"))
                .andRespond(withSuccess("""
                        {"movieInfoResult":{"movieInfo":{
                          "movieCd":"20265146","movieNm":"옵세션","movieNmEn":"Obsession","showTm":"109","openDt":"20260902",
                          "genres":[{"genreNm":"공포"},{"genreNm":"스릴러"},{"genreNm":"멜로/로맨스"}],
                          "nations":[{"nationNm":"미국"}],
                          "audits":[{"watchGradeNm":"청소년관람불가"}]
                        }}}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://kofic.test/boxoffice/searchDailyBoxOfficeList.json?key=fake-key&targetDt=20260913"))
                .andRespond(withSuccess("""
                        {"boxOfficeResult":{"dailyBoxOfficeList":[
                          {"movieCd":"20265146","rank":"6","salesShare":"3.3","audiAcc":"475000"}
                        ]}}
                        """, MediaType.APPLICATION_JSON));

        KoreanTheatricalInfo result = client.getKoreanTheatricalInfo(
                new MovieDetail(1L, "옵세션", "/poster.jpg", LocalDate.of(2026, 9, 2)));

        assertThat(result.status()).isEqualTo("AVAILABLE");
        assertThat(result.asOfDate()).isEqualTo(LocalDate.of(2026, 9, 13));
        assertThat(result.titleEnglish()).isEqualTo("Obsession");
        assertThat(result.genres()).containsExactly("공포", "스릴러", "멜로/로맨스");
        assertThat(result.nations()).containsExactly("미국");
        assertThat(result.runningTimeMinutes()).isEqualTo(109);
        assertThat(result.watchGrade()).isEqualTo("청소년관람불가");
        assertThat(result.daysSinceRelease()).isEqualTo(13);
        assertThat(result.boxOfficeRank()).isEqualTo(6);
        assertThat(result.salesShare()).isEqualByComparingTo("3.3");
        assertThat(result.accumulatedAudience()).isEqualTo(475000L);
        server.verify();
    }

    @Test
    void returnsNotAvailableWhenNoExactKoficTitleMatchExists() {
        server.expect(requestTo(containsString("/movie/searchMovieList.json?key=fake-key&movieNm=")))
                .andRespond(withSuccess("""
                        {"movieListResult":{"movieList":[
                          {"movieCd":"1","movieNm":"다른 영화","openDt":"20260902"}
                        ]}}
                        """, MediaType.APPLICATION_JSON));

        KoreanTheatricalInfo result = client.getKoreanTheatricalInfo(
                new MovieDetail(1L, "옵세션", null, LocalDate.of(2026, 9, 2)));

        assertThat(result.status()).isEqualTo("NOT_AVAILABLE");
        server.verify();
    }

    @Test
    void matchesOneSameYearCandidateWhenTheGlobalAndDomesticReleaseDatesDiffer() {
        server.expect(requestTo(containsString("/movie/searchMovieList.json?key=fake-key&movieNm=")))
                .andRespond(withSuccess("""
                        {"movieListResult":{"movieList":[
                          {"movieCd":"20265146","movieNm":"옵세션","openDt":"20260902"},
                          {"movieCd":"20201141","movieNm":"옵세션","openDt":"20190522"}
                        ]}}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://kofic.test/movie/searchMovieInfo.json?key=fake-key&movieCd=20265146"))
                .andRespond(withSuccess("""
                        {"movieInfoResult":{"movieInfo":{
                          "movieCd":"20265146","movieNm":"옵세션","movieNmEn":"Obsession","showTm":"109","openDt":"20260902",
                          "genres":[],"nations":[],"audits":[]
                        }}}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://kofic.test/boxoffice/searchDailyBoxOfficeList.json?key=fake-key&targetDt=20260913"))
                .andRespond(withSuccess("""
                        {"boxOfficeResult":{"dailyBoxOfficeList":[]}}
                        """, MediaType.APPLICATION_JSON));

        KoreanTheatricalInfo result = client.getKoreanTheatricalInfo(
                new MovieDetail(1L, "옵세션", null, LocalDate.of(2026, 5, 13)));

        assertThat(result.status()).isEqualTo("AVAILABLE");
        assertThat(result.koficMovieCode()).isEqualTo("20265146");
        server.verify();
    }

    @Test
    void returnsUnavailableWithoutAnApiKey() {
        KoficRestClient unconfiguredClient = new KoficRestClient(RestClient.create(),
                new KoficProperties("https://kofic.test", "", Duration.ofSeconds(1), Duration.ofSeconds(2)));

        assertThat(unconfiguredClient.getKoreanTheatricalInfo(new MovieDetail(1L, "옵세션", null, null)).status())
                .isEqualTo("UNAVAILABLE");
    }
}
