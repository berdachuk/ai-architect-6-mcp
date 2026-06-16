package com.example.medicalmcp.support;

import com.example.medicalmcp.embedding.multiendpoint.EmbeddingEndpointPool;
import com.example.medicalmcp.embedding.multiendpoint.EndpointState;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
public class TestEmbeddingConfiguration {

    @Bean
    @Primary
    EmbeddingEndpointPool testEmbeddingEndpointPool() {
        EmbeddingModel model = new DeterministicEmbeddingModel();
        EndpointState endpoint = new EndpointState("test://embedding", "test-model", model);
        return new EmbeddingEndpointPool(List.of(endpoint), List.of(1), 1, 10);
    }

    static final class DeterministicEmbeddingModel implements EmbeddingModel {

        private static final int DIMENSIONS = 768;

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            List<Embedding> embeddings = new ArrayList<>();
            int index = 0;
            for (String text : request.getInstructions()) {
                embeddings.add(new Embedding(toVector(text), index++));
            }
            return new EmbeddingResponse(embeddings);
        }

        @Override
        public float[] embed(Document document) {
            return toVector(document.getText());
        }

        private static float[] toVector(String text) {
            float[] vector = new float[DIMENSIONS];
            int hash = text == null ? 0 : text.hashCode();
            float norm = 0f;
            for (int i = 0; i < DIMENSIONS; i++) {
                vector[i] = (hash + i * 31) % 1000 / 1000f;
                norm += vector[i] * vector[i];
            }
            norm = (float) Math.sqrt(norm);
            if (norm > 0f) {
                for (int i = 0; i < DIMENSIONS; i++) {
                    vector[i] /= norm;
                }
            }
            return vector;
        }
    }
}
