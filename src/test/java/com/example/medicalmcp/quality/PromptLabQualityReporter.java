package com.example.medicalmcp.quality;

import com.example.medicalmcp.promptlab.domain.ClassificationEvalSummary;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Map;

public final class PromptLabQualityReporter {

    private PromptLabQualityReporter() {}

    public static void mergePromptLabMetrics(ClassificationEvalSummary summary, double minAccuracy) {
        try {
            Files.createDirectories(QualityReportWriter.reportPath().getParent());
            ObjectMapper mapper = new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            ObjectNode root;
            if (Files.exists(QualityReportWriter.reportPath())) {
                root = (ObjectNode) mapper.readTree(QualityReportWriter.reportPath().toFile());
            } else {
                root = mapper.createObjectNode();
                root.put("timestamp", Instant.now().toString());
            }

            ObjectNode promptLab = mapper.createObjectNode();
            promptLab.put("templateId", summary.templateId());
            promptLab.put("split", summary.split());
            promptLab.put("total", summary.total());
            promptLab.put("correct", summary.correct());
            promptLab.put("accuracy", summary.accuracy());
            promptLab.put("minAccuracy", minAccuracy);
            promptLab.put("passed", summary.meetsGate(minAccuracy));
            promptLab.set("perSpecialtyErrors", mapper.valueToTree(summary.perSpecialtyErrors()));

            root.set("promptLab", promptLab);
            boolean retrievalPassed = !root.has("passed") || root.get("passed").asBoolean(true);
            root.put("passed", retrievalPassed && summary.meetsGate(minAccuracy));

            mapper.writerWithDefaultPrettyPrinter().writeValue(QualityReportWriter.reportPath().toFile(), root);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to merge prompt-lab metrics into quality report", ex);
        }
    }
}
