package com.example.medicalmcp.promptlab.domain;

import java.util.Map;

public record ClassificationEvalSummary(
        String templateId,
        String split,
        int total,
        int correct,
        double accuracy,
        Map<String, Long> perSpecialtyErrors) {

    public boolean meetsGate(double minimumAccuracy) {
        return total > 0 && accuracy >= minimumAccuracy;
    }
}
