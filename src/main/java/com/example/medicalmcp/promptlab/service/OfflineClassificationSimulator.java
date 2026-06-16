package com.example.medicalmcp.promptlab.service;

import com.example.medicalmcp.medicalcase.domain.MedicalCase;
import com.example.medicalmcp.promptlab.normalization.SpecialtyLabelNormalizer;

public final class OfflineClassificationSimulator {

    private OfflineClassificationSimulator() {}

    /** Simulates a well-formed classifier that always emits the gold label. */
    public static String accurateOutput(MedicalCase medicalCase) {
        String snake = SpecialtyLabelNormalizer.toSnakeCase(medicalCase.medicalSpecialty());
        return "Thought: case reviewed\nAnswer: PREDICTED_LABEL: " + snake;
    }

    /** Simulates the naive template that omits the required output contract. */
    public static String badOutput(MedicalCase medicalCase) {
        return medicalCase.medicalSpecialty();
    }
}
