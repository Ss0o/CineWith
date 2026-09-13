package com.community.board.movie.integration;

import com.community.board.TestcontainersConfiguration;
import com.community.board.movie.client.MovieClient;
import com.community.board.movie.client.model.MovieDetail;
import com.community.board.movie.client.model.MovieSummary;
import com.community.board.movie.kofic.KoficClient;
import com.community.board.movie.kofic.KoreanTheatricalInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MovieControllerTests {

    @Autowired MockMvc mockMvc;
    @MockitoBean MovieClient movieClient;
    @MockitoBean KoficClient koficClient;

    @Test
    void searchesMoviesPublicly() throws Exception {
        when(movieClient.searchMovies("Fight Club")).thenReturn(List.of(
                new MovieSummary(550L, "Fight Club", "/poster.jpg", LocalDate.of(1999, 10, 15))));

        mockMvc.perform(get("/api/movies/search").queryParam("query", "Fight Club"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tmdbId").value(550))
                .andExpect(jsonPath("$[0].title").value("Fight Club"));
        verify(movieClient).searchMovies("Fight Club");
    }

    @Test
    void returnsMovieDetailPublicly() throws Exception {
        when(movieClient.getMovie(550L)).thenReturn(
                new MovieDetail(550L, "Fight Club", "/poster.jpg", LocalDate.of(1999, 10, 15)));

        mockMvc.perform(get("/api/movies/550"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tmdbId").value(550))
                .andExpect(jsonPath("$.releaseDate").value("1999-10-15"));
    }

    @Test
    void returnsKoreanTheatricalInformationPublicly() throws Exception {
        MovieDetail movie = new MovieDetail(550L, "옵세션", "/poster.jpg", LocalDate.of(2026, 9, 2));
        when(movieClient.getMovie(550L)).thenReturn(movie);
        when(koficClient.getKoreanTheatricalInfo(movie)).thenReturn(new KoreanTheatricalInfo(
                "AVAILABLE", LocalDate.of(2026, 9, 13), "20265146", "옵세션", "Obsession",
                List.of("공포", "스릴러"), List.of("미국"), 109, "청소년관람불가",
                LocalDate.of(2026, 9, 2), 13L, 6, new java.math.BigDecimal("3.3"), 475000L));

        mockMvc.perform(get("/api/movies/550/korean-theatrical"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.titleEnglish").value("Obsession"))
                .andExpect(jsonPath("$.runningTimeMinutes").value(109))
                .andExpect(jsonPath("$.boxOfficeRank").value(6))
                .andExpect(jsonPath("$.accumulatedAudience").value(475000));
        verify(koficClient).getKoreanTheatricalInfo(movie);
    }

    @Test
    void returnsNowPlayingMoviesPublicly() throws Exception {
        when(movieClient.getNowPlaying()).thenReturn(List.of(
                new MovieSummary(157336L, "Interstellar", "/interstellar.jpg", LocalDate.of(2014, 11, 5))));

        mockMvc.perform(get("/api/movies/now-playing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tmdbId").value(157336))
                .andExpect(jsonPath("$[0].posterPath").value("/interstellar.jpg"));
        verify(movieClient).getNowPlaying();
    }

    @Test
    void returnsRecommendationsPublicly() throws Exception {
        when(movieClient.getRecommendations(550L)).thenReturn(List.of(
                new MovieSummary(13L, "Forrest Gump", null, LocalDate.of(1994, 7, 6))));

        mockMvc.perform(get("/api/movies/550/recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tmdbId").value(13));
    }
}
