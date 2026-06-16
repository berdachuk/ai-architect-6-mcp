package com.example.medicalmcp.promptlab.template;

import com.example.medicalmcp.promptlab.domain.PromptTemplate;
import com.example.medicalmcp.promptlab.normalization.SpecialtyLabelNormalizer;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class PromptTemplateLibrary {

    private static final String ALLOWED_LABELS = SpecialtyLabelNormalizer.CANONICAL_LABELS.stream()
            .map(SpecialtyLabelNormalizer::toSnakeCase)
            .collect(Collectors.joining(", "));

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
                            "react_self_reflection",
                            "ReAct + self-reflection",
                            """
                            Classify the medical case into exactly one of these labels (snake_case):
                            %s

                            Thought → Action → Observation → Reflection → Answer.
                            End with: PREDICTED_LABEL: <label>
                            """.formatted(ALLOWED_LABELS)))
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
