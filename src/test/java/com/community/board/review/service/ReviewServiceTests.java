package com.community.board.review.service;

import com.community.board.comment.repository.CommentRepository;
import com.community.board.integration.tmdb.exception.MovieNotFoundException;
import com.community.board.member.domain.Member;
import com.community.board.member.repository.MemberRepository;
import com.community.board.movie.client.MovieClient;
import com.community.board.movie.client.model.MovieDetail;
import com.community.board.movie.domain.Movie;
import com.community.board.movie.repository.MovieRepository;
import com.community.board.review.domain.Review;
import com.community.board.review.domain.InvalidReviewRatingException;
import com.community.board.review.domain.InvalidReviewUpdateException;
import com.community.board.review.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReviewServiceTests {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private MovieClient movieClient;

    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(
                memberRepository,
                movieRepository,
                reviewRepository,
                commentRepository,
                movieClient
        );
    }

    @Test
    void createsReviewWithExistingMovieWithoutCallingMovieClient() {
        Member member = member(1L, "reviewer");
        Movie movie = movie(10L, 550L, "Fight Club");
        prepareCreation(member, movie);

        reviewService.create(1L, 550L, "Title", "Content", new BigDecimal("4.5"));

        verify(movieClient, never()).getMovie(any(Long.class));
        verify(movieRepository, never()).save(any(Movie.class));
        verify(reviewRepository).saveAndFlush(any(Review.class));
    }

    @Test
    void fetchesAndSavesMissingMovieBeforeCreatingReview() {
        Member member = member(1L, "reviewer");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(movieRepository.findByTmdbId(550L)).thenReturn(Optional.empty());
        when(movieClient.getMovie(550L)).thenReturn(new MovieDetail(
                550L,
                "Fight Club",
                "/poster.jpg",
                LocalDate.of(1999, 10, 15)
        ));
        when(movieRepository.save(any(Movie.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reviewRepository.existsByMemberAndMovie(any(), any())).thenReturn(false);
        when(reviewRepository.saveAndFlush(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        reviewService.create(1L, 550L, "Title", "Content", new BigDecimal("4.5"));

        ArgumentCaptor<Movie> movieCaptor = ArgumentCaptor.forClass(Movie.class);
        verify(movieRepository).save(movieCaptor.capture());
        assertThat(movieCaptor.getValue().getTmdbId()).isEqualTo(550L);
        assertThat(movieCaptor.getValue().getTitle()).isEqualTo("Fight Club");
        assertThat(movieCaptor.getValue().getPosterPath()).isEqualTo("/poster.jpg");
        assertThat(movieCaptor.getValue().getReleaseDate()).isEqualTo(LocalDate.of(1999, 10, 15));
        verify(reviewRepository).saveAndFlush(any(Review.class));
    }

    @Test
    void rejectsDuplicateReviewBeforeSaving() {
        Member member = member(1L, "reviewer");
        Movie movie = movie(10L, 550L, "Fight Club");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(movieRepository.findByTmdbId(550L)).thenReturn(Optional.of(movie));
        when(reviewRepository.existsByMemberAndMovie(member, movie)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.create(
                1L, 550L, "Title", "Content", new BigDecimal("4.5")
        )).isInstanceOf(DuplicateReviewException.class);

        verify(reviewRepository, never()).saveAndFlush(any(Review.class));
    }

    @Test
    void doesNotSaveMovieOrReviewWhenTmdbMovieDoesNotExist() {
        Member member = member(1L, "reviewer");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(movieRepository.findByTmdbId(999L)).thenReturn(Optional.empty());
        when(movieClient.getMovie(999L)).thenThrow(new MovieNotFoundException(999L));

        assertThatThrownBy(() -> reviewService.create(
                1L, 999L, "Title", "Content", new BigDecimal("4.5")
        )).isInstanceOf(MovieNotFoundException.class);

        verify(movieRepository, never()).save(any(Movie.class));
        verify(reviewRepository, never()).saveAndFlush(any(Review.class));
    }

    @Test
    void keepsRatingUnchangedWhenCreatingReview() {
        Member member = member(1L, "reviewer");
        Movie movie = movie(10L, 550L, "Fight Club");
        prepareCreation(member, movie);

        reviewService.create(1L, 550L, "Title", "Content", new BigDecimal("4.5"));

        ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).saveAndFlush(reviewCaptor.capture());
        assertThat(reviewCaptor.getValue().getRating()).isEqualByComparingTo("4.5");
    }

    @Test
    void rejectsRatingThatIsNotInHalfPointSteps() {
        Member member = member(1L, "reviewer");
        Movie movie = movie(10L, 550L, "Fight Club");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(movieRepository.findByTmdbId(550L)).thenReturn(Optional.of(movie));
        when(reviewRepository.existsByMemberAndMovie(member, movie)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.create(
                1L, 550L, "Title", "Content", new BigDecimal("4.6")
        )).isInstanceOf(InvalidReviewRatingException.class);
    }

    @Test
    void rejectsUpdateByAnotherMember() {
        Member currentMember = member(2L, "other");
        Review review = review(20L, member(1L, "author"), movie(10L, 550L, "Fight Club"));
        when(memberRepository.findById(2L)).thenReturn(Optional.of(currentMember));
        when(reviewRepository.findById(20L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.update(
                2L,
                20L,
                new ReviewUpdateCommand(true, "Changed", false, null, false, null)
        )).isInstanceOf(ReviewOwnershipException.class);
    }

    @Test
    void rejectsDeleteByAnotherMember() {
        Member currentMember = member(2L, "other");
        Review review = review(20L, member(1L, "author"), movie(10L, 550L, "Fight Club"));
        when(memberRepository.findById(2L)).thenReturn(Optional.of(currentMember));
        when(reviewRepository.findById(20L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.delete(2L, 20L))
                .isInstanceOf(ReviewOwnershipException.class);

        verify(commentRepository, never()).deleteByReview(any(Review.class));
        verify(reviewRepository, never()).delete(any(Review.class));
    }

    @Test
    void updatesOnlyTitleByAuthor() {
        Member author = member(1L, "author");
        Review review = review(20L, author, movie(10L, 550L, "Fight Club"));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(author));
        when(reviewRepository.findById(20L)).thenReturn(Optional.of(review));

        ReviewView result = reviewService.update(
                1L,
                20L,
                new ReviewUpdateCommand(true, "Changed", false, null, false, null)
        );

        assertThat(result.title()).isEqualTo("Changed");
        assertThat(result.content()).isEqualTo("Content");
        assertThat(result.rating()).isEqualByComparingTo("4.0");
        assertThat(result.updatedAt()).isNotNull();
    }

    @Test
    void updatesOnlyContentByAuthor() {
        ReviewView result = updateAsAuthor(
                new ReviewUpdateCommand(false, null, true, "Changed content", false, null)
        );

        assertThat(result.title()).isEqualTo("Title");
        assertThat(result.content()).isEqualTo("Changed content");
        assertThat(result.rating()).isEqualByComparingTo("4.0");
    }

    @Test
    void updatesOnlyRatingByAuthor() {
        ReviewView result = updateAsAuthor(
                new ReviewUpdateCommand(false, null, false, null, true, new BigDecimal("4.5"))
        );

        assertThat(result.title()).isEqualTo("Title");
        assertThat(result.content()).isEqualTo("Content");
        assertThat(result.rating()).isEqualByComparingTo("4.5");
    }

    @Test
    void updatesTitleAndRatingWhileKeepingContent() {
        ReviewView result = updateAsAuthor(
                new ReviewUpdateCommand(true, "Changed", false, null, true, new BigDecimal("4.5"))
        );

        assertThat(result.title()).isEqualTo("Changed");
        assertThat(result.content()).isEqualTo("Content");
        assertThat(result.rating()).isEqualByComparingTo("4.5");
    }

    @Test
    void rejectsEmptyUpdate() {
        assertThatThrownBy(() -> updateAsAuthor(
                new ReviewUpdateCommand(false, null, false, null, false, null)
        )).isInstanceOf(InvalidReviewUpdateException.class);
    }

    @Test
    void rejectsBlankTitleUpdate() {
        assertThatThrownBy(() -> updateAsAuthor(
                new ReviewUpdateCommand(true, " ", false, null, false, null)
        )).isInstanceOf(InvalidReviewUpdateException.class);
    }

    @Test
    void rejectsBlankContentUpdate() {
        assertThatThrownBy(() -> updateAsAuthor(
                new ReviewUpdateCommand(false, null, true, " ", false, null)
        )).isInstanceOf(InvalidReviewUpdateException.class);
    }

    @Test
    void rejectsInvalidRatingUpdate() {
        assertThatThrownBy(() -> updateAsAuthor(
                new ReviewUpdateCommand(false, null, false, null, true, new BigDecimal("4.6"))
        )).isInstanceOf(InvalidReviewRatingException.class);
    }

    @Test
    void deletesCommentsBeforeReviewByAuthor() {
        Member author = member(1L, "author");
        Review review = review(20L, author, movie(10L, 550L, "Fight Club"));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(author));
        when(reviewRepository.findById(20L)).thenReturn(Optional.of(review));

        reviewService.delete(1L, 20L);

        var order = org.mockito.Mockito.inOrder(commentRepository, reviewRepository);
        order.verify(commentRepository).deleteByReview(review);
        order.verify(reviewRepository).delete(review);
    }

    private void prepareCreation(Member member, Movie movie) {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(movieRepository.findByTmdbId(550L)).thenReturn(Optional.of(movie));
        when(reviewRepository.existsByMemberAndMovie(member, movie)).thenReturn(false);
        when(reviewRepository.saveAndFlush(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private ReviewView updateAsAuthor(ReviewUpdateCommand command) {
        Member author = member(1L, "author");
        Review review = review(20L, author, movie(10L, 550L, "Fight Club"));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(author));
        when(reviewRepository.findById(20L)).thenReturn(Optional.of(review));
        return reviewService.update(1L, 20L, command);
    }

    private Member member(Long id, String nickname) {
        Member member = org.mockito.Mockito.mock(Member.class);
        when(member.getId()).thenReturn(id);
        when(member.getNickname()).thenReturn(nickname);
        return member;
    }

    private Movie movie(Long id, Long tmdbId, String title) {
        Movie movie = org.mockito.Mockito.mock(Movie.class);
        when(movie.getId()).thenReturn(id);
        when(movie.getTmdbId()).thenReturn(tmdbId);
        when(movie.getTitle()).thenReturn(title);
        return movie;
    }

    private Review review(Long id, Member member, Movie movie) {
        Review review = Review.create(member, movie, "Title", "Content", new BigDecimal("4.0"));
        return review;
    }
}
