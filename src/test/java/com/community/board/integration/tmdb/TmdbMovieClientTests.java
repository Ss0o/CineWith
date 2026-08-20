package com.community.board.integration.tmdb;

import com.community.board.integration.tmdb.exception.MovieClientAuthenticationException;
import com.community.board.integration.tmdb.exception.MovieClientUnavailableException;
import com.community.board.integration.tmdb.exception.MovieNotFoundException;
import com.community.board.movie.client.model.MovieDetail;
import com.community.board.movie.client.model.MovieSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TmdbMovieClientTests {

    private MockRestServiceServer server;
    private TmdbMovieClient movieClient;

    @BeforeEach
    void setUp() {
        TmdbProperties properties = new TmdbProperties(
                "https://tmdb.test",
                "fake-test-token",
                "ko-KR",
                "KR",
                Duration.ofSeconds(1),
                Duration.ofSeconds(2)
        );
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.accessToken())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        server = MockRestServiceServer.bindTo(builder).build();
        movieClient = new TmdbMovieClient(builder.build(), properties);
    }

    @Test
    void mapsSearchResponseAndSendsBearerAuthorization() {
        server.expect(requestTo(
                        "https://tmdb.test/3/search/movie?query=Fight%20Club&language=ko-KR&region=KR"
                ))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer fake-test-token"))
                .andRespond(withSuccess("""
                        {
                          "results": [
                            {
                              "id": 550,
                              "title": "Fight Club",
                              "poster_path": "/poster.jpg",
                              "release_date": "1999-10-15"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<MovieSummary> result = movieClient.searchMovies("Fight Club");

        assertThat(result).containsExactly(new MovieSummary(
                550L,
                "Fight Club",
                "/poster.jpg",
                LocalDate.of(1999, 10, 15)
        ));
        server.verify();
    }

    @Test
    void mapsMovieDetailResponse() {
        server.expect(requestTo("https://tmdb.test/3/movie/550?language=ko-KR"))
                .andRespond(withSuccess("""
                        {
                          "id": 550,
                          "title": "Fight Club",
                          "poster_path": "/poster.jpg",
                          "release_date": "1999-10-15"
                        }
                        """, MediaType.APPLICATION_JSON));

        MovieDetail result = movieClient.getMovie(550L);

        assertThat(result).isEqualTo(new MovieDetail(
                550L,
                "Fight Club",
                "/poster.jpg",
                LocalDate.of(1999, 10, 15)
        ));
        server.verify();
    }

    @Test
    void mapsRecommendationResponse() {
        server.expect(requestTo("https://tmdb.test/3/movie/550/recommendations?language=ko-KR"))
                .andRespond(withSuccess("""
                        {
                          "results": [
                            {
                              "id": 680,
                              "title": "Pulp Fiction",
                              "poster_path": "/pulp.jpg",
                              "release_date": "1994-09-10"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<MovieSummary> result = movieClient.getRecommendations(550L);

        assertThat(result).containsExactly(new MovieSummary(
                680L,
                "Pulp Fiction",
                "/pulp.jpg",
                LocalDate.of(1994, 9, 10)
        ));
        server.verify();
    }

    @Test
    void mapsEmptyReleaseDateToNull() {
        server.expect(requestTo("https://tmdb.test/3/movie/1?language=ko-KR"))
                .andRespond(withSuccess("""
                        {
                          "id": 1,
                          "title": "Undated Movie",
                          "poster_path": null,
                          "release_date": ""
                        }
                        """, MediaType.APPLICATION_JSON));

        MovieDetail result = movieClient.getMovie(1L);

        assertThat(result.releaseDate()).isNull();
        server.verify();
    }

    @Test
    void convertsNotFoundResponse() {
        server.expect(requestTo("https://tmdb.test/3/movie/999?language=ko-KR"))
                .andRespond(withResourceNotFound());

        assertThatThrownBy(() -> movieClient.getMovie(999L))
                .isInstanceOf(MovieNotFoundException.class);
        server.verify();
    }

    @Test
    void convertsAuthenticationFailure() {
        server.expect(requestTo("https://tmdb.test/3/movie/550?language=ko-KR"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> movieClient.getMovie(550L))
                .isInstanceOf(MovieClientAuthenticationException.class);
        server.verify();
    }

    @Test
    void convertsServerError() {
        server.expect(requestTo("https://tmdb.test/3/movie/550?language=ko-KR"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> movieClient.getMovie(550L))
                .isInstanceOf(MovieClientUnavailableException.class);
        server.verify();
    }
}
