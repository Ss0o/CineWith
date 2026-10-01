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
import com.community.board.recommendation.repository.CommentRecommendationRepository;
import com.community.board.recommendation.repository.ReviewRecommendationRepository;
import com.community.board.recommendation.service.RecommendationService;
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
/** 리뷰 서비스의 영화 확보, 중복 방지, 소유권, 부분 수정, 삭제 순서를 단위 검증한다. */
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

    @Mock private RecommendationService recommendationService;
    @Mock private ReviewRecommendationRepository reviewRecommendationRepository;
    @Mock private CommentRecommendationRepository commentRecommendationRepository;

    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(
                memberRepository,
                movieRepository,
                reviewRepository,
                commentRepository,
                movieClient,
                recommendationService,
                reviewRecommendationRepository,
                commentRecommendationRepository
        );
    }

    @Test
    void createsReviewWithExistingMovieWithoutCallingMovieClient() {
        // Given: 로컬에 영화가 있고 작성 가능한 회원이 있다.
        Member member = member(1L, "reviewer");
        Movie movie = movie(10L, 550L, "Fight Club");
        prepareCreation(member, movie);

        // When: 해당 영화의 리뷰를 작성한다.
        reviewService.create(1L, 550L, "Title", "Content", new BigDecimal("4.5"));

        // Then: 외부 조회나 영화 저장 없이 리뷰만 저장한다.
        verify(movieClient, never()).getMovie(any(Long.class));
        verify(movieRepository, never()).save(any(Movie.class));
        verify(reviewRepository).saveAndFlush(any(Review.class));
    }

    @Test
    void fetchesAndSavesMissingMovieBeforeCreatingReview() {
        // Given: 로컬 영화는 없고 TMDB 상세 조회는 성공한다.
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

        // When: 리뷰를 작성한다.
        reviewService.create(1L, 550L, "Title", "Content", new BigDecimal("4.5"));

        // Then: TMDB의 최소 영화 정보를 저장한 뒤 리뷰를 저장한다.
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
        // Given: 같은 회원과 영화의 리뷰가 이미 있다.
        Member member = member(1L, "reviewer");
        Movie movie = movie(10L, 550L, "Fight Club");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(movieRepository.findByTmdbId(550L)).thenReturn(Optional.of(movie));
        when(reviewRepository.existsByMemberAndMovie(member, movie)).thenReturn(true);

        // When / Then: 추가 작성은 거부되고 저장은 발생하지 않는다.
        assertThatThrownBy(() -> reviewService.create(
                1L, 550L, "Title", "Content", new BigDecimal("4.5")
        )).isInstanceOf(DuplicateReviewException.class);

        verify(reviewRepository, never()).saveAndFlush(any(Review.class));
    }

    @Test
    void doesNotSaveMovieOrReviewWhenTmdbMovieDoesNotExist() {
        // Given: 로컬 영화가 없고 TMDB도 영화를 찾지 못한다.
        Member member = member(1L, "reviewer");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(movieRepository.findByTmdbId(999L)).thenReturn(Optional.empty());
        when(movieClient.getMovie(999L)).thenThrow(new MovieNotFoundException(999L));

        // When / Then: 외부의 not-found를 전파하며 어떤 엔터티도 저장하지 않는다.
        assertThatThrownBy(() -> reviewService.create(
                1L, 999L, "Title", "Content", new BigDecimal("4.5")
        )).isInstanceOf(MovieNotFoundException.class);

        verify(movieRepository, never()).save(any(Movie.class));
        verify(reviewRepository, never()).saveAndFlush(any(Review.class));
    }

    @Test
    void keepsRatingUnchangedWhenCreatingReview() {
        // Given: 유효한 작성 조건과 4.5 평점이 있다.
        Member member = member(1L, "reviewer");
        Movie movie = movie(10L, 550L, "Fight Club");
        prepareCreation(member, movie);

        // When: 리뷰를 작성한다.
        reviewService.create(1L, 550L, "Title", "Content", new BigDecimal("4.5"));

        // Then: API 입력 평점이 도메인과 저장 요청까지 변경 없이 전달된다.
        ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).saveAndFlush(reviewCaptor.capture());
        assertThat(reviewCaptor.getValue().getRating()).isEqualByComparingTo("4.5");
    }

    @Test
    void rejectsRatingThatIsNotInHalfPointSteps() {
        // Given: 유효한 회원과 영화가 있다.
        Member member = member(1L, "reviewer");
        Movie movie = movie(10L, 550L, "Fight Club");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(movieRepository.findByTmdbId(550L)).thenReturn(Optional.of(movie));
        when(reviewRepository.existsByMemberAndMovie(member, movie)).thenReturn(false);

        // When / Then: 0.5 단위가 아닌 평점은 도메인에서 거부된다.
        assertThatThrownBy(() -> reviewService.create(
                1L, 550L, "Title", "Content", new BigDecimal("4.6")
        )).isInstanceOf(InvalidReviewRatingException.class);
    }

    @Test
    void rejectsUpdateByAnotherMember() {
        // Given: 현재 회원이 아닌 다른 작성자의 리뷰가 있다.
        Member currentMember = member(2L, "other");
        Review review = review(20L, member(1L, "author"), movie(10L, 550L, "Fight Club"));
        when(memberRepository.findById(2L)).thenReturn(Optional.of(currentMember));
        when(reviewRepository.findById(20L)).thenReturn(Optional.of(review));

        // When / Then: 비작성자의 수정은 소유권 예외로 거부된다.
        assertThatThrownBy(() -> reviewService.update(
                2L,
                20L,
                new ReviewUpdateCommand(true, "Changed", false, null, false, null)
        )).isInstanceOf(ReviewOwnershipException.class);
    }

    @Test
    void rejectsDeleteByAnotherMember() {
        // Given: 현재 회원이 아닌 다른 작성자의 리뷰가 있다.
        Member currentMember = member(2L, "other");
        Review review = review(20L, member(1L, "author"), movie(10L, 550L, "Fight Club"));
        when(memberRepository.findById(2L)).thenReturn(Optional.of(currentMember));
        when(reviewRepository.findById(20L)).thenReturn(Optional.of(review));

        // When / Then: 비작성자의 삭제는 거부되고 댓글·리뷰 삭제가 일어나지 않는다.
        assertThatThrownBy(() -> reviewService.delete(2L, 20L))
                .isInstanceOf(ReviewOwnershipException.class);

        verify(commentRepository, never()).deleteByReview(any(Review.class));
        verify(reviewRepository, never()).delete(any(Review.class));
    }

    @Test
    void updatesOnlyTitleByAuthor() {
        // Given: 작성자 본인의 기존 리뷰가 있다.
        Member author = member(1L, "author");
        Review review = review(20L, author, movie(10L, 550L, "Fight Club"));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(author));
        when(reviewRepository.findById(20L)).thenReturn(Optional.of(review));

        // When: 제목만 지정해 부분 수정한다.
        ReviewView result = reviewService.update(
                1L,
                20L,
                new ReviewUpdateCommand(true, "Changed", false, null, false, null)
        );

        // Then: 제목만 바뀌고 나머지 값은 유지된다.
        assertThat(result.title()).isEqualTo("Changed");
        assertThat(result.content()).isEqualTo("Content");
        assertThat(result.rating()).isEqualByComparingTo("4.0");
        assertThat(result.updatedAt()).isNotNull();
    }

    @Test
    void updatesOnlyContentByAuthor() {
        // Given: 작성자 본인의 기존 리뷰가 있다.
        // When: 본문만 지정해 부분 수정한다.
        ReviewView result = updateAsAuthor(
                new ReviewUpdateCommand(false, null, true, "Changed content", false, null)
        );

        // Then: 본문만 바뀌고 제목과 평점은 유지된다.
        assertThat(result.title()).isEqualTo("Title");
        assertThat(result.content()).isEqualTo("Changed content");
        assertThat(result.rating()).isEqualByComparingTo("4.0");
    }

    @Test
    void updatesOnlyRatingByAuthor() {
        // Given: 작성자 본인의 기존 리뷰가 있다.
        // When: 평점만 지정해 부분 수정한다.
        ReviewView result = updateAsAuthor(
                new ReviewUpdateCommand(false, null, false, null, true, new BigDecimal("4.5"))
        );

        // Then: 평점만 바뀌고 제목과 본문은 유지된다.
        assertThat(result.title()).isEqualTo("Title");
        assertThat(result.content()).isEqualTo("Content");
        assertThat(result.rating()).isEqualByComparingTo("4.5");
    }

    @Test
    void updatesTitleAndRatingWhileKeepingContent() {
        // Given: 작성자 본인의 기존 리뷰가 있다.
        // When: 제목과 평점만 지정해 부분 수정한다.
        ReviewView result = updateAsAuthor(
                new ReviewUpdateCommand(true, "Changed", false, null, true, new BigDecimal("4.5"))
        );

        // Then: 지정하지 않은 본문은 유지된다.
        assertThat(result.title()).isEqualTo("Changed");
        assertThat(result.content()).isEqualTo("Content");
        assertThat(result.rating()).isEqualByComparingTo("4.5");
    }

    @Test
    void rejectsEmptyUpdate() {
        // Given: 작성자 본인의 기존 리뷰가 있다.
        // When / Then: 어떤 필드도 지정하지 않으면 수정 요청은 거부된다.
        assertThatThrownBy(() -> updateAsAuthor(
                new ReviewUpdateCommand(false, null, false, null, false, null)
        )).isInstanceOf(InvalidReviewUpdateException.class);
    }

    @Test
    void rejectsBlankTitleUpdate() {
        // Given: 작성자 본인의 기존 리뷰가 있다.
        // When / Then: 공백 제목으로 수정하면 요청은 거부된다.
        assertThatThrownBy(() -> updateAsAuthor(
                new ReviewUpdateCommand(true, " ", false, null, false, null)
        )).isInstanceOf(InvalidReviewUpdateException.class);
    }

    @Test
    void rejectsBlankContentUpdate() {
        // Given: 작성자 본인의 기존 리뷰가 있다.
        // When / Then: 공백 본문으로 수정하면 요청은 거부된다.
        assertThatThrownBy(() -> updateAsAuthor(
                new ReviewUpdateCommand(false, null, true, " ", false, null)
        )).isInstanceOf(InvalidReviewUpdateException.class);
    }

    @Test
    void rejectsInvalidRatingUpdate() {
        // Given: 작성자 본인의 기존 리뷰가 있다.
        // When / Then: 0.5 단위가 아닌 평점으로 수정하면 도메인 검증에서 거부된다.
        assertThatThrownBy(() -> updateAsAuthor(
                new ReviewUpdateCommand(false, null, false, null, true, new BigDecimal("4.6"))
        )).isInstanceOf(InvalidReviewRatingException.class);
    }

    @Test
    void deletesCommentsBeforeReviewByAuthor() {
        // Given: 댓글이 연결될 수 있는 작성자 본인의 리뷰가 있다.
        Member author = member(1L, "author");
        Review review = review(20L, author, movie(10L, 550L, "Fight Club"));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(author));
        when(reviewRepository.findById(20L)).thenReturn(Optional.of(review));

        // When: 작성자가 리뷰를 삭제한다.
        reviewService.delete(1L, 20L);

        // Then: 외래 키 참조를 제거할 댓글 삭제가 리뷰 삭제보다 먼저 실행된다.
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
