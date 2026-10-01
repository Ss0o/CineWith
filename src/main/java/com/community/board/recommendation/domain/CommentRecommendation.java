package com.community.board.recommendation.domain;

import com.community.board.comment.domain.Comment;
import com.community.board.member.domain.Member;
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
@Table(name = "comment_recommendation", uniqueConstraints = @UniqueConstraint(
        name = "uk_comment_recommendation_member_comment", columnNames = {"member_id", "comment_id"}
))
public class CommentRecommendation {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    @Column(name = "created_at", nullable = false)
    @JdbcTypeCode(SqlTypes.TIMESTAMP_WITH_TIMEZONE)
    private Instant createdAt;

    protected CommentRecommendation() { }

    private CommentRecommendation(Member member, Comment comment) {
        this.member = Objects.requireNonNull(member, "member must not be null");
        this.comment = Objects.requireNonNull(comment, "comment must not be null");
        this.createdAt = Instant.now();
    }

    public static CommentRecommendation create(Member member, Comment comment) { return new CommentRecommendation(member, comment); }
    public Long getId() { return id; }
    public Member getMember() { return member; }
    public Comment getComment() { return comment; }
    public Instant getCreatedAt() { return createdAt; }
}
