package com.community.board.movie.integration;

import com.community.board.TestcontainersConfiguration;
import com.community.board.movie.client.MovieClient;
import com.community.board.movie.client.model.MovieDetail;
import com.community.board.movie.client.model.MovieSummary;
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
