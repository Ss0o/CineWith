package com.community.board.review.repository;

import com.community.board.member.domain.Member;
import com.community.board.movie.domain.Movie;
import com.community.board.review.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByMemberAndMovie(Member member, Movie movie);
}
