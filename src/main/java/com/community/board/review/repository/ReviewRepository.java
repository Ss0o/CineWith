package com.community.board.review.repository;

import com.community.board.member.domain.Member;
import com.community.board.movie.domain.Movie;
import com.community.board.review.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByMemberAndMovie(Member member, Movie movie);

    Page<Review> findByMovieTmdbId(Long tmdbId, Pageable pageable);
}
