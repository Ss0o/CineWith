package com.community.board.comment.repository;

import com.community.board.PostgresRepositoryTest;
import com.community.board.comment.domain.Comment;
import com.community.board.member.domain.Member;
import com.community.board.member.domain.OAuthProvider;
import com.community.board.member.repository.MemberRepository;
import com.community.board.movie.domain.Movie;
import com.community.board.movie.repository.MovieRepository;
import com.community.board.review.domain.Review;
import com.community.board.review.repository.ReviewRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@PostgresRepositoryTest
class CommentRepositoryTests {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private DataSource dataSource;

    @Test
    void savesCommentWithMemberAndReview() {
        Member member = saveMember("comment-save-sub", "commentSaver");
        Review review = saveReview(member, 550L);

        Comment savedComment = commentRepository.saveAndFlush(
                Comment.create(member, review, "Comment content")
        );
        Long commentId = savedComment.getId();
        entityManager.clear();

        Comment foundComment = commentRepository.findById(commentId).orElseThrow();

        assertThat(foundComment.getMember().getId()).isEqualTo(member.getId());
        assertThat(foundComment.getReview().getId()).isEqualTo(review.getId());
        assertThat(foundComment.getContent()).isEqualTo("Comment content");
        assertThat(foundComment.getCreatedAt()).isNotNull();
        assertThat(foundComment.getUpdatedAt()).isNull();
    }

    @Test
    void allowsDifferentMembersToCommentOnSameReview() {
        Member reviewAuthor = saveMember("review-author-sub", "reviewAuthor");
        Member firstCommenter = saveMember("first-commenter-sub", "firstCommenter");
        Member secondCommenter = saveMember("second-commenter-sub", "secondCommenter");
        Review review = saveReview(reviewAuthor, 680L);

        commentRepository.saveAndFlush(Comment.create(firstCommenter, review, "First comment"));
        commentRepository.saveAndFlush(Comment.create(secondCommenter, review, "Second comment"));

        assertThat(commentRepository.count()).isEqualTo(2);
    }

    @Test
    void allowsMemberToWriteMultipleCommentsOnSameReview() {
        Member member = saveMember("multi-comment-sub", "multiCommenter");
        Review review = saveReview(member, 13L);

        commentRepository.saveAndFlush(Comment.create(member, review, "First comment"));
        commentRepository.saveAndFlush(Comment.create(member, review, "Second comment"));

        assertThat(commentRepository.count()).isEqualTo(2);
    }

    @Test
    void hardDeletesComment() {
        Member member = saveMember("delete-comment-sub", "commentDeleter");
        Review review = saveReview(member, 155L);
        Comment comment = commentRepository.saveAndFlush(Comment.create(member, review, "Delete me"));
        Long commentId = comment.getId();

        commentRepository.delete(comment);
        commentRepository.flush();
        entityManager.clear();

        assertThat(commentRepository.findById(commentId)).isEmpty();
    }

    @Test
    void createsCommentTableWithRequiredPostgreSqlSchema() throws SQLException {
        Map<String, ColumnMetadata> columnsByName = readCommentColumns();

        assertThat(columnsByName.get("member_id").isNullable()).isEqualTo("NO");
        assertThat(columnsByName.get("review_id").isNullable()).isEqualTo("NO");
        assertThat(columnsByName.get("content").dataType()).isEqualTo("text");
        assertThat(columnsByName.get("content").isNullable()).isEqualTo("NO");

        ColumnMetadata createdAt = columnsByName.get("created_at");
        assertThat(createdAt.dataType()).isEqualTo("timestamp with time zone");
        assertThat(createdAt.udtName()).isEqualTo("timestamptz");
        assertThat(createdAt.isNullable()).isEqualTo("NO");

        ColumnMetadata updatedAt = columnsByName.get("updated_at");
        assertThat(updatedAt.dataType()).isEqualTo("timestamp with time zone");
        assertThat(updatedAt.udtName()).isEqualTo("timestamptz");
        assertThat(updatedAt.isNullable()).isEqualTo("YES");

        Map<String, ForeignKeyMetadata> foreignKeysByColumn = readCommentForeignKeys();
        assertThat(foreignKeysByColumn.get("member_id"))
                .isEqualTo(new ForeignKeyMetadata("member", "id"));
        assertThat(foreignKeysByColumn.get("review_id"))
                .isEqualTo(new ForeignKeyMetadata("review", "id"));

        System.out.printf(
                "PostgreSQL comment schema: member_id=NO/FK-member.id, review_id=NO/FK-review.id, "
                        + "content=text/NO, created_at=%s/%s/%s, updated_at=%s/%s/%s%n",
                createdAt.dataType(),
                createdAt.udtName(),
                createdAt.isNullable(),
                updatedAt.dataType(),
                updatedAt.udtName(),
                updatedAt.isNullable()
        );
    }

    private Member saveMember(String providerId, String nickname) {
        return memberRepository.saveAndFlush(
                Member.create(OAuthProvider.GOOGLE, providerId, null, nickname)
        );
    }

    private Review saveReview(Member member, Long tmdbId) {
        Movie movie = movieRepository.saveAndFlush(Movie.create(tmdbId, "Movie " + tmdbId, null, null));
        return reviewRepository.saveAndFlush(Review.create(member, movie, "Review", "Content", 8));
    }

    private Map<String, ColumnMetadata> readCommentColumns() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet columns = statement.executeQuery("""
                     select column_name, data_type, udt_name, is_nullable
                     from information_schema.columns
                     where table_schema = current_schema()
                       and table_name = 'comment'
                     order by ordinal_position
                     """)) {
            Map<String, ColumnMetadata> columnsByName = new HashMap<>();
            while (columns.next()) {
                columnsByName.put(
                        columns.getString("column_name"),
                        new ColumnMetadata(
                                columns.getString("data_type"),
                                columns.getString("udt_name"),
                                columns.getString("is_nullable")
                        )
                );
            }
            return columnsByName;
        }
    }

    private Map<String, ForeignKeyMetadata> readCommentForeignKeys() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet foreignKeys = statement.executeQuery("""
                     select kcu.column_name, ccu.table_name as referenced_table, ccu.column_name as referenced_column
                     from information_schema.table_constraints tc
                     join information_schema.key_column_usage kcu
                       on tc.constraint_catalog = kcu.constraint_catalog
                      and tc.constraint_schema = kcu.constraint_schema
                      and tc.constraint_name = kcu.constraint_name
                     join information_schema.constraint_column_usage ccu
                       on tc.constraint_catalog = ccu.constraint_catalog
                      and tc.constraint_schema = ccu.constraint_schema
                      and tc.constraint_name = ccu.constraint_name
                     where tc.table_schema = current_schema()
                       and tc.table_name = 'comment'
                       and tc.constraint_type = 'FOREIGN KEY'
                     """)) {
            Map<String, ForeignKeyMetadata> foreignKeysByColumn = new HashMap<>();
            while (foreignKeys.next()) {
                foreignKeysByColumn.put(
                        foreignKeys.getString("column_name"),
                        new ForeignKeyMetadata(
                                foreignKeys.getString("referenced_table"),
                                foreignKeys.getString("referenced_column")
                        )
                );
            }
            return foreignKeysByColumn;
        }
    }

    private record ColumnMetadata(String dataType, String udtName, String isNullable) {
    }

    private record ForeignKeyMetadata(String referencedTable, String referencedColumn) {
    }
}
