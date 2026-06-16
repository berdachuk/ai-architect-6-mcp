package com.example.medicalmcp.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.medicalmcp.dataset.service.DatasetLoaderService;
import com.example.medicalmcp.medicalcase.domain.CaseSummary;
import com.example.medicalmcp.medicalcase.domain.DatasetStats;
import com.example.medicalmcp.medicalcase.repository.MedicalCaseRepository;
import com.example.medicalmcp.retrieval.service.VectorSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(
        properties = {
            "medicalmcp.dataset.loader.enabled=false",
            "medicalmcp.dataset.loader.sources=classpath:dataset/train-sample-10.csv"
        })
class VectorSearchServiceIntegrationTest extends AbstractPostgresIntegrationTest {

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
    void getDatasetStatsAggregatesTotals() {
        DatasetStats stats = vectorSearchService.getDatasetStats();

        assertThat(stats.totalCases()).isEqualTo(10);
        assertThat(stats.bySplit()).containsEntry("train", 10L);
        assertThat(stats.bySpecialty()).hasSize(8);
        assertThat(stats.bySpecialty().values().stream().mapToLong(Long::longValue).sum())
                .isEqualTo(10);
    }

    @Test
    void searchCasesClampsLimitToMax() {
        assertThat(vectorSearchService.searchCases("patient", null, "train", 100)).hasSizeLessThanOrEqualTo(50);
    }

    @Test
    void cachedStatsReturnConsistentCounts() {
        DatasetStats first = vectorSearchService.getDatasetStats();
        DatasetStats second = vectorSearchService.getDatasetStats();

        assertThat(second.totalCases()).isEqualTo(first.totalCases());
        assertThat(second.bySplit()).isEqualTo(first.bySplit());
        assertThat(second.bySpecialty()).isEqualTo(first.bySpecialty());
    }

    @Test
    void searchCasesReturnsCaseSummaryWithoutTranscription() {
        CaseSummary summary =
                vectorSearchService.searchCases("Pacemaker Interrogation", null, null, 1).getFirst();

        assertThat(summary.sampleName()).isEqualTo("Pacemaker Interrogation");
        assertThat(summary.id()).isNotNull();
    }
}
