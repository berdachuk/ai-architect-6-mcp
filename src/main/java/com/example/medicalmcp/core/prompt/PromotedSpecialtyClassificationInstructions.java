package com.example.medicalmcp.core.prompt;

/**
 * Production promotion of prompt-lab winner {@value #TEMPLATE_ID} (M10).
 * Shared by {@code MedicalCasePrompts} and {@code PromptTemplateLibrary}.
 */
public final class PromotedSpecialtyClassificationInstructions {

    public static final String TEMPLATE_ID = "react_self_reflection";

    private PromotedSpecialtyClassificationInstructions() {}

    public static String classificationBlock() {
        return """
                Classify the medical case into exactly one of these labels (snake_case):
                %s

                Use only the case fields above. Do not invent clinical facts.

                Thought → Action → Observation → Reflection → Answer.
                End with: PREDICTED_LABEL: <label>
                """
                .formatted(MedicalSpecialtyLabels.allowedLabelsSnakeCase())
                .stripTrailing();
    }
}
