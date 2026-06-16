package com.example.medicalmcp.promptlab.eval;

import com.example.medicalmcp.medicalcase.domain.MedicalCase;
import org.springframework.util.StringUtils;

public final class CaseTextFormatter {

    private CaseTextFormatter() {}

    public static String format(MedicalCase medicalCase) {
        StringBuilder text = new StringBuilder();
        text.append(medicalCase.sampleName()).append(". ").append(medicalCase.description()).append('\n');
        text.append(medicalCase.transcription());
        if (StringUtils.hasText(medicalCase.keywords())) {
            text.append("\nKeywords: ").append(medicalCase.keywords());
        }
        return text.toString();
    }
}
