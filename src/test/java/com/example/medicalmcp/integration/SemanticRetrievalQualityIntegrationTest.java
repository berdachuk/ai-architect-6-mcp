package com.example.medicalmcp.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.medicalmcp.dataset.service.DatasetLoaderService;
import com.example.medicalmcp.embedding.service.EmbeddingService;
import com.example.medicalmcp.medicalcase.domain.MedicalCase;
import com.example.medicalmcp.medicalcase.domain.SemanticMatch;
import com.example.medicalmcp.medicalcase.repository.MedicalCaseRepository;
import com.example.medicalmcp.retrieval.service.VectorSearchService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(
        properties = {
            "medicalmcp.dataset.loader.enabled=false",
            "medicalmcp.dataset.loader.sources=classpath:dataset/train-sample-10.csv",
            "medicalmcp.retrieval.max-limit=50"
        })
class SemanticRetrievalQualityIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private DatasetLoaderService datasetLoaderService;

    @Autowired
    private MedicalCaseRepository medicalCaseRepository;

    @Autowired
    private VectorSearchService vectorSearchService;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void loadFixture() {
        jdbcTemplate.update("DELETE FROM medical_case");
        datasetLoaderService.loadIfEmpty();
    }

    @Test
    void storedEmbeddingInputRanksSourceRowFirst() {
        UUID id = jdbcTemplate.queryForObject("SELECT id FROM medical_case LIMIT 1", UUID.class);
        MedicalCase sample = medicalCaseRepository.findById(id).orElseThrow();
        String query = embeddingService.buildEmbeddingInput(
                sample.sampleName(), sample.description(), sample.keywords());

        List<SemanticMatch> results = vectorSearchService.semanticSearch(query, null, 5, 0.0);

        assertThat(results).isNotEmpty();
        assertThat(results.getFirst().caseSummary().id()).isEqualTo(sample.id());
        assertThat(results.getFirst().similarity()).isGreaterThan(0.99);
    }

    @Test
    void specialtyFilterReturnsOnlyMatchingLabel() {
        List<SemanticMatch> results =
                vectorSearchService.semanticSearch("orthopedic knee joint", "Orthopedic", 10, -1.0);

        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(match -> "Orthopedic".equals(match.caseSummary().medicalSpecialty()));
    }
}
