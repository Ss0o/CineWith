package com.community.board.movie.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(
        name = "movie",
        uniqueConstraints = @UniqueConstraint(name = "uk_movie_tmdb_id", columnNames = "tmdb_id")
)
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tmdb_id", nullable = false)
    private Long tmdbId;

    @Column(nullable = false)
    private String title;

    @Column(name = "poster_path")
    private String posterPath;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "created_at", nullable = false)
    @JdbcTypeCode(SqlTypes.TIMESTAMP_WITH_TIMEZONE)
    private Instant createdAt;

    protected Movie() {
    }

    private Movie(
            Long tmdbId,
            String title,
            String posterPath,
            LocalDate releaseDate,
            Instant createdAt
    ) {
        this.tmdbId = Objects.requireNonNull(tmdbId, "tmdbId must not be null");
        this.title = Objects.requireNonNull(title, "title must not be null");
        this.posterPath = posterPath;
        this.releaseDate = releaseDate;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static Movie create(
            Long tmdbId,
            String title,
            String posterPath,
            LocalDate releaseDate
    ) {
        return new Movie(tmdbId, title, posterPath, releaseDate, Instant.now());
    }

    public Long getId() {
        return id;
    }

    public Long getTmdbId() {
        return tmdbId;
    }

    public String getTitle() {
        return title;
    }

    public String getPosterPath() {
        return posterPath;
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
