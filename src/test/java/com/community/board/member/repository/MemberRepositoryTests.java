package com.community.board.member.repository;

import com.community.board.PostgresRepositoryTest;
import com.community.board.member.domain.Member;
import com.community.board.member.domain.OAuthProvider;
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
class MemberRepositoryTests {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private DataSource dataSource;

    @Test
    void savesMember() {
        Member member = Member.create(
                OAuthProvider.GOOGLE,
                "google-sub-1",
                "member@example.com",
                "movieFan"
        );

        Member savedMember = memberRepository.saveAndFlush(member);

        assertThat(savedMember.getId()).isNotNull();
        assertThat(savedMember.getProvider()).isEqualTo(OAuthProvider.GOOGLE);
        assertThat(savedMember.getProviderId()).isEqualTo("google-sub-1");
        assertThat(savedMember.getEmail()).isEqualTo("member@example.com");
        assertThat(savedMember.getNickname()).isEqualTo("movieFan");
        assertThat(savedMember.getCreatedAt()).isNotNull();
    }

    @Test
    void findsMemberByProviderAndProviderId() {
        memberRepository.saveAndFlush(Member.create(
                OAuthProvider.GOOGLE,
                "google-sub-2",
                null,
                "reviewer"
        ));
        entityManager.clear();

        Member foundMember = memberRepository.findByProviderAndProviderId(
                        OAuthProvider.GOOGLE,
                        "google-sub-2"
                )
                .orElseThrow();

        assertThat(foundMember.getNickname()).isEqualTo("reviewer");
        assertThat(foundMember.getEmail()).isNull();
    }

    @Test
    void detectsExistingNickname() {
        memberRepository.saveAndFlush(Member.create(
                OAuthProvider.GOOGLE,
                "google-sub-for-nickname",
                null,
                "existingNickname"
        ));

        assertThat(memberRepository.existsByNickname("existingNickname")).isTrue();
        assertThat(memberRepository.existsByNickname("unusedNickname")).isFalse();
    }

    @Test
    void rejectsDuplicateNickname() {
        memberRepository.saveAndFlush(Member.create(
                OAuthProvider.GOOGLE,
                "google-sub-3",
                "first@example.com",
                "sameNickname"
        ));

        Member duplicate = Member.create(
                OAuthProvider.GOOGLE,
                "google-sub-4",
                "second@example.com",
                "sameNickname"
        );

        assertThatThrownBy(() -> memberRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsDuplicateProviderAndProviderId() {
        memberRepository.saveAndFlush(Member.create(
                OAuthProvider.GOOGLE,
                "duplicate-google-sub",
                "first@example.com",
                "firstNickname"
        ));

        Member duplicate = Member.create(
                OAuthProvider.GOOGLE,
                "duplicate-google-sub",
                "second@example.com",
                "secondNickname"
        );

        assertThatThrownBy(() -> memberRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void createsMemberCreatedAtAsPostgreSqlTimestampWithTimeZone() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet columns = statement.executeQuery("""
                     select table_schema, table_name, column_name, data_type, udt_name, is_nullable
                     from information_schema.columns
                     where table_schema = current_schema()
                     order by table_name, ordinal_position
                     """)) {
            Map<String, ColumnMetadata> metadataByQualifiedName = new HashMap<>();
            while (columns.next()) {
                ColumnMetadata metadata = new ColumnMetadata(
                        columns.getString("table_schema"),
                        columns.getString("table_name"),
                        columns.getString("column_name"),
                        columns.getString("data_type"),
                        columns.getString("udt_name"),
                        columns.getString("is_nullable")
                );
                metadataByQualifiedName.put(metadata.tableName() + "." + metadata.columnName(), metadata);
            }

            ColumnMetadata createdAt = metadataByQualifiedName.get("member.created_at");

            assertThat(createdAt).isNotNull();
            assertThat(createdAt.tableSchema()).isEqualTo("public");
            assertThat(createdAt.tableName()).isEqualTo("member");
            assertThat(createdAt.columnName()).isEqualTo("created_at");
            assertThat(createdAt.dataType()).isEqualTo("timestamp with time zone");
            assertThat(createdAt.udtName()).isEqualTo("timestamptz");
            assertThat(createdAt.isNullable()).isEqualTo("NO");

            assertThat(metadataByQualifiedName.get("member.provider").isNullable()).isEqualTo("NO");
            assertThat(metadataByQualifiedName.get("member.provider_id").isNullable()).isEqualTo("NO");
            assertThat(metadataByQualifiedName.get("member.nickname").isNullable()).isEqualTo("NO");

            System.out.printf(
                    "PostgreSQL schema metadata: table_schema=%s, table_name=%s, column_name=%s, "
                            + "data_type=%s, udt_name=%s, is_nullable=%s%n",
                    createdAt.tableSchema(),
                    createdAt.tableName(),
                    createdAt.columnName(),
                    createdAt.dataType(),
                    createdAt.udtName(),
                    createdAt.isNullable()
            );
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
