package com.example.medicalmcp.embedding.service.impl;

import com.example.medicalmcp.embedding.multiendpoint.EmbeddingEndpointPool;
import com.example.medicalmcp.embedding.service.EmbeddingHealth;
import com.example.medicalmcp.embedding.service.EmbeddingService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmbeddingServiceImpl implements EmbeddingService {

    private final EmbeddingEndpointPool pool;

    public EmbeddingServiceImpl(EmbeddingEndpointPool pool) {
        this.pool = pool;
    }

    @Override
    public float[] embedAsFloatArray(String text) {
        try {
            List<Double> embedding = pool.embed(text).join();
            return toFloatArray(embedding);
        } catch (Exception ex) {
            throw new IllegalStateException("Embedding request failed", ex);
        }
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        if (texts.isEmpty()) {
            return List.of();
        }
        List<CompletableFuture<List<Double>>> futures = pool.embedBatch(texts);
        List<float[]> results = new ArrayList<>(futures.size());
        for (CompletableFuture<List<Double>> future : futures) {
            results.add(toFloatArray(future.join()));
        }
        return results;
    }

    @Override
    public List<EmbeddingHealth> pingAll() {
        return pool.pingAll();
    }

    @Override
    public String buildEmbeddingInput(String sampleName, String description, String keywords) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(sampleName)) {
            builder.append(sampleName.trim());
        }
        if (StringUtils.hasText(description)) {
            if (!builder.isEmpty()) {
                builder.append(". ");
            }
            builder.append(description.trim());
        }
        if (StringUtils.hasText(keywords)) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(keywords.trim());
        }
        return builder.toString();
    }

    private static float[] toFloatArray(List<Double> embedding) {
        float[] result = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            result[i] = embedding.get(i).floatValue();
        }
        return result;
    }
}
