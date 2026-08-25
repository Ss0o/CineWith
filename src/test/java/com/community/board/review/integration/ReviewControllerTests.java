package com.community.board.review.integration;

import com.community.board.TestcontainersConfiguration;
import com.community.board.comment.domain.Comment;
import com.community.board.comment.repository.CommentRepository;
import com.community.board.integration.tmdb.exception.MovieClientAuthenticationException;
import com.community.board.integration.tmdb.exception.MovieClientUnavailableException;
import com.community.board.integration.tmdb.exception.MovieNotFoundException;
import com.community.board.member.domain.Member;
import com.community.board.member.domain.OAuthProvider;
import com.community.board.member.repository.MemberRepository;
import com.community.board.movie.client.MovieClient;
import com.community.board.movie.domain.Movie;
import com.community.board.movie.repository.MovieRepository;
import com.community.board.review.domain.Review;
import com.community.board.review.repository.ReviewRepository;
import com.community.board.security.CommunityOidcPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class ReviewControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private CommentRepository commentRepository;

    @MockitoBean
    private MovieClient movieClient;

    @Test
    void rejectsAnonymousCreateWithUnauthorized() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequest(550L)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void rejectsSignupRequiredCreateWithForbidden() throws Exception {
        CommunityOidcPrincipal principal = CommunityOidcPrincipal.signupRequired(
                oidcUser("signup-required-review", "signup@example.com")
        );

        mockMvc.perform(post("/api/reviews")
                        .with(authentication(oauthAuthentication(principal)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequest(550L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void createsReviewForAuthenticatedMember() throws Exception {
        Member member = saveMember("create-review", "creator");
        saveMovie(550L, "Fight Club");

        mockMvc.perform(post("/api/reviews")
                        .with(authentication(oauthAuthentication(memberPrincipal(member))))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequest(550L)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/reviews/\\d+")))
                .andExpect(jsonPath("$.tmdbId").value(550L))
                .andExpect(jsonPath("$.authorNickname").value("creator"))
                .andExpect(jsonPath("$.rating").value(4.5));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.5", "5.0"})
    void createsReviewWithBoundaryRating(String rating) throws Exception {
        Member member = saveMember("boundary-create-" + rating, "creator" + rating.replace(".", ""));
        saveMovie(550L, "Fight Club");

        mockMvc.perform(post("/api/reviews")
                        .with(authentication(oauthAuthentication(memberPrincipal(member))))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(550L, rating)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(Double.parseDouble(rating)));
    }

    @Test
    void rejectsDuplicateReviewWithConflict() throws Exception {
        Member member = saveMember("duplicate-review", "duplicateAuthor");
        Movie movie = saveMovie(550L, "Fight Club");
        reviewRepository.saveAndFlush(Review.create(member, movie, "Existing", "Content", new BigDecimal("4.0")));

        mockMvc.perform(post("/api/reviews")
                        .with(authentication(oauthAuthentication(memberPrincipal(member))))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequest(550L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_REVIEW"));
    }

    @Test
    void rejectsInvalidRatingWithBadRequest() throws Exception {
        Member member = saveMember("invalid-rating", "ratingAuthor");
        saveMovie(550L, "Fight Club");

        mockMvc.perform(post("/api/reviews")
                        .with(authentication(oauthAuthentication(memberPrincipal(member))))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tmdbId":550,"title":"Title","content":"Content","rating":4.6}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REVIEW_RATING"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.0", "5.5"})
    void rejectsOutOfRangeRatingWithBadRequest(String rating) throws Exception {
        Member member = saveMember("out-of-range-" + rating, "rating" + rating.replace(".", ""));
        saveMovie(550L, "Fight Club");

        mockMvc.perform(post("/api/reviews")
                        .with(authentication(oauthAuthentication(memberPrincipal(member))))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(550L, rating)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void returnsNotFoundWhenTmdbMovieDoesNotExist() throws Exception {
        Member member = saveMember("missing-tmdb", "missingMovieAuthor");
        when(movieClient.getMovie(999L)).thenThrow(new MovieNotFoundException(999L));

        mockMvc.perform(post("/api/reviews")
                        .with(authentication(oauthAuthentication(memberPrincipal(member))))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequest(999L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MOVIE_NOT_FOUND"));
    }

    @Test
    void mapsTmdbAuthenticationFailureToBadGateway() throws Exception {
        Member member = saveMember("tmdb-auth", "tmdbAuthAuthor");
        when(movieClient.getMovie(550L)).thenThrow(new MovieClientAuthenticationException());

        mockMvc.perform(post("/api/reviews")
                        .with(authentication(oauthAuthentication(memberPrincipal(member))))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequest(550L)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("MOVIE_PROVIDER_ERROR"))
                .andExpect(jsonPath("$.message").value("영화 정보를 가져오지 못했습니다."));
    }

    @Test
    void mapsUnavailableTmdbToServiceUnavailable() throws Exception {
        Member member = saveMember("tmdb-unavailable", "tmdbUnavailableAuthor");
        when(movieClient.getMovie(550L)).thenThrow(new MovieClientUnavailableException());

        mockMvc.perform(post("/api/reviews")
                        .with(authentication(oauthAuthentication(memberPrincipal(member))))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequest(550L)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("MOVIE_PROVIDER_UNAVAILABLE"));
    }

    @Test
    void returnsPublicReviewDetail() throws Exception {
        Member member = saveMember("detail-review", "detailAuthor");
        Movie movie = saveMovie(550L, "Fight Club");
        Review review = reviewRepository.saveAndFlush(
                Review.create(member, movie, "Detail", "Detail content", new BigDecimal("4.5"))
        );

        mockMvc.perform(get("/api/reviews/{reviewId}", review.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewId").value(review.getId()))
                .andExpect(jsonPath("$.movieTitle").value("Fight Club"))
                .andExpect(jsonPath("$.authorNickname").value("detailAuthor"))
                .andExpect(jsonPath("$.rating").value(4.5));
    }

    @Test
    void returnsNotFoundForMissingReview() throws Exception {
        mockMvc.perform(get("/api/reviews/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REVIEW_NOT_FOUND"));
    }

    @Test
    void returnsMovieReviewsWithDefaultPaginationAndNewestFirst() throws Exception {
        Member first = saveMember("list-first", "firstAuthor");
        Member second = saveMember("list-second", "secondAuthor");
        Movie movie = saveMovie(550L, "Fight Club");
        reviewRepository.saveAndFlush(Review.create(first, movie, "First", "Content", new BigDecimal("4.0")));
        reviewRepository.saveAndFlush(Review.create(second, movie, "Second", "Content", new BigDecimal("4.5")));

        mockMvc.perform(get("/api/movies/550/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].title").value("Second"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void updatesOnlyRatingAndReturnsCompleteReview() throws Exception {
        Member author = saveMember("update-author", "updateAuthor");
        Review review = saveReview(author, 550L);

        mockMvc.perform(patch("/api/reviews/{reviewId}", review.getId())
                        .with(authentication(oauthAuthentication(memberPrincipal(author))))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating":5.0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Title"))
                .andExpect(jsonPath("$.content").value("Content"))
                .andExpect(jsonPath("$.rating").value(5.0))
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void updatesOnlyTitleAndKeepsContentAndRating() throws Exception {
        Member author = saveMember("title-update-author", "titleUpdateAuthor");
        Review review = saveReview(author, 550L);

        mockMvc.perform(patch("/api/reviews/{reviewId}", review.getId())
                        .with(authentication(oauthAuthentication(memberPrincipal(author))))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"새 제목"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("새 제목"))
                .andExpect(jsonPath("$.content").value("Content"))
                .andExpect(jsonPath("$.rating").value(4.0));
    }

    @Test
    void rejectsEmptyUpdate() throws Exception {
        Member author = saveMember("empty-update-author", "emptyUpdateAuthor");
        Review review = saveReview(author, 550L);

        mockMvc.perform(patch("/api/reviews/{reviewId}", review.getId())
                        .with(authentication(oauthAuthentication(memberPrincipal(author))))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REVIEW_UPDATE"));
    }

    @Test
    void rejectsBlankTitleUpdate() throws Exception {
        Member author = saveMember("blank-update-author", "blankUpdateAuthor");
        Review review = saveReview(author, 550L);

        mockMvc.perform(patch("/api/reviews/{reviewId}", review.getId())
                        .with(authentication(oauthAuthentication(memberPrincipal(author))))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REVIEW_UPDATE"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"title\":null}",
            "{\"content\":null}",
            "{\"rating\":null}"
    })
    void rejectsExplicitNullUpdate(String request) throws Exception {
        Member author = saveMember("null-update-" + request.hashCode(), "nullUpdate" + Math.abs(request.hashCode()));
        Review review = saveReview(author, 550L);

        mockMvc.perform(patch("/api/reviews/{reviewId}", review.getId())
                        .with(authentication(oauthAuthentication(memberPrincipal(author))))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REVIEW_UPDATE"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.0", "4.6", "5.5"})
    void rejectsInvalidRatingWhenUpdating(String rating) throws Exception {
        Member author = saveMember("invalid-update-" + rating, "update" + rating.replace(".", ""));
        Review review = saveReview(author, 550L);

        mockMvc.perform(patch("/api/reviews/{reviewId}", review.getId())
                        .with(authentication(oauthAuthentication(memberPrincipal(author))))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating":%s}
                                """.formatted(rating)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsUpdateByAnotherMember() throws Exception {
        Member author = saveMember("update-owner", "owner");
        Member other = saveMember("update-other", "other");
        Review review = saveReview(author, 550L);

        mockMvc.perform(patch("/api/reviews/{reviewId}", review.getId())
                        .with(authentication(oauthAuthentication(memberPrincipal(other))))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Changed"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("REVIEW_FORBIDDEN"));
    }

    @Test
    void rejectsAnonymousUpdateWithUnauthorized() throws Exception {
        mockMvc.perform(patch("/api/reviews/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating":5.0}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void deletesReviewByAuthor() throws Exception {
        Member author = saveMember("delete-author", "deleteAuthor");
        Review review = saveReview(author, 550L);
        Comment comment = commentRepository.saveAndFlush(Comment.create(author, review, "Delete together"));
        Long reviewId = review.getId();
        Long commentId = comment.getId();

        mockMvc.perform(delete("/api/reviews/{reviewId}", reviewId)
                        .with(authentication(oauthAuthentication(memberPrincipal(author))))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        reviewRepository.flush();
        assertThat(reviewRepository.findById(reviewId)).isEmpty();
        assertThat(commentRepository.findById(commentId)).isEmpty();
    }

    @Test
    void rejectsDeleteByAnotherMember() throws Exception {
        Member author = saveMember("delete-owner", "deleteOwner");
        Member other = saveMember("delete-other", "deleteOther");
        Review review = saveReview(author, 550L);

        mockMvc.perform(delete("/api/reviews/{reviewId}", review.getId())
                        .with(authentication(oauthAuthentication(memberPrincipal(other))))
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("REVIEW_FORBIDDEN"));
    }

    private String validCreateRequest(Long tmdbId) {
        return createRequest(tmdbId, "4.5");
    }

    private String createRequest(Long tmdbId, String rating) {
        return """
                {"tmdbId":%d,"title":"Title","content":"Content","rating":%s}
                """.formatted(tmdbId, rating);
    }

    private Member saveMember(String providerId, String nickname) {
        return memberRepository.saveAndFlush(
                Member.create(OAuthProvider.GOOGLE, providerId, providerId + "@example.com", nickname)
        );
    }

    private Movie saveMovie(Long tmdbId, String title) {
        return movieRepository.saveAndFlush(Movie.create(tmdbId, title, null, null));
    }

    private Review saveReview(Member author, Long tmdbId) {
        return reviewRepository.saveAndFlush(
                Review.create(author, saveMovie(tmdbId, "Movie " + tmdbId), "Title", "Content", new BigDecimal("4.0"))
        );
    }

    private OAuth2AuthenticationToken oauthAuthentication(CommunityOidcPrincipal principal) {
        return new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "google");
    }

    private CommunityOidcPrincipal memberPrincipal(Member member) {
        return CommunityOidcPrincipal.member(
                oidcUser(member.getProviderId(), member.getEmail()),
                member
        );
    }

    private DefaultOidcUser oidcUser(String subject, String email) {
        Instant issuedAt = Instant.now();
        OidcIdToken idToken = new OidcIdToken(
                "id-token-value",
                issuedAt,
                issuedAt.plusSeconds(300),
                Map.of(StandardClaimNames.SUB, subject, StandardClaimNames.EMAIL, email)
        );
        return new DefaultOidcUser(List.of(), idToken);
    }
}
