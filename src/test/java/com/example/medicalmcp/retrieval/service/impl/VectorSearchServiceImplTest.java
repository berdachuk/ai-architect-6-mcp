package com.example.medicalmcp.retrieval.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.medicalmcp.core.util.IdGenerator;
import com.example.medicalmcp.embedding.service.EmbeddingService;
import com.example.medicalmcp.medicalcase.domain.CaseSummary;
import com.example.medicalmcp.medicalcase.repository.MedicalCaseRepository;
import com.example.medicalmcp.retrieval.config.RetrievalProperties;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VectorSearchServiceImplTest {

    private RetrievalProperties retrievalProperties;

    @BeforeEach
    void setUp() {
        retrievalProperties = new RetrievalProperties();
        retrievalProperties.setMaxLimit(50);
        retrievalProperties.setDefaultTopK(5);
        retrievalProperties.setSimilarityThreshold(0.70);
    }

    @Test
    void searchCasesUsesDefaultLimitWhenNull() {
        MedicalCaseRepository repository = mock(MedicalCaseRepository.class);
        EmbeddingService embeddingService = mock(EmbeddingService.class);
        VectorSearchServiceImpl service =
                new VectorSearchServiceImpl(repository, embeddingService, retrievalProperties);
        when(repository.fullTextSearch("pacemaker", null, null, 10)).thenReturn(List.of());

        service.searchCases("pacemaker", null, null, null);

        verify(repository).fullTextSearch("pacemaker", null, null, 10);
    }

    @Test
    void searchCasesClampsLimitToMax() {
        MedicalCaseRepository repository = mock(MedicalCaseRepository.class);
        EmbeddingService embeddingService = mock(EmbeddingService.class);
        VectorSearchServiceImpl service =
                new VectorSearchServiceImpl(repository, embeddingService, retrievalProperties);
        when(repository.fullTextSearch("pacemaker", null, null, 50))
                .thenReturn(List.of(new CaseSummary(IdGenerator.generateId(), "x", "y", "z", "k", "train")));

        List<CaseSummary> results = service.searchCases("pacemaker", null, null, 200);

        assertThat(results).hasSize(1);
        verify(repository).fullTextSearch("pacemaker", null, null, 50);
    }
}
