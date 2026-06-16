package com.example.medicalmcp.quality;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class QualityReportWriter {

    private static final Path REPORT_PATH = Path.of("target", "test-output", "quality-report.json");

    private QualityReportWriter() {}

    public static void write(QualityReport report) {
        try {
            Files.createDirectories(REPORT_PATH.getParent());
            ObjectMapper mapper = new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            mapper.writerWithDefaultPrettyPrinter().writeValue(REPORT_PATH.toFile(), report);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write quality report to " + REPORT_PATH, ex);
        }
    }

    public static Path reportPath() {
        return REPORT_PATH;
    }
}
