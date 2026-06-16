package com.example.medicalmcp.promptlab.eval;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

public final class PredictedLabelExtractor {

    private static final Pattern PREDICTED_LABEL =
            Pattern.compile("PREDICTED_LABEL\\s*:\\s*(.+)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private PredictedLabelExtractor() {}

    public static Optional<String> extract(String modelOutput) {
        if (!StringUtils.hasText(modelOutput)) {
            return Optional.empty();
        }
        Matcher matcher = PREDICTED_LABEL.matcher(modelOutput);
        if (!matcher.find()) {
            return Optional.empty();
        }
        String label = matcher.group(1).trim();
        int newline = label.indexOf('\n');
        if (newline >= 0) {
            label = label.substring(0, newline).trim();
        }
        return StringUtils.hasText(label) ? Optional.of(label) : Optional.empty();
    }
}
