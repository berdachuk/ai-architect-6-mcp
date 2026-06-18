package com.example.medicalmcp.embedding.service;

public record EmbeddingHealth(String url, String model, boolean up, int dimensions, long latencyMs, String error) {

    public boolean isUp() {
        return up;
    }
}
