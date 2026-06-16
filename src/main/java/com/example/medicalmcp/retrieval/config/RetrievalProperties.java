package com.example.medicalmcp.retrieval.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "medicalmcp.retrieval")
public class RetrievalProperties {

    @Min(1)
    private int maxLimit = 50;

    @Min(1)
    private int defaultTopK = 5;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double similarityThreshold = 0.70;

    @Min(1)
    private int statsCacheTtlSeconds = 60;

    public int getMaxLimit() {
        return maxLimit;
    }

    public void setMaxLimit(int maxLimit) {
        this.maxLimit = maxLimit;
    }

    public int getDefaultTopK() {
        return defaultTopK;
    }

    public void setDefaultTopK(int defaultTopK) {
        this.defaultTopK = defaultTopK;
    }

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public void setSimilarityThreshold(double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }

    public int getStatsCacheTtlSeconds() {
        return statsCacheTtlSeconds;
    }

    public void setStatsCacheTtlSeconds(int statsCacheTtlSeconds) {
        this.statsCacheTtlSeconds = statsCacheTtlSeconds;
    }
}
