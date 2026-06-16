package com.example.medicalmcp.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.medicalmcp.dataset.service.DatasetLoaderService;
import com.example.medicalmcp.medicalcase.domain.CaseSummary;
import com.example.medicalmcp.medicalcase.domain.MedicalCase;
import com.example.medicalmcp.retrieval.service.VectorSearchService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

/**
 * Subset of FTS quality gates (docs/04-testing.md §6.3) on the 10-row train fixture.
 */
@TestPropertySource(
        properties = {
            "medicalmcp.dataset.loader.enabled=false",
            "medicalmcp.dataset.loader.sources=classpath:dataset/train-sample-10.csv"
        })
class FtsRetrievalQualityIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private DatasetLoaderService datasetLoaderService;

    @Autowired
    private VectorSearchService vectorSearchService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void loadFixture() {
        jdbcTemplate.update("DELETE FROM medical_case");
        datasetLoaderService.loadIfEmpty();
    }

    @Test
    void sampleNameQueriesHitSourceRow() {
        List<MedicalCase> rows = jdbcTemplate.query(
                "SELECT id, sample_name, description, transcription, medical_specialty, keywords, split, created_at FROM medical_case",
                (rs, rowNum) -> new MedicalCase(
                        rs.getObject("id", UUID.class),
                        rs.getString("sample_name"),
                        rs.getString("description"),
                        rs.getString("transcription"),
                        rs.getString("medical_specialty"),
                        rs.getString("keywords"),
                        rs.getString("split"),
                        rs.getTimestamp("created_at").toInstant()));

        long hits = rows.stream()
                .filter(row -> containsSourceRow(
                        vectorSearchService.searchCases(row.sampleName(), null, null, 10), row.id()))
                .count();

        assertThat((double) hits / rows.size()).isGreaterThanOrEqualTo(0.95);
    }

    @Test
    void specialtyFilterReturnsOnlyMatchingLabel() {
        List<CaseSummary> results =
                vectorSearchService.searchCases("patient", "Orthopedic", null, 50);

        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(result -> "Orthopedic".equals(result.medicalSpecialty()));
    }

    private static boolean containsSourceRow(List<CaseSummary> results, UUID sourceId) {
        return results.stream().anyMatch(result -> sourceId.equals(result.id()));
    }
}
