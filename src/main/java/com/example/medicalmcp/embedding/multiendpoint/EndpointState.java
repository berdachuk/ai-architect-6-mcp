package com.example.medicalmcp.embedding.multiendpoint;

import com.example.medicalmcp.embedding.service.EmbeddingHealth;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;

public class EndpointState {

    private static final List<String> PROBE = List.of("ping");

    private final String url;
    private final String model;
    private final EmbeddingModel embeddingModel;
    private final AtomicInteger completedCount = new AtomicInteger(0);
    private volatile long lastFailureTime;
    private volatile boolean skipped;

    public EndpointState(String url, String model, EmbeddingModel embeddingModel) {
        this.url = url;
        this.model = model;
        this.embeddingModel = embeddingModel;
    }

    public String getUrl() {
        return url;
    }

    public String getModel() {
        return model;
    }

    public EmbeddingModel getEmbeddingModel() {
        return embeddingModel;
    }

    public AtomicInteger getCompletedCount() {
        return completedCount;
    }

    public long getLastFailureTime() {
        return lastFailureTime;
    }

    public void setLastFailureTime(long lastFailureTime) {
        this.lastFailureTime = lastFailureTime;
    }

    public boolean isSkipped() {
        return skipped;
    }

    public void setSkipped(boolean skipped) {
        this.skipped = skipped;
    }

    public EmbeddingHealth ping() {
        long start = System.currentTimeMillis();
        try {
            EmbeddingResponse response = embeddingModel.embedForResponse(PROBE);
            int dimensions = response.getResults().isEmpty() ? 0 : response.getResults().get(0).getOutput().length;
            return new EmbeddingHealth(url, model, true, dimensions, System.currentTimeMillis() - start, null);
        } catch (Exception ex) {
            return new EmbeddingHealth(url, model, false, 0, System.currentTimeMillis() - start, ex.getMessage());
        }
    }
}
