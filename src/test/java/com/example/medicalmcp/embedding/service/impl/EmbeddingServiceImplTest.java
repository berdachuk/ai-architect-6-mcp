package com.example.medicalmcp.embedding.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.medicalmcp.embedding.multiendpoint.EmbeddingEndpointPool;
import com.example.medicalmcp.embedding.service.EmbeddingService;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class EmbeddingServiceImplTest {

    @Test
    void buildEmbeddingInputConcatenatesFields() {
        EmbeddingService service = new EmbeddingServiceImpl(mock(EmbeddingEndpointPool.class));

        String input = service.buildEmbeddingInput(
                "Pacemaker Interrogation", "ICD check", "cardiology, pacemaker");

        assertThat(input)
                .isEqualTo("Pacemaker Interrogation. ICD check cardiology, pacemaker");
    }

    @Test
    void embedAsFloatArrayUsesPool() {
        EmbeddingEndpointPool pool = mock(EmbeddingEndpointPool.class);
        when(pool.embed("test")).thenReturn(CompletableFuture.completedFuture(List.of(0.1, 0.2, 0.3)));
        EmbeddingService service = new EmbeddingServiceImpl(pool);

        float[] vector = service.embedAsFloatArray("test");

        assertThat(vector).containsExactly(0.1f, 0.2f, 0.3f);
    }
}
