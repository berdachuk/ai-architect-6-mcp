package com.example.medicalmcp.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.medicalmcp.dataset.service.DatasetLoaderService;
import com.example.medicalmcp.medicalcase.domain.CaseSummary;
import com.example.medicalmcp.medicalcase.repository.MedicalCaseRepository;
import com.example.medicalmcp.retrieval.service.VectorSearchService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
    private MedicalCaseRepository medicalCaseRepository;

    @BeforeEach
    void loadFixture() {
        datasetLoaderService.loadIfEmpty();
    }

    @Test
    void sampleNameQueriesHitSourceRow() {
        List<CaseSummary> rows = medicalCaseRepository.fullTextSearch("patient", null, "train", 50);

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

    private static boolean containsSourceRow(List<CaseSummary> results, String sourceId) {
        return results.stream().anyMatch(result -> sourceId.equals(result.id()));
    }
}
