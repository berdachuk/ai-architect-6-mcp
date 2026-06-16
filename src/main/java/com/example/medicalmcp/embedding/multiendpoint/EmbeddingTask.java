package com.example.medicalmcp.embedding.multiendpoint;

import java.util.List;
import java.util.concurrent.CompletableFuture;

class EmbeddingTask {

    private final List<String> texts;
    private final List<CompletableFuture<List<Double>>> futures;

    EmbeddingTask(String text) {
        this.texts = List.of(text);
        CompletableFuture<List<Double>> future = new CompletableFuture<>();
        this.futures = List.of(future);
    }

    EmbeddingTask(List<String> texts, List<CompletableFuture<List<Double>>> futures) {
        if (texts.size() != futures.size()) {
            throw new IllegalArgumentException("texts and futures must have same size");
        }
        this.texts = List.copyOf(texts);
        this.futures = List.copyOf(futures);
    }

    List<String> getTexts() {
        return texts;
    }

    List<CompletableFuture<List<Double>>> getFutures() {
        return futures;
    }
}
