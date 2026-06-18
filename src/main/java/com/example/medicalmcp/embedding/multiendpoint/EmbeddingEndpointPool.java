package com.example.medicalmcp.embedding.multiendpoint;

import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import com.example.medicalmcp.embedding.service.EmbeddingHealth;

public class EmbeddingEndpointPool {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingEndpointPool.class);

    private final List<EndpointState> endpoints;
    private final BlockingQueue<EmbeddingTask> taskQueue;
    private final long skipDurationMs;
    private final int apiBatchSize;
    private final ExecutorService executor;
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final AtomicInteger totalCompleted = new AtomicInteger(0);

    public EmbeddingEndpointPool(
            List<EndpointState> endpoints, List<Integer> workersPerEndpoint, int skipDurationMin, int apiBatchSize) {
        this.endpoints = List.copyOf(endpoints);
        this.taskQueue = new LinkedBlockingQueue<>();
        this.skipDurationMs = TimeUnit.MINUTES.toMillis(skipDurationMin);
        this.apiBatchSize = Math.max(1, apiBatchSize);

        int totalWorkers = 0;
        for (int i = 0; i < endpoints.size(); i++) {
            int workers = i < workersPerEndpoint.size() ? Math.max(1, workersPerEndpoint.get(i)) : 1;
            totalWorkers += workers;
        }

        this.executor = Executors.newFixedThreadPool(totalWorkers, r -> {
            Thread thread = new Thread(r, "embedding-pool-worker");
            thread.setDaemon(false);
            return thread;
        });

        for (int i = 0; i < endpoints.size(); i++) {
            EndpointState endpoint = endpoints.get(i);
            int workers = i < workersPerEndpoint.size() ? Math.max(1, workersPerEndpoint.get(i)) : 1;
            for (int w = 0; w < workers; w++) {
                executor.submit(() -> runWorker(endpoint));
            }
        }

        log.info(
                "EmbeddingEndpointPool started with {} endpoints, {} workers, api-batch-size={}",
                endpoints.size(),
                totalWorkers,
                this.apiBatchSize);
    }

    public CompletableFuture<List<Double>> embed(String text) {
        EmbeddingTask task = new EmbeddingTask(text);
        taskQueue.offer(task);
        return task.getFutures().getFirst();
    }

    public List<EndpointState> getEndpoints() {
        return endpoints;
    }

    public List<EmbeddingHealth> pingAll() {
        List<EmbeddingHealth> results = new ArrayList<>(endpoints.size());
        for (EndpointState endpoint : endpoints) {
            results.add(endpoint.ping());
        }
        return results;
    }

    public List<CompletableFuture<List<Double>>> embedBatch(List<String> texts) {
        if (texts.isEmpty()) {
            return List.of();
        }
        if (apiBatchSize <= 1) {
            List<CompletableFuture<List<Double>>> futures = new ArrayList<>(texts.size());
            for (String text : texts) {
                EmbeddingTask task = new EmbeddingTask(text);
                taskQueue.offer(task);
                futures.add(task.getFutures().getFirst());
            }
            return futures;
        }

        List<CompletableFuture<List<Double>>> allFutures = new ArrayList<>(texts.size());
        for (int i = 0; i < texts.size(); i += apiBatchSize) {
            int end = Math.min(i + apiBatchSize, texts.size());
            List<String> subBatch = texts.subList(i, end);
            List<CompletableFuture<List<Double>>> subFutures = new ArrayList<>(subBatch.size());
            for (int j = 0; j < subBatch.size(); j++) {
                subFutures.add(new CompletableFuture<>());
            }
            EmbeddingTask task = new EmbeddingTask(subBatch, subFutures);
            taskQueue.offer(task);
            allFutures.addAll(subFutures);
        }
        return allFutures;
    }

    private void runWorker(EndpointState endpoint) {
        while (!shutdown.get()) {
            try {
                EmbeddingTask task = taskQueue.poll(1, TimeUnit.SECONDS);
                if (task == null) {
                    continue;
                }

                if (endpoint.isSkipped()) {
                    long elapsed = System.currentTimeMillis() - endpoint.getLastFailureTime();
                    if (elapsed >= skipDurationMs) {
                        endpoint.setSkipped(false);
                    } else {
                        taskQueue.offer(task);
                        Thread.sleep(Math.min(5000, skipDurationMs - elapsed));
                        continue;
                    }
                }

                try {
                    processTask(endpoint, task);
                } catch (Exception ex) {
                    log.warn("Endpoint {} failed: {}", endpoint.getUrl(), ex.getMessage());
                    endpoint.setLastFailureTime(System.currentTimeMillis());
                    endpoint.setSkipped(true);
                    taskQueue.offer(task);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void processTask(EndpointState endpoint, EmbeddingTask task) {
        List<String> texts = task.getTexts();
        List<CompletableFuture<List<Double>>> futures = task.getFutures();
        List<List<Double>> results = embedBatchWithModel(endpoint.getEmbeddingModel(), texts);

        for (int i = 0; i < futures.size(); i++) {
            List<Double> embedding = i < results.size() ? results.get(i) : List.of();
            futures.get(i).complete(embedding);
        }

        endpoint.getCompletedCount().addAndGet(texts.size());
        totalCompleted.addAndGet(texts.size());
    }

    private static List<List<Double>> embedBatchWithModel(EmbeddingModel model, List<String> texts) {
        if (texts.isEmpty()) {
            return List.of();
        }
        EmbeddingResponse response = model.embedForResponse(texts);
        List<List<Double>> results = new ArrayList<>(response.getResults().size());
        for (var embeddingResult : response.getResults()) {
            float[] output = embeddingResult.getOutput();
            List<Double> vector = new ArrayList<>(output.length);
            for (float value : output) {
                vector.add((double) value);
            }
            results.add(vector);
        }
        return results;
    }

    @PreDestroy
    public void shutdown() {
        if (shutdown.compareAndSet(false, true)) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException ex) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            log.info("EmbeddingEndpointPool shutdown complete");
        }
    }
}
