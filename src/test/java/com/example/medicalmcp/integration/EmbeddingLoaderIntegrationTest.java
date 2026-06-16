package com.example.medicalmcp.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.medicalmcp.dataset.service.DatasetLoaderService;
import com.example.medicalmcp.embedding.service.EmbeddingService;
import com.example.medicalmcp.medicalcase.repository.MedicalCaseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(
        properties = {
            "medicalmcp.dataset.loader.enabled=false",
            "medicalmcp.dataset.loader.sources=classpath:dataset/train-sample-10.csv"
        })
class EmbeddingLoaderIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private DatasetLoaderService datasetLoaderService;

    @Autowired
    private MedicalCaseRepository medicalCaseRepository;

    @Autowired
    private EmbeddingService embeddingService;

    @Test
    void passTwoFillsEmbeddingsForLoadedRows() {
        datasetLoaderService.loadIfEmpty();

        assertThat(medicalCaseRepository.countAll()).isEqualTo(10);
        assertThat(medicalCaseRepository.findWithoutEmbeddings()).isEmpty();
    }

    @Test
    void embedPassIsIdempotentWhenVectorsExist() {
        datasetLoaderService.loadIfEmpty();
        datasetLoaderService.loadIfEmpty();

        assertThat(medicalCaseRepository.findWithoutEmbeddings()).isEmpty();
        assertThat(embeddingService.embedAsFloatArray("probe")).hasSize(768);
    }
}
