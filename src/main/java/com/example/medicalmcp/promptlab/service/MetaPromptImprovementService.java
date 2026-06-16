package com.example.medicalmcp.promptlab.service;

import com.example.medicalmcp.medicalcase.domain.MedicalCase;
import com.example.medicalmcp.medicalcase.repository.MedicalCaseRepository;
import com.example.medicalmcp.promptlab.config.PromptLabProperties;
import com.example.medicalmcp.promptlab.domain.ClassificationEvalResult;
import com.example.medicalmcp.promptlab.domain.EvalRun;
import com.example.medicalmcp.promptlab.domain.ImprovePromptResult;
import com.example.medicalmcp.promptlab.domain.PromptTemplate;
import com.example.medicalmcp.promptlab.eval.CaseTextFormatter;
import com.example.medicalmcp.promptlab.template.PromptTemplateRegistry;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Profile("prompt-lab")
public class MetaPromptImprovementService {

    private final PromptTemplateRegistry templateRegistry;
    private final PromptLabEvalRunStore evalRunStore;
    private final MedicalCaseRepository medicalCaseRepository;
    private final PromptLabProperties properties;

    public MetaPromptImprovementService(
            PromptTemplateRegistry templateRegistry,
            PromptLabEvalRunStore evalRunStore,
            MedicalCaseRepository medicalCaseRepository,
            PromptLabProperties properties) {
        this.templateRegistry = templateRegistry;
        this.evalRunStore = evalRunStore;
        this.medicalCaseRepository = medicalCaseRepository;
        this.properties = properties;
    }

    public ImprovePromptResult improve(String baseTemplateId, String evalRunId) {
        PromptTemplate base = templateRegistry
                .findById(baseTemplateId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown template: " + baseTemplateId));

        EvalRun sourceRun = resolveEvalRun(baseTemplateId, evalRunId);
        List<ClassificationEvalResult> failures = evalRunStore.failures(sourceRun);
        String improvedText = appendFailureContext(base.systemText(), failures);

        String newId = baseTemplateId + "-meta-" + UUID.randomUUID().toString().substring(0, 8);
        String name = base.name() + " (meta-improved)";
        templateRegistry.register(newId, name, improvedText);

        return new ImprovePromptResult(newId, name, improvedText, sourceRun.runId());
    }

    private EvalRun resolveEvalRun(String baseTemplateId, String evalRunId) {
        if (StringUtils.hasText(evalRunId)) {
            return evalRunStore
                    .findById(evalRunId)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown eval run: " + evalRunId));
        }
        return evalRunStore
                .findLatestForTemplate(baseTemplateId)
                .orElseThrow(() -> new IllegalArgumentException("No eval run for template: " + baseTemplateId));
    }

    private String appendFailureContext(String basePrompt, List<ClassificationEvalResult> failures) {
        if (failures.isEmpty()) {
            return basePrompt;
        }
        int limit = Math.min(failures.size(), properties.getMaxFailureExamples());
        StringBuilder improved = new StringBuilder(basePrompt.trim()).append("\n\n## Failure examples (learn from these)\n");
        for (int i = 0; i < limit; i++) {
            ClassificationEvalResult failure = failures.get(i);
            MedicalCase medicalCase = medicalCaseRepository.findById(failure.caseId()).orElse(null);
            if (medicalCase == null) {
                continue;
            }
            improved.append("\n### Example ")
                    .append(i + 1)
                    .append("\nCase:\n")
                    .append(CaseTextFormatter.format(medicalCase))
                    .append("\nGold label: ")
                    .append(failure.goldSpecialty())
                    .append("\nWrong prediction: ")
                    .append(failure.predictedSpecialty())
                    .append('\n');
        }
        improved.append("\nRevise your reasoning. End with: PREDICTED_LABEL: <label>\n");
        return improved.toString();
    }
}
