package com.example.medicalmcp.promptlab.template;

import com.example.medicalmcp.core.prompt.MedicalSpecialtyLabels;
import com.example.medicalmcp.core.prompt.PromotedSpecialtyClassificationInstructions;
import com.example.medicalmcp.promptlab.domain.PromptTemplate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class PromptTemplateLibrary {

    private static final String ALLOWED_LABELS = MedicalSpecialtyLabels.allowedLabelsSnakeCase();

    private static final Map<String, PromptTemplate> TEMPLATES = List.of(
                    new PromptTemplate(
                            "bad",
                            "Naive classifier",
                            """
                            Classify the medical case specialty.
                            Answer with the specialty name.
                            """),
                    new PromptTemplate(
                            "react",
                            "ReAct classifier",
                            """
                            Classify the medical case into exactly one of these labels (snake_case):
                            %s

                            Use ReAct format:
                            Thought: ...
                            Action: classify
                            Observation: ...
                            Answer: PREDICTED_LABEL: <label>
                            """.formatted(ALLOWED_LABELS)),
                    new PromptTemplate(
                            PromotedSpecialtyClassificationInstructions.TEMPLATE_ID,
                            "ReAct + self-reflection",
                            PromotedSpecialtyClassificationInstructions.classificationBlock()))
            .stream()
            .collect(Collectors.toUnmodifiableMap(PromptTemplate::id, Function.identity()));

    private PromptTemplateLibrary() {}

    public static Optional<PromptTemplate> findById(String id) {
        return Optional.ofNullable(TEMPLATES.get(id));
    }

    public static List<PromptTemplate> all() {
        return List.copyOf(TEMPLATES.values());
    }
}
