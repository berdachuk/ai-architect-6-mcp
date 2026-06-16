package com.example.medicalmcp.quality;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Map;

public record QualityReport(
        String dataset,
        String split,
        int rows,
        String embeddingModel,
        Instant timestamp,
        Map<String, Double> fts,
        Map<String, Double> semantic,
        Map<String, Object> integrity,
        @JsonProperty("passed") boolean passed) {}
