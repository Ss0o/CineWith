package com.community.board.review.domain;

import com.community.board.member.domain.Member;
import com.community.board.movie.domain.Movie;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
        name = "review",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_review_member_movie",
                columnNames = {"member_id", "movie_id"}
        )
)
public class Review {

    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(nullable = false)
    private Integer rating;

    @Column(name = "created_at", nullable = false)
    @JdbcTypeCode(SqlTypes.TIMESTAMP_WITH_TIMEZONE)
    private Instant createdAt;

    @Column(name = "updated_at")
    @JdbcTypeCode(SqlTypes.TIMESTAMP_WITH_TIMEZONE)
    private Instant updatedAt;

    protected Review() {
    }

    private Review(
            Member member,
            Movie movie,
            String title,
            String content,
            Integer rating,
            Instant createdAt
    ) {
        this.member = Objects.requireNonNull(member, "member must not be null");
        this.movie = Objects.requireNonNull(movie, "movie must not be null");
        this.title = Objects.requireNonNull(title, "title must not be null");
        this.content = Objects.requireNonNull(content, "content must not be null");
        this.rating = validateRating(rating);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = null;
    }

    public static Review create(
            Member member,
            Movie movie,
            String title,
            String content,
            Integer rating
    ) {
        return new Review(member, movie, title, content, rating, Instant.now());
    }

    private static Integer validateRating(Integer rating) {
        Objects.requireNonNull(rating, "rating must not be null");
        if (rating < MIN_RATING || rating > MAX_RATING) {
            throw new IllegalArgumentException("rating must be between 1 and 10");
        }
        return rating;
    }

    public Long getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public Movie getMovie() {
        return movie;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public Integer getRating() {
        return rating;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
