package com.community.board.review.repository;

import com.community.board.member.domain.Member;
import com.community.board.movie.domain.Movie;
import com.community.board.review.domain.Review;
import com.community.board.review.service.ReviewFeedItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByMemberAndMovie(Member member, Movie movie);

    Page<Review> findByMovieTmdbId(Long tmdbId, Pageable pageable);

    @Query(value = """
                    select new com.community.board.review.service.ReviewFeedItem(
                        r.id, m.tmdbId, m.title, m.posterPath, member.nickname,
                        r.rating, r.title, r.content, r.createdAt
                    )
                    from Review r
                    join r.movie m
                    join r.member member
                    order by r.createdAt desc, r.id desc
                    """, countQuery = "select count(r.id) from Review r")
    Page<ReviewFeedItem> findFeed(Pageable pageable);

    @Query(
            value = """
                    select new com.community.board.review.service.ReviewFeedItem(
                        r.id, m.tmdbId, m.title, m.posterPath, member.nickname,
                        r.rating, r.title, r.content, r.createdAt
                    )
                    from Review r
                    join r.movie m
                    join r.member member
                    where lower(r.title) like concat('%', lower(:query), '%')
                       or lower(r.content) like concat('%', lower(:query), '%')
                       or lower(m.title) like concat('%', lower(:query), '%')
                    order by r.createdAt desc, r.id desc
                    """,
            countQuery = """
                    select count(r.id)
                    from Review r
                    join r.movie m
                    where lower(r.title) like concat('%', lower(:query), '%')
                       or lower(r.content) like concat('%', lower(:query), '%')
                       or lower(m.title) like concat('%', lower(:query), '%')
                    """
    )
    Page<ReviewFeedItem> searchFeed(@Param("query") String query, Pageable pageable);

    @Query("""
            select avg(r.rating) as averageRating, count(r.id) as reviewCount
            from Review r
            where r.movie.tmdbId = :tmdbId
            """)
    RatingSummary summarizeRatingsByTmdbId(@Param("tmdbId") Long tmdbId);

    @Query("""
            select r.rating as rating, count(r.id) as reviewCount
            from Review r
            where r.movie.tmdbId = :tmdbId
            group by r.rating
            order by r.rating
            """)
    List<RatingCount> countRatingsByTmdbId(@Param("tmdbId") Long tmdbId);
}
