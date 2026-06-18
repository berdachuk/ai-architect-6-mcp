package com.example.medicalmcp.embedding.config;

import com.example.medicalmcp.embedding.multiendpoint.EmbeddingEndpointPool;
import com.example.medicalmcp.embedding.multiendpoint.EndpointState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

@Configuration
@Profile("!test")
public class EmbeddingEndpointPoolConfig {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingEndpointPoolConfig.class);

    @Bean
    public EmbeddingEndpointPool embeddingEndpointPool(
            MultiEndpointEmbeddingProperties properties, Environment environment) {
        String embeddingApiKey = environment.getProperty("spring.ai.custom.embedding.api-key", "");
        String embeddingDimensions = environment.getProperty("spring.ai.custom.embedding.dimensions", "768");

        List<MultiEndpointEmbeddingProperties.EndpointConfig> sortedEndpoints =
                new ArrayList<>(properties.getEndpoints());
        sortedEndpoints.sort(Comparator.comparingInt(MultiEndpointEmbeddingProperties.EndpointConfig::getPriority));

        List<EndpointState> endpointStates = new ArrayList<>();
        List<Integer> workersPerEndpoint = new ArrayList<>();

        for (MultiEndpointEmbeddingProperties.EndpointConfig endpointConfig : sortedEndpoints) {
            if (endpointConfig.getUrl() == null || endpointConfig.getUrl().isBlank()) {
                log.warn("Skipping endpoint with empty URL");
                continue;
            }

            String baseUrl = normalizeOpenAiBaseUrl(endpointConfig.getUrl());
            OpenAiEmbeddingOptions.Builder optionsBuilder = OpenAiEmbeddingOptions.builder()
                    .baseUrl(baseUrl)
                    .apiKey(embeddingApiKey != null ? embeddingApiKey : "");
            if (endpointConfig.getModel() != null && !endpointConfig.getModel().isBlank()) {
                optionsBuilder.model(endpointConfig.getModel());
            }
            if (embeddingDimensions != null && !embeddingDimensions.isBlank()) {
                try {
                    optionsBuilder.dimensions(Integer.parseInt(embeddingDimensions));
                } catch (NumberFormatException ex) {
                    log.warn("Invalid embedding dimensions: {}", embeddingDimensions);
                }
            }

            OpenAiEmbeddingModel model = OpenAiEmbeddingModel.builder()
                    .metadataMode(MetadataMode.EMBED)
                    .options(optionsBuilder.build())
                    .build();
            endpointStates.add(new EndpointState(baseUrl, endpointConfig.getModel(), model));
            int workers = endpointConfig.getWorkers() != null
                    ? endpointConfig.getWorkers()
                    : properties.getWorkerPerEndpoint();
            workersPerEndpoint.add(Math.max(1, workers));
            log.info(
                    "Embedding endpoint {} model {} priority {} workers {}",
                    baseUrl,
                    endpointConfig.getModel(),
                    endpointConfig.getPriority(),
                    workers);
        }

        if (endpointStates.isEmpty()) {
            throw new IllegalStateException(
                    "No valid embedding endpoints configured in medicalmcp.embedding.multi-endpoint.endpoints");
        }

        return new EmbeddingEndpointPool(
                endpointStates, workersPerEndpoint, properties.getSkipDurationMin(), properties.getApiBatchSize());
    }

    static String normalizeOpenAiBaseUrl(String url) {
        if (url == null || url.isBlank()) {
            return url;
        }
        String normalized = url.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (!normalized.endsWith("/v1")) {
            normalized = normalized + "/v1";
        }
        return normalized;
    }
}
