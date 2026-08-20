package com.community.board.movie.repository;

import com.community.board.PostgresRepositoryTest;
import com.community.board.movie.domain.Movie;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@PostgresRepositoryTest
class MovieRepositoryTests {

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private DataSource dataSource;

    @Test
    void savesMovie() {
        Movie movie = Movie.create(
                550L,
                "Fight Club",
                "/poster.jpg",
                LocalDate.of(1999, 10, 15)
        );

        Movie savedMovie = movieRepository.saveAndFlush(movie);

        assertThat(savedMovie.getId()).isNotNull();
        assertThat(savedMovie.getTmdbId()).isEqualTo(550L);
        assertThat(savedMovie.getTitle()).isEqualTo("Fight Club");
        assertThat(savedMovie.getPosterPath()).isEqualTo("/poster.jpg");
        assertThat(savedMovie.getReleaseDate()).isEqualTo(LocalDate.of(1999, 10, 15));
        assertThat(savedMovie.getCreatedAt()).isNotNull();
    }

    @Test
    void findsMovieByTmdbId() {
        movieRepository.saveAndFlush(Movie.create(
                680L,
                "Pulp Fiction",
                "/pulp-fiction.jpg",
                LocalDate.of(1994, 10, 14)
        ));
        entityManager.clear();

        Movie foundMovie = movieRepository.findByTmdbId(680L).orElseThrow();

        assertThat(foundMovie.getTitle()).isEqualTo("Pulp Fiction");
    }

    @Test
    void rejectsDuplicateTmdbId() {
        movieRepository.saveAndFlush(Movie.create(13L, "Forrest Gump", null, null));

        Movie duplicate = Movie.create(13L, "Duplicate Movie", "/duplicate.jpg", null);

        assertThatThrownBy(() -> movieRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void savesMovieWithoutPosterPath() {
        Movie savedMovie = movieRepository.saveAndFlush(Movie.create(
                155L,
                "The Dark Knight",
                null,
                LocalDate.of(2008, 7, 18)
        ));

        assertThat(savedMovie.getPosterPath()).isNull();
    }

    @Test
    void savesMovieWithoutReleaseDate() {
        Movie savedMovie = movieRepository.saveAndFlush(Movie.create(
                27205L,
                "Inception",
                "/inception.jpg",
                null
        ));

        assertThat(savedMovie.getReleaseDate()).isNull();
    }

    @Test
    void createsMovieTableWithRequiredPostgreSqlSchema() throws SQLException {
        Map<String, ColumnMetadata> columnsByName = readMovieColumns();

        assertThat(columnsByName.get("tmdb_id").dataType()).isEqualTo("bigint");
        assertThat(columnsByName.get("tmdb_id").isNullable()).isEqualTo("NO");
        assertThat(columnsByName.get("title").dataType()).isEqualTo("character varying");
        assertThat(columnsByName.get("title").isNullable()).isEqualTo("NO");
        assertThat(columnsByName.get("poster_path").dataType()).isEqualTo("character varying");
        assertThat(columnsByName.get("poster_path").isNullable()).isEqualTo("YES");
        assertThat(columnsByName.get("release_date").dataType()).isEqualTo("date");
        assertThat(columnsByName.get("release_date").isNullable()).isEqualTo("YES");

        ColumnMetadata createdAt = columnsByName.get("created_at");
        assertThat(createdAt.isNullable()).isEqualTo("NO");
        assertThat(createdAt.dataType()).isEqualTo("timestamp with time zone");
        assertThat(createdAt.udtName()).isEqualTo("timestamptz");

        assertThat(hasUniqueConstraintForTmdbId()).isTrue();

        System.out.printf(
                "PostgreSQL movie schema: table_schema=%s, table_name=%s, "
                        + "tmdb_id=%s/%s, title=%s/%s, poster_path=%s/%s, release_date=%s/%s, "
                        + "created_at=%s/%s/%s, tmdb_id_unique=true%n",
                createdAt.tableSchema(),
                createdAt.tableName(),
                columnsByName.get("tmdb_id").dataType(),
                columnsByName.get("tmdb_id").isNullable(),
                columnsByName.get("title").dataType(),
                columnsByName.get("title").isNullable(),
                columnsByName.get("poster_path").dataType(),
                columnsByName.get("poster_path").isNullable(),
                columnsByName.get("release_date").dataType(),
                columnsByName.get("release_date").isNullable(),
                createdAt.dataType(),
                createdAt.udtName(),
                createdAt.isNullable()
        );
    }

    private Map<String, ColumnMetadata> readMovieColumns() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet columns = statement.executeQuery("""
                     select table_schema, table_name, column_name, data_type, udt_name, is_nullable
                     from information_schema.columns
                     where table_schema = current_schema()
                       and table_name = 'movie'
                     order by ordinal_position
                     """)) {
            Map<String, ColumnMetadata> columnsByName = new HashMap<>();
            while (columns.next()) {
                ColumnMetadata metadata = new ColumnMetadata(
                        columns.getString("table_schema"),
                        columns.getString("table_name"),
                        columns.getString("column_name"),
                        columns.getString("data_type"),
                        columns.getString("udt_name"),
                        columns.getString("is_nullable")
                );
                columnsByName.put(metadata.columnName(), metadata);
            }
            return columnsByName;
        }
    }

    private boolean hasUniqueConstraintForTmdbId() throws SQLException {
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
                       and tc.table_name = 'movie'
                       and tc.constraint_type = 'UNIQUE'
                     group by tc.constraint_name
                     having count(*) = 1 and max(kcu.column_name) = 'tmdb_id'
                     """)) {
            return constraints.next();
        }
    }

    private record ColumnMetadata(
            String tableSchema,
            String tableName,
            String columnName,
            String dataType,
            String udtName,
            String isNullable
    ) {
    }
}
