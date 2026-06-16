package com.example.medicalmcp.promptlab.domain;

import java.util.List;
import java.util.Map;

public record ClassificationEvalOutcome(
        ClassificationEvalSummary summary, List<ClassificationEvalResult> results) {

    public static ClassificationEvalOutcome empty(String templateId, String split) {
        return new ClassificationEvalOutcome(
                new ClassificationEvalSummary(templateId, split, 0, 0, 0.0, Map.of()), List.of());
    }
}
