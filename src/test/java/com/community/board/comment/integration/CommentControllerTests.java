package com.community.board.comment.integration;

import com.community.board.TestcontainersConfiguration;
import com.community.board.comment.domain.Comment;
import com.community.board.comment.repository.CommentRepository;
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
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.Matchers.hasSize;
import static org.assertj.core.api.Assertions.assertThat;
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
class CommentControllerTests {

    private static final AtomicLong IDS = new AtomicLong(1000);

    @Autowired MockMvc mockMvc;
    @Autowired MemberRepository memberRepository;
    @Autowired MovieRepository movieRepository;
    @Autowired ReviewRepository reviewRepository;
    @Autowired CommentRepository commentRepository;
    @MockitoBean MovieClient movieClient;

    @Test
    void createsCommentAsMember() throws Exception {
        Member member = saveMember("create");
        Review review = saveReview(member);

        mockMvc.perform(post("/api/reviews/{reviewId}/comments", review.getId())
                        .with(authentication(oauthAuthentication(memberPrincipal(member))))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"재미있게 봤습니다.\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/comments/")))
                .andExpect(jsonPath("$.reviewId").value(review.getId()))
                .andExpect(jsonPath("$.authorNickname").value(member.getNickname()))
                .andExpect(jsonPath("$.content").value("재미있게 봤습니다."))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void protectsCommentCreationByAuthenticationState() throws Exception {
        Member author = saveMember("security-review");
        Review review = saveReview(author);

        mockMvc.perform(post("/api/reviews/{reviewId}/comments", review.getId())
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Comment\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        CommunityOidcPrincipal signup = CommunityOidcPrincipal.signupRequired(
                oidcUser("signup-comment", "signup-comment@example.com"));
        mockMvc.perform(post("/api/reviews/{reviewId}/comments", review.getId())
                        .with(authentication(oauthAuthentication(signup))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"Comment\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void rejectsMissingReviewAndBlankCreate() throws Exception {
        Member member = saveMember("invalid-create");
        mockMvc.perform(post("/api/reviews/999999/comments")
                        .with(authentication(oauthAuthentication(memberPrincipal(member)))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"Comment\"}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("REVIEW_NOT_FOUND"));

        mockMvc.perform(post("/api/reviews/999999/comments")
                        .with(authentication(oauthAuthentication(memberPrincipal(member)))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"   \"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void publiclyListsExistingReviewCommentsOldestFirstWithDefaults() throws Exception {
        Member member = saveMember("list");
        Review review = saveReview(member);
        commentRepository.saveAndFlush(Comment.create(member, review, "First"));
        commentRepository.saveAndFlush(Comment.create(member, review, "Second"));

        mockMvc.perform(get("/api/reviews/{reviewId}/comments", review.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].content").value("First"))
                .andExpect(jsonPath("$.content[1].content").value("Second"));
    }

    @Test
    void rejectsMissingReviewList() throws Exception {
        mockMvc.perform(get("/api/reviews/999999/comments"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("REVIEW_NOT_FOUND"));
    }

    @Test
    void acceptsMaximumCommentPageSize() throws Exception {
        Member member = saveMember("max-page-size");
        Review review = saveReview(member);

        mockMvc.perform(get("/api/reviews/{reviewId}/comments?size=100", review.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void rejectsCommentPageSizeAboveMaximum() throws Exception {
        Member member = saveMember("above-max-page-size");
        Review review = saveReview(member);

        mockMvc.perform(get("/api/reviews/{reviewId}/comments?size=101", review.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void returnsProjectErrorForInvalidCommentAndReviewPagination() throws Exception {
        Member member = saveMember("pagination");
        Review review = saveReview(member);

        mockMvc.perform(get("/api/reviews/{reviewId}/comments?page=-1", review.getId()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").exists());
        mockMvc.perform(get("/api/reviews/{reviewId}/comments?size=0", review.getId()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        mockMvc.perform(get("/api/movies/{tmdbId}/reviews?page=-1", review.getMovie().getTmdbId()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        mockMvc.perform(get("/api/movies/{tmdbId}/reviews?size=0", review.getMovie().getTmdbId()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void updatesOnlyByCommentAuthorAndRequiresContent() throws Exception {
        Member reviewAuthor = saveMember("update-review-author");
        Member commentAuthor = saveMember("update-comment-author");
        Member other = saveMember("update-other");
        Review review = saveReview(reviewAuthor);
        Comment comment = commentRepository.saveAndFlush(Comment.create(commentAuthor, review, "Before"));

        mockMvc.perform(patch("/api/comments/{id}", comment.getId())
                        .with(authentication(oauthAuthentication(memberPrincipal(commentAuthor)))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"After\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").value("After"))
                .andExpect(jsonPath("$.updatedAt").exists());

        assertForbiddenPatch(comment.getId(), other);
        assertForbiddenPatch(comment.getId(), reviewAuthor);
        for (String body : List.of("{}", "{\"content\":null}", "{\"content\":\"\"}", "{\"content\":\"   \"}")) {
            mockMvc.perform(patch("/api/comments/{id}", comment.getId())
                            .with(authentication(oauthAuthentication(memberPrincipal(commentAuthor)))).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        }
    }

    @Test
    void returnsNotFoundForMissingCommentUpdateAndDelete() throws Exception {
        Member member = saveMember("missing-comment");
        mockMvc.perform(patch("/api/comments/999999")
                        .with(authentication(oauthAuthentication(memberPrincipal(member)))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"After\"}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));
        mockMvc.perform(delete("/api/comments/999999")
                        .with(authentication(oauthAuthentication(memberPrincipal(member)))).with(csrf()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));
    }

    @Test
    void hardDeletesOnlyByCommentAuthor() throws Exception {
        Member reviewAuthor = saveMember("delete-review-author");
        Member commentAuthor = saveMember("delete-comment-author");
        Member other = saveMember("delete-other");
        Review review = saveReview(reviewAuthor);
        Comment comment = commentRepository.saveAndFlush(Comment.create(commentAuthor, review, "Delete"));

        assertForbiddenDelete(comment.getId(), other);
        assertForbiddenDelete(comment.getId(), reviewAuthor);
        mockMvc.perform(delete("/api/comments/{id}", comment.getId())
                        .with(authentication(oauthAuthentication(memberPrincipal(commentAuthor)))).with(csrf()))
                .andExpect(status().isNoContent());
        commentRepository.flush();
        assertThat(commentRepository.findById(comment.getId())).isEmpty();
    }

    private void assertForbiddenPatch(Long id, Member member) throws Exception {
        mockMvc.perform(patch("/api/comments/{id}", id)
                        .with(authentication(oauthAuthentication(memberPrincipal(member)))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"Forbidden\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("COMMENT_FORBIDDEN"));
    }

    private void assertForbiddenDelete(Long id, Member member) throws Exception {
        mockMvc.perform(delete("/api/comments/{id}", id)
                        .with(authentication(oauthAuthentication(memberPrincipal(member)))).with(csrf()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("COMMENT_FORBIDDEN"));
    }

    private Member saveMember(String suffix) {
        long value = IDS.incrementAndGet();
        return memberRepository.saveAndFlush(Member.create(
                OAuthProvider.GOOGLE, suffix + value, suffix + value + "@example.com", suffix + value));
    }

    private Review saveReview(Member member) {
        long tmdbId = IDS.incrementAndGet();
        Movie movie = movieRepository.saveAndFlush(Movie.create(tmdbId, "Movie " + tmdbId, null, null));
        return reviewRepository.saveAndFlush(Review.create(
                member, movie, "Title", "Content", new BigDecimal("4.0")));
    }

    private OAuth2AuthenticationToken oauthAuthentication(CommunityOidcPrincipal principal) {
        return new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "google");
    }

    private CommunityOidcPrincipal memberPrincipal(Member member) {
        return CommunityOidcPrincipal.member(oidcUser(member.getProviderId(), member.getEmail()), member);
    }

    private DefaultOidcUser oidcUser(String subject, String email) {
        Instant now = Instant.now();
        OidcIdToken token = new OidcIdToken("token", now, now.plusSeconds(300),
                Map.of(StandardClaimNames.SUB, subject, StandardClaimNames.EMAIL, email));
        return new DefaultOidcUser(List.of(), token);
    }
}
