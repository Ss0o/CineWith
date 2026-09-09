package com.community.board.review.service;

import com.community.board.comment.repository.CommentRepository;
import com.community.board.member.domain.Member;
import com.community.board.member.repository.MemberRepository;
import com.community.board.member.service.MemberNotFoundException;
import com.community.board.movie.client.MovieClient;
import com.community.board.movie.client.model.MovieDetail;
import com.community.board.movie.domain.Movie;
import com.community.board.movie.repository.MovieRepository;
import com.community.board.review.domain.Review;
import com.community.board.review.domain.InvalidReviewUpdateException;
import com.community.board.review.repository.ReviewRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class ReviewService {

    private final MemberRepository memberRepository;
    private final MovieRepository movieRepository;
    private final ReviewRepository reviewRepository;
    private final CommentRepository commentRepository;
    private final MovieClient movieClient;

    public ReviewService(
            MemberRepository memberRepository,
            MovieRepository movieRepository,
            ReviewRepository reviewRepository,
            CommentRepository commentRepository,
            MovieClient movieClient
    ) {
        this.memberRepository = memberRepository;
        this.movieRepository = movieRepository;
        this.reviewRepository = reviewRepository;
        this.commentRepository = commentRepository;
        this.movieClient = movieClient;
    }

    @Transactional
    public ReviewView create(
            Long memberId,
            Long tmdbId,
            String title,
            String content,
            BigDecimal rating
    ) {
        Member member = getMember(memberId);
        Movie movie = movieRepository.findByTmdbId(tmdbId)
                .orElseGet(() -> saveMovie(movieClient.getMovie(tmdbId)));

        if (reviewRepository.existsByMemberAndMovie(member, movie)) {
            throw new DuplicateReviewException();
        }

        Review review = Review.create(
                member,
                movie,
                title,
                content,
                rating
        );
        try {
            return ReviewView.from(reviewRepository.saveAndFlush(review));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateReviewException(exception);
        }
    }

    @Transactional(readOnly = true)
    public ReviewView get(Long reviewId) {
        return ReviewView.from(getReview(reviewId));
    }

    @Transactional(readOnly = true)
    public ReviewPage getByMovie(Long tmdbId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        return ReviewPage.from(
                reviewRepository.findByMovieTmdbId(tmdbId, pageRequest).map(ReviewView::from)
        );
    }

    @Transactional(readOnly = true)
    public ReviewFeedPage getFeed(String query, int page, int size) {
        String normalizedQuery = normalizeQuery(query);
        PageRequest pageRequest = PageRequest.of(page, size);
        return ReviewFeedPage.from(normalizedQuery == null
                ? reviewRepository.findFeed(pageRequest)
                : reviewRepository.searchFeed(normalizedQuery, pageRequest));
    }

    @Transactional
    public ReviewView update(
            Long memberId,
            Long reviewId,
            ReviewUpdateCommand command
    ) {
        Member member = getMember(memberId);
        Review review = getReview(reviewId);
        verifyOwner(member, review);
        validateUpdate(command);
        review.update(
                command.titlePresent() ? command.title() : null,
                command.contentPresent() ? command.content() : null,
                command.ratingPresent() ? command.rating() : null
        );
        return ReviewView.from(review);
    }

    @Transactional
    public void delete(Long memberId, Long reviewId) {
        Member member = getMember(memberId);
        Review review = getReview(reviewId);
        verifyOwner(member, review);
        commentRepository.deleteByReview(review);
        reviewRepository.delete(review);
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);
    }

    private Review getReview(Long reviewId) {
        return reviewRepository.findById(reviewId).orElseThrow(ReviewNotFoundException::new);
    }

    private Movie saveMovie(MovieDetail detail) {
        return movieRepository.save(Movie.create(
                detail.tmdbId(),
                detail.title(),
                detail.posterPath(),
                detail.releaseDate()
        ));
    }

    private void verifyOwner(Member member, Review review) {
        if (!review.getMember().getId().equals(member.getId())) {
            throw new ReviewOwnershipException();
        }
    }

    private void validateUpdate(ReviewUpdateCommand command) {
        if (command == null
                || command.isEmpty()
                || command.titlePresent() && (command.title() == null || command.title().isBlank())
                || command.contentPresent() && (command.content() == null || command.content().isBlank())
                || command.ratingPresent() && command.rating() == null) {
            throw new InvalidReviewUpdateException();
        }
    }

    private String normalizeQuery(String query) {
        if (query == null) {
            return null;
        }
        String normalized = query.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
