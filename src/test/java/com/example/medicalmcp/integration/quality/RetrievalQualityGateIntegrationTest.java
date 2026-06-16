package com.example.medicalmcp.integration.quality;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.medicalmcp.dataset.service.DatasetLoaderService;
import com.example.medicalmcp.embedding.service.EmbeddingService;
import com.example.medicalmcp.integration.AbstractPostgresIntegrationTest;
import com.example.medicalmcp.medicalcase.repository.MedicalCaseRepository;
import com.example.medicalmcp.quality.QualityReport;
import com.example.medicalmcp.quality.QualityReportWriter;
import com.example.medicalmcp.quality.RetrievalQualityBenchmark;
import com.example.medicalmcp.retrieval.service.VectorSearchService;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

@Tag("quality")
@ActiveProfiles({"test", "quality"})
class RetrievalQualityGateIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String SPLIT = "test";

    @Autowired
    private DatasetLoaderService datasetLoaderService;

    @Autowired
    private MedicalCaseRepository medicalCaseRepository;

    @Autowired
    private VectorSearchService vectorSearchService;

    @Autowired
    private EmbeddingService embeddingService;

    @BeforeEach
    void loadFixture() {
        datasetLoaderService.loadIfEmpty();
    }

    @Test
    void retrievalQualityGateOnTestSplit() {
        int rows = medicalCaseRepository.fullTextSearch("patient", null, SPLIT, 50).size();
        assertThat(rows).isGreaterThan(0);

        RetrievalQualityBenchmark benchmark =
                new RetrievalQualityBenchmark(medicalCaseRepository, vectorSearchService, embeddingService);
        Map<String, Double> fts = benchmark.runFtsMetrics(SPLIT);
        Map<String, Double> semantic = benchmark.runSemanticMetrics(SPLIT);

        boolean passed = fts.get("sampleName_hit10") >= 0.95
                && fts.get("transcriptionPhrase_hit10") >= 0.70
                && fts.get("keyword_hit10") >= 0.80
                && semantic.get("self_at_1") >= 0.35
                && semantic.get("self_at_5") >= 0.65
                && semantic.get("specialty_at_5") >= 0.85
                && semantic.get("mean_similarity_at_1") >= 0.75;

        QualityReportWriter.write(new QualityReport(
                "hpe-ai/medical-cases-classification-tutorial",
                SPLIT,
                rows,
                "nomic-embed-text:v1.5",
                Instant.now(),
                fts,
                semantic,
                Map.of(
                        "specialty_count",
                        vectorSearchService.listSpecialties().size(),
                        "round_trip_pass",
                        true),
                passed));

        assertThat(fts.get("sampleName_hit10")).isGreaterThanOrEqualTo(0.95);
        assertThat(fts.get("transcriptionPhrase_hit10")).isGreaterThanOrEqualTo(0.70);
        assertThat(fts.get("keyword_hit10")).isGreaterThanOrEqualTo(0.80);
        assertThat(semantic.get("self_at_1")).isGreaterThanOrEqualTo(0.35);
        assertThat(semantic.get("self_at_5")).isGreaterThanOrEqualTo(0.65);
        assertThat(semantic.get("specialty_at_5")).isGreaterThanOrEqualTo(0.85);
        assertThat(semantic.get("mean_similarity_at_1")).isGreaterThanOrEqualTo(0.75);
        assertThat(QualityReportWriter.reportPath()).exists();
    }
}
