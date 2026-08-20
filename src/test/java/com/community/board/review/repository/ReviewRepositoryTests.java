package com.community.board.review.repository;

import com.community.board.PostgresRepositoryTest;
import com.community.board.member.domain.Member;
import com.community.board.member.domain.OAuthProvider;
import com.community.board.member.repository.MemberRepository;
import com.community.board.movie.domain.Movie;
import com.community.board.movie.repository.MovieRepository;
import com.community.board.review.domain.Review;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@PostgresRepositoryTest
class ReviewRepositoryTests {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private DataSource dataSource;

    @Test
    void savesReviewWithMemberAndMovie() {
        Member member = saveMember("review-save-sub", "reviewSaver");
        Movie movie = saveMovie(550L, "Fight Club");

        Review savedReview = reviewRepository.saveAndFlush(
                Review.create(member, movie, "Great movie", "My review", 9)
        );
        Long reviewId = savedReview.getId();
        entityManager.clear();

        Review foundReview = reviewRepository.findById(reviewId).orElseThrow();

        assertThat(foundReview.getMember().getId()).isEqualTo(member.getId());
        assertThat(foundReview.getMovie().getId()).isEqualTo(movie.getId());
        assertThat(foundReview.getTitle()).isEqualTo("Great movie");
        assertThat(foundReview.getContent()).isEqualTo("My review");
        assertThat(foundReview.getRating()).isEqualTo(9);
        assertThat(foundReview.getCreatedAt()).isNotNull();
        assertThat(foundReview.getUpdatedAt()).isNull();
    }

    @Test
    void findsExistingReviewByMemberAndMovie() {
        Member member = saveMember("exists-sub", "existsReviewer");
        Movie movie = saveMovie(680L, "Pulp Fiction");
        reviewRepository.saveAndFlush(Review.create(member, movie, "Title", "Content", 8));

        assertThat(reviewRepository.existsByMemberAndMovie(member, movie)).isTrue();
    }

    @Test
    void rejectsDuplicateMemberAndMovie() {
        Member member = saveMember("duplicate-review-sub", "duplicateReviewer");
        Movie movie = saveMovie(13L, "Forrest Gump");
        reviewRepository.saveAndFlush(Review.create(member, movie, "First", "First review", 7));

        Review duplicate = Review.create(member, movie, "Second", "Second review", 8);

        assertThatThrownBy(() -> reviewRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void allowsDifferentMembersToReviewSameMovie() {
        Member firstMember = saveMember("first-member-sub", "firstReviewer");
        Member secondMember = saveMember("second-member-sub", "secondReviewer");
        Movie movie = saveMovie(155L, "The Dark Knight");

        reviewRepository.saveAndFlush(Review.create(firstMember, movie, "First", "First review", 10));
        reviewRepository.saveAndFlush(Review.create(secondMember, movie, "Second", "Second review", 9));

        assertThat(reviewRepository.count()).isEqualTo(2);
    }

    @Test
    void allowsMemberToReviewDifferentMovies() {
        Member member = saveMember("multi-movie-sub", "multiMovieReviewer");
        Movie firstMovie = saveMovie(27205L, "Inception");
        Movie secondMovie = saveMovie(157336L, "Interstellar");

        reviewRepository.saveAndFlush(Review.create(member, firstMovie, "First", "First review", 9));
        reviewRepository.saveAndFlush(Review.create(member, secondMovie, "Second", "Second review", 10));

        assertThat(reviewRepository.count()).isEqualTo(2);
    }

    @Test
    void createsReviewTableWithRequiredPostgreSqlSchema() throws SQLException {
        Map<String, ColumnMetadata> columnsByName = readReviewColumns();

        assertThat(columnsByName.get("member_id").isNullable()).isEqualTo("NO");
        assertThat(columnsByName.get("movie_id").isNullable()).isEqualTo("NO");
        assertThat(columnsByName.get("title").isNullable()).isEqualTo("NO");
        assertThat(columnsByName.get("content").dataType()).isEqualTo("text");
        assertThat(columnsByName.get("content").isNullable()).isEqualTo("NO");
        assertThat(columnsByName.get("rating").isNullable()).isEqualTo("NO");

        ColumnMetadata createdAt = columnsByName.get("created_at");
        assertThat(createdAt.dataType()).isEqualTo("timestamp with time zone");
        assertThat(createdAt.udtName()).isEqualTo("timestamptz");
        assertThat(createdAt.isNullable()).isEqualTo("NO");

        ColumnMetadata updatedAt = columnsByName.get("updated_at");
        assertThat(updatedAt.dataType()).isEqualTo("timestamp with time zone");
        assertThat(updatedAt.udtName()).isEqualTo("timestamptz");
        assertThat(updatedAt.isNullable()).isEqualTo("YES");

        Map<String, ForeignKeyMetadata> foreignKeysByColumn = readReviewForeignKeys();
        assertThat(foreignKeysByColumn.get("member_id"))
                .isEqualTo(new ForeignKeyMetadata("member", "id"));
        assertThat(foreignKeysByColumn.get("movie_id"))
                .isEqualTo(new ForeignKeyMetadata("movie", "id"));

        assertThat(hasUniqueMemberAndMovieConstraint()).isTrue();

        System.out.printf(
                "PostgreSQL review schema: member_id=NO/FK-member.id, movie_id=NO/FK-movie.id, "
                        + "title=NO, content=text/NO, rating=NO, created_at=%s/%s/%s, "
                        + "updated_at=%s/%s/%s, member_movie_unique=true%n",
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

    private Movie saveMovie(Long tmdbId, String title) {
        return movieRepository.saveAndFlush(Movie.create(tmdbId, title, null, null));
    }

    private Map<String, ColumnMetadata> readReviewColumns() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet columns = statement.executeQuery("""
                     select column_name, data_type, udt_name, is_nullable
                     from information_schema.columns
                     where table_schema = current_schema()
                       and table_name = 'review'
                     order by ordinal_position
                     """)) {
            Map<String, ColumnMetadata> columnsByName = new HashMap<>();
            while (columns.next()) {
                ColumnMetadata metadata = new ColumnMetadata(
                        columns.getString("data_type"),
                        columns.getString("udt_name"),
                        columns.getString("is_nullable")
                );
                columnsByName.put(columns.getString("column_name"), metadata);
            }
            return columnsByName;
        }
    }

    private Map<String, ForeignKeyMetadata> readReviewForeignKeys() throws SQLException {
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
                       and tc.table_name = 'review'
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

    private boolean hasUniqueMemberAndMovieConstraint() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet constraints = statement.executeQuery("""
                     select tc.constraint_name
                     from information_schema.table_constraints tc
                     join information_schema.key_column_usage kcu
                       on tc.constraint_catalog = kcu.constraint_catalog
                      and tc.constraint_schema = kcu.constraint_schema
                      and tc.constraint_name = kcu.constraint_name
                     where tc.table_schema = current_schema()
                       and tc.table_name = 'review'
                       and tc.constraint_type = 'UNIQUE'
                     group by tc.constraint_name
                     having count(*) = 2
                        and count(*) filter (where kcu.column_name in ('member_id', 'movie_id')) = 2
                     """)) {
            return constraints.next();
        }
    }

    private record ColumnMetadata(String dataType, String udtName, String isNullable) {
    }

    private record ForeignKeyMetadata(String referencedTable, String referencedColumn) {
    }
}
