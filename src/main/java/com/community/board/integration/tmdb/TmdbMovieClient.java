package com.community.board.integration.tmdb;

import com.community.board.integration.tmdb.dto.TmdbMovieResponse;
import com.community.board.integration.tmdb.dto.TmdbMovieResult;
import com.community.board.integration.tmdb.dto.TmdbMovieResultsResponse;
import com.community.board.integration.tmdb.exception.MovieClientAuthenticationException;
import com.community.board.integration.tmdb.exception.MovieClientCommunicationException;
import com.community.board.integration.tmdb.exception.MovieClientException;
import com.community.board.integration.tmdb.exception.MovieClientUnavailableException;
import com.community.board.integration.tmdb.exception.MovieNotFoundException;
import com.community.board.movie.client.MovieClient;
import com.community.board.movie.client.model.MovieDetail;
import com.community.board.movie.client.model.MovieSummary;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriBuilder;

import java.time.LocalDate;
import java.util.List;

public class TmdbMovieClient implements MovieClient {

    private final RestClient restClient;
    private final TmdbProperties properties;

    public TmdbMovieClient(RestClient restClient, TmdbProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public List<MovieSummary> searchMovies(String query) {
        TmdbMovieResultsResponse response = execute(() -> restClient.get()
                .uri(uriBuilder -> addSearchParameters(uriBuilder.path("/3/search/movie"), query).build())
                .retrieve()
                .onStatus(status -> status.value() == 401 || status.value() == 403,
                        (request, result) -> { throw new MovieClientAuthenticationException(); })
                .onStatus(HttpStatusCode::is5xxServerError,
                        (request, result) -> { throw new MovieClientUnavailableException(); })
                .requiredBody(TmdbMovieResultsResponse.class));

        return response.results().stream().map(this::toSummary).toList();
    }

    @Override
    public MovieDetail getMovie(long tmdbId) {
        TmdbMovieResponse response = execute(() -> restClient.get()
                .uri(uriBuilder -> addLanguage(uriBuilder.path("/3/movie/{movieId}"))
                        .build(tmdbId))
                .retrieve()
                .onStatus(status -> status.value() == 404,
                        (request, result) -> { throw new MovieNotFoundException(tmdbId); })
                .onStatus(status -> status.value() == 401 || status.value() == 403,
                        (request, result) -> { throw new MovieClientAuthenticationException(); })
                .onStatus(HttpStatusCode::is5xxServerError,
                        (request, result) -> { throw new MovieClientUnavailableException(); })
                .requiredBody(TmdbMovieResponse.class));

        return new MovieDetail(
                response.id(),
                response.title(),
                response.posterPath(),
                parseReleaseDate(response.releaseDate())
        );
    }

    @Override
    public List<MovieSummary> getRecommendations(long tmdbId) {
        TmdbMovieResultsResponse response = execute(() -> restClient.get()
                .uri(uriBuilder -> addLanguage(uriBuilder.path("/3/movie/{movieId}/recommendations"))
                        .build(tmdbId))
                .retrieve()
                .onStatus(status -> status.value() == 404,
                        (request, result) -> { throw new MovieNotFoundException(tmdbId); })
                .onStatus(status -> status.value() == 401 || status.value() == 403,
                        (request, result) -> { throw new MovieClientAuthenticationException(); })
                .onStatus(HttpStatusCode::is5xxServerError,
                        (request, result) -> { throw new MovieClientUnavailableException(); })
                .requiredBody(TmdbMovieResultsResponse.class));

        return response.results().stream().map(this::toSummary).toList();
    }

    private UriBuilder addSearchParameters(UriBuilder uriBuilder, String query) {
        UriBuilder result = uriBuilder.queryParam("query", query);
        result = addLanguage(result);
        if (hasText(properties.region())) {
            result = result.queryParam("region", properties.region());
        }
        return result;
    }

    private UriBuilder addLanguage(UriBuilder uriBuilder) {
        if (hasText(properties.language())) {
            return uriBuilder.queryParam("language", properties.language());
        }
        return uriBuilder;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private MovieSummary toSummary(TmdbMovieResult result) {
        return new MovieSummary(
                result.id(),
                result.title(),
                result.posterPath(),
                parseReleaseDate(result.releaseDate())
        );
    }

    private LocalDate parseReleaseDate(String releaseDate) {
        return hasText(releaseDate) ? LocalDate.parse(releaseDate) : null;
    }

    private <T> T execute(ClientCall<T> call) {
        try {
            return call.execute();
        } catch (MovieClientException exception) {
            throw exception;
        } catch (ResourceAccessException exception) {
            throw new MovieClientCommunicationException(exception);
        } catch (RestClientResponseException exception) {
            throw new MovieClientException("TMDB 요청이 거부되었습니다.", exception);
        } catch (RestClientException exception) {
            throw new MovieClientException("TMDB 응답을 처리할 수 없습니다.", exception);
        }
    }

    @FunctionalInterface
    private interface ClientCall<T> {
        T execute();
    }
}
