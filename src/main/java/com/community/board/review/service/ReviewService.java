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
/**
 * 리뷰 작성·조회·수정·삭제 유스케이스를 조율한다.
 *
 * <p>작성 흐름은 현재 회원 확인 → 로컬 영화 확인 또는 TMDB 조회 후 최소 영화 정보 저장 →
 * 중복 리뷰 확인 → 리뷰 저장 순서다. 삭제 흐름에서는 외래 키 제약을 만족시키기 위해 소속 댓글을
 * 먼저 제거한다.</p>
 */
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
    /**
     * 현재 회원의 영화 리뷰를 생성한다. 외부 영화 조회는 로컬에 영화가 없을 때만 수행하며,
     * 같은 트랜잭션 안에서 실패하면 새 영화 저장도 함께 롤백된다.
     */
    public ReviewView create(
            Long memberId,
            Long tmdbId,
            String title,
            String content,
            BigDecimal rating
    ) {
        Member member = getMember(memberId);
        // 로컬에 이미 리뷰 대상 영화가 있으면 외부 API 호출을 피한다.
        Movie movie = movieRepository.findByTmdbId(tmdbId)
                .orElseGet(() -> saveMovie(movieClient.getMovie(tmdbId)));

        // 애플리케이션 선검사와 DB 유니크 제약을 함께 사용해 중복 생성을 막는다.
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
    /** 공개 리뷰 상세 조회. */
    public ReviewView get(Long reviewId) {
        return ReviewView.from(getReview(reviewId));
    }

    @Transactional(readOnly = true)
    /** 영화별 리뷰를 최신 작성순으로 페이지 조회한다. */
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
    /** 전체 공개 피드를 조회하며, 공백뿐인 검색어는 검색하지 않은 것과 동일하게 처리한다. */
    public ReviewFeedPage getFeed(String query, int page, int size) {
        String normalizedQuery = normalizeQuery(query);
        PageRequest pageRequest = PageRequest.of(page, size);
        return ReviewFeedPage.from(normalizedQuery == null
                ? reviewRepository.findFeed(pageRequest)
                : reviewRepository.searchFeed(normalizedQuery, pageRequest));
    }

    @Transactional(readOnly = true)
    /** 현재 회원이 작성한 리뷰를 최신순으로 페이지 조회한다. */
    public ReviewFeedPage getMyFeed(Long memberId, int page, int size) {
        getMember(memberId);
        return ReviewFeedPage.from(reviewRepository.findFeedByMemberId(
                memberId,
                PageRequest.of(page, size)
        ));
    }

    @Transactional
    /**
     * 작성자만 전달한 필드만 부분 수정한다. Controller가 아닌 이 계층에서 소유권을 다시 검증해
     * 클라이언트가 보낸 작성자 정보를 신뢰하지 않는다.
     */
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
    /** 작성자 요청에 따라 댓글을 먼저 Hard Delete하고 리뷰를 Hard Delete한다. */
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
        // 검색·상세 조회만으로는 저장하지 않고, 실제 리뷰 작성에 필요한 최소 정보만 보관한다.
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
