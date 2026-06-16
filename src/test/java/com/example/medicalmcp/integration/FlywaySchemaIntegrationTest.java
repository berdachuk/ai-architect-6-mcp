package com.example.medicalmcp.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class FlywaySchemaIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void extensionsAreInstalled() {
        assertThat(countExtension("vector")).isEqualTo(1);
        assertThat(countExtension("pg_trgm")).isEqualTo(1);
    }

    @Test
    void medicalCaseTableAndColumnsExist() {
        assertThat(tableExists("medical_case")).isTrue();
        assertThat(columnExists("medical_case", "embedding")).isTrue();
        assertThat(columnExists("medical_case", "fts")).isTrue();
    }

    @Test
    void embeddingColumnIs768Dimensions() {
        String type = jdbcTemplate.queryForObject(
                """
                SELECT format_type(a.atttypid, a.atttypmod)
                FROM pg_attribute a
                JOIN pg_class c ON a.attrelid = c.oid
                WHERE c.relname = 'medical_case' AND a.attname = 'embedding'
                """,
                String.class);
        assertThat(type).isEqualTo("vector(768)");
    }

    @Test
    void ftsColumnIsGeneratedStored() {
        String generated = jdbcTemplate.queryForObject(
                """
                SELECT attgenerated
                FROM pg_attribute
                WHERE attrelid = 'medical_case'::regclass
                  AND attname = 'fts'
                """,
                String.class);
        assertThat(generated).isEqualTo("s");
    }

    @Test
    void indexesExist() {
        assertThat(indexExists("idx_medical_case_embedding")).isTrue();
        assertThat(indexExists("idx_medical_case_fts")).isTrue();
        assertThat(indexExists("idx_medical_case_specialty")).isTrue();
        assertThat(indexExists("idx_medical_case_specialty_trgm")).isTrue();
    }

    @Test
    void generatedFtsPopulatesOnInsert() {
        jdbcTemplate.update(
                """
                INSERT INTO medical_case (sample_name, description, transcription, keywords)
                VALUES ('Chest pain case', 'Patient reports chest pain', 'Detailed note', 'pain,chest')
                """);

        String lexeme = jdbcTemplate.queryForObject(
                "SELECT fts::text FROM medical_case WHERE sample_name = 'Chest pain case'", String.class);
        assertThat(lexeme).contains("chest").contains("pain");
    }

    private int countExtension(String name) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_extension WHERE extname = ?", Integer.class, name);
        return count == null ? 0 : count;
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public' AND table_name = ?
                """,
                Integer.class,
                tableName);
        return count != null && count > 0;
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = ? AND column_name = ?
                """,
                Integer.class,
                tableName,
                columnName);
        return count != null && count > 0;
    }

    private boolean indexExists(String indexName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes WHERE indexname = ?", Integer.class, indexName);
        return count != null && count > 0;
    }
}
