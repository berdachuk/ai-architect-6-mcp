package com.example.medicalmcp.promptlab.service;

import com.example.medicalmcp.medicalcase.domain.MedicalCase;
import com.example.medicalmcp.promptlab.domain.ClassificationEvalOutcome;
import com.example.medicalmcp.promptlab.domain.EvalRun;
import com.example.medicalmcp.promptlab.domain.PromptTemplate;
import com.example.medicalmcp.promptlab.template.PromptTemplateRegistry;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("prompt-lab")
public class SpecialtyPromptClassificationService {

    private final SpecialtyClassificationEvaluator evaluator;
    private final PromptLabCaseLoader caseLoader;
    private final PromptLabEvalRunStore evalRunStore;
    private final PromptTemplateRegistry templateRegistry;
    private final PromptLabClassificationClient classificationClient;

    public SpecialtyPromptClassificationService(
            SpecialtyClassificationEvaluator evaluator,
            PromptLabCaseLoader caseLoader,
            PromptLabEvalRunStore evalRunStore,
            PromptTemplateRegistry templateRegistry,
            PromptLabClassificationClient classificationClient) {
        this.evaluator = evaluator;
        this.caseLoader = caseLoader;
        this.evalRunStore = evalRunStore;
        this.templateRegistry = templateRegistry;
        this.classificationClient = classificationClient;
    }

    public EvalRun evaluate(String templateId, String split, int limit, boolean saveRun) {
        PromptTemplate template = templateRegistry
                .findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown template: " + templateId));
        List<MedicalCase> cases = caseLoader.loadCases(split, limit, "patient");
        ClassificationEvalOutcome outcome = runEval(template, split, cases);
        String runId = saveRun ? evalRunStore.save(outcome.summary(), outcome.results()) : null;
        return new EvalRun(runId, outcome.summary(), outcome.results());
    }

    private ClassificationEvalOutcome runEval(PromptTemplate template, String split, List<MedicalCase> cases) {
        return evaluator.evaluate(
                template.id(),
                split,
                cases,
                medicalCase -> classificationClient.classify(medicalCase, template.systemText(), template.id()));
    }
}
