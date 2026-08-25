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

import java.math.BigDecimal;
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

    private static final BigDecimal MIN_RATING = new BigDecimal("0.5");
    private static final BigDecimal MAX_RATING = new BigDecimal("5.0");
    private static final BigDecimal RATING_STEP = new BigDecimal("0.5");

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

    @Column(nullable = false, precision = 2, scale = 1)
    private BigDecimal rating;

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
            BigDecimal rating,
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
            BigDecimal rating
    ) {
        return new Review(member, movie, title, content, rating, Instant.now());
    }

    public void update(String title, String content, BigDecimal rating) {
        if (title == null && content == null && rating == null) {
            throw new InvalidReviewUpdateException();
        }
        if (title != null) {
            if (title.isBlank()) {
                throw new InvalidReviewUpdateException();
            }
            this.title = title;
        }
        if (content != null) {
            if (content.isBlank()) {
                throw new InvalidReviewUpdateException();
            }
            this.content = content;
        }
        if (rating != null) {
            this.rating = validateRating(rating);
        }
        this.updatedAt = Instant.now();
    }

    private static BigDecimal validateRating(BigDecimal rating) {
        if (rating == null
                || rating.compareTo(MIN_RATING) < 0
                || rating.compareTo(MAX_RATING) > 0
                || rating.remainder(RATING_STEP).compareTo(BigDecimal.ZERO) != 0) {
            throw new InvalidReviewRatingException();
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

    public BigDecimal getRating() {
        return rating;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
