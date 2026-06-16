package com.example.medicalmcp.embedding.multiendpoint;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.ai.embedding.EmbeddingModel;

public class EndpointState {

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
}
