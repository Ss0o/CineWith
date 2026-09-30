package com.community.board.recommendation.domain;

import com.community.board.member.domain.Member;
import com.community.board.review.domain.Review;
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
@Table(name = "review_recommendation", uniqueConstraints = @UniqueConstraint(
        name = "uk_review_recommendation_member_review", columnNames = {"member_id", "review_id"}
))
public class ReviewRecommendation {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @Column(name = "created_at", nullable = false)
    @JdbcTypeCode(SqlTypes.TIMESTAMP_WITH_TIMEZONE)
    private Instant createdAt;

    protected ReviewRecommendation() { }

    private ReviewRecommendation(Member member, Review review) {
        this.member = Objects.requireNonNull(member, "member must not be null");
        this.review = Objects.requireNonNull(review, "review must not be null");
        this.createdAt = Instant.now();
    }

    public static ReviewRecommendation create(Member member, Review review) { return new ReviewRecommendation(member, review); }
    public Long getId() { return id; }
    public Member getMember() { return member; }
    public Review getReview() { return review; }
    public Instant getCreatedAt() { return createdAt; }
}
