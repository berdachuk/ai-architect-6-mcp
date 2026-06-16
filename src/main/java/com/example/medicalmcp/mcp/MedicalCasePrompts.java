package com.example.medicalmcp.mcp;

import com.example.medicalmcp.medicalcase.domain.MedicalCase;
import com.example.medicalmcp.medicalcase.repository.MedicalCaseRepository;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import java.util.List;
import java.util.UUID;
import org.springframework.ai.mcp.annotation.McpArg;
import org.springframework.ai.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class MedicalCasePrompts {

    private final MedicalCaseRepository caseRepository;

    public MedicalCasePrompts(MedicalCaseRepository caseRepository) {
        this.caseRepository = caseRepository;
    }

    @McpPrompt(
            name = "case-analysis",
            description = "Structured prompt for LLM analysis of a medical case (dataset fields only).")
    public GetPromptResult analyzeCase(
            @McpArg(name = "caseId", description = "Server UUID from search_cases / semantic_search", required = true)
                    String caseId,
            @McpArg(
                            name = "focus",
                            description =
                                    "Dataset field emphasis: description | transcription | keywords | specialty | all",
                            required = false)
                    String focus) {
        UUID uuid = parseUuid(caseId);
        MedicalCase medicalCase = uuid == null ? null : caseRepository.findById(uuid).orElse(null);
        if (medicalCase == null) {
            return GetPromptResult.builder(List.of())
                    .description("Case not found")
                    .build();
        }

        String message = buildAnalysisMessage(medicalCase, resolveFocus(focus));
        return GetPromptResult.builder(List.of(new PromptMessage(Role.USER, new TextContent(message))))
                .description("Medical case analysis template")
                .build();
    }

    private static String resolveFocus(String focus) {
        return StringUtils.hasText(focus) ? focus.trim().toLowerCase() : "all";
    }

    private static String buildAnalysisMessage(MedicalCase medicalCase, String focus) {
        StringBuilder message = new StringBuilder();
        message.append("Analyze the following medical case from the dataset.\n\n");
        message.append("Case ID: ").append(medicalCase.id()).append('\n');
        message.append("Sample name: ").append(medicalCase.sampleName()).append('\n');
        message.append("Split: ").append(medicalCase.split()).append('\n');

        switch (focus) {
            case "description" -> appendDescription(message, medicalCase);
            case "transcription" -> appendTranscription(message, medicalCase);
            case "keywords" -> appendKeywords(message, medicalCase);
            case "specialty" -> appendSpecialty(message, medicalCase);
            default -> {
                appendSpecialty(message, medicalCase);
                appendDescription(message, medicalCase);
                appendTranscription(message, medicalCase);
                appendKeywords(message, medicalCase);
            }
        }

        message.append("\nProvide a structured summary using only the fields above. Do not invent clinical facts.");
        return message.toString();
    }

    private static void appendDescription(StringBuilder message, MedicalCase medicalCase) {
        message.append("\nDescription:\n").append(medicalCase.description()).append('\n');
    }

    private static void appendTranscription(StringBuilder message, MedicalCase medicalCase) {
        message.append("\nTranscription:\n").append(medicalCase.transcription()).append('\n');
    }

    private static void appendKeywords(StringBuilder message, MedicalCase medicalCase) {
        if (StringUtils.hasText(medicalCase.keywords())) {
            message.append("\nKeywords:\n").append(medicalCase.keywords()).append('\n');
        }
    }

    private static void appendSpecialty(StringBuilder message, MedicalCase medicalCase) {
        message.append("Medical specialty: ").append(medicalCase.medicalSpecialty()).append('\n');
    }

    private static UUID parseUuid(String id) {
        if (!StringUtils.hasText(id)) {
            return null;
        }
        try {
            return UUID.fromString(id.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
