package com.example.medicalmcp.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.medicalmcp.dataset.service.DatasetLoaderService;
import com.example.medicalmcp.embedding.service.EmbeddingService;
import com.example.medicalmcp.medicalcase.domain.CaseSummary;
import com.example.medicalmcp.medicalcase.repository.MedicalCaseRepository;
import com.example.medicalmcp.retrieval.service.VectorSearchService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/**
 * Verifies Flyway V1 schema via repository behavior (docs/04-testing.md §5.3).
 * No pg_catalog SQL — {@code @InjectSql} is limited to {@code repository/impl} (DEC-010).
 */
@TestPropertySource(
        properties = {
            "medicalmcp.dataset.loader.enabled=false",
            "medicalmcp.dataset.loader.sources=classpath:dataset/train-sample-10.csv"
        })
class FlywaySchemaIntegrationTest extends AbstractPostgresIntegrationTest {

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
    void flywayCreatesQueryableMedicalCaseTable() {
        assertThat(medicalCaseRepository.countAll()).isEqualTo(10);

        CaseSummary summary =
                medicalCaseRepository.fullTextSearch("Pacemaker Interrogation", null, null, 1).getFirst();
        assertThat(medicalCaseRepository.findById(summary.id())).isPresent();
    }

    @Test
    void vectorExtensionStores768DimensionalEmbeddings() {
        assertThat(medicalCaseRepository.findWithoutEmbeddings()).isEmpty();
        assertThat(embeddingService.embedAsFloatArray("schema probe")).hasSize(768);
        assertThat(vectorSearchService.semanticSearch("pacemaker device", null, 3, -1.0)).isNotEmpty();
    }

    @Test
    void generatedFtsColumnSupportsFullTextSearch() {
        List<CaseSummary> results = medicalCaseRepository.fullTextSearch("chest pain", null, null, 5);

        assertThat(results).isNotEmpty();
    }

    @Test
    void specialtyFilterUsesIndexedColumns() {
        List<CaseSummary> results =
                medicalCaseRepository.fullTextSearch("patient", "Orthopedic", null, 50);

        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(result -> "Orthopedic".equals(result.medicalSpecialty()));
    }
}
