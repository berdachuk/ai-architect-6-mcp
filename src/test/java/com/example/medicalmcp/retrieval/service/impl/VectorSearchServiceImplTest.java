package com.example.medicalmcp.retrieval.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.medicalmcp.embedding.service.EmbeddingService;
import com.example.medicalmcp.medicalcase.domain.CaseSummary;
import com.example.medicalmcp.medicalcase.repository.MedicalCaseRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VectorSearchServiceImplTest {

    @Test
    void searchCasesUsesDefaultLimitWhenNull() {
        MedicalCaseRepository repository = mock(MedicalCaseRepository.class);
        EmbeddingService embeddingService = mock(EmbeddingService.class);
        VectorSearchServiceImpl service = new VectorSearchServiceImpl(repository, embeddingService, 50);
        when(repository.fullTextSearch("pacemaker", null, null, 10)).thenReturn(List.of());

        service.searchCases("pacemaker", null, null, null);

        verify(repository).fullTextSearch("pacemaker", null, null, 10);
    }

    @Test
    void searchCasesClampsLimitToMax() {
        MedicalCaseRepository repository = mock(MedicalCaseRepository.class);
        EmbeddingService embeddingService = mock(EmbeddingService.class);
        VectorSearchServiceImpl service = new VectorSearchServiceImpl(repository, embeddingService, 50);
        when(repository.fullTextSearch("pacemaker", null, null, 50))
                .thenReturn(List.of(new CaseSummary(UUID.randomUUID(), "x", "y", "z", "k", "train")));

        List<CaseSummary> results = service.searchCases("pacemaker", null, null, 200);

        assertThat(results).hasSize(1);
        verify(repository).fullTextSearch("pacemaker", null, null, 50);
    }
}
