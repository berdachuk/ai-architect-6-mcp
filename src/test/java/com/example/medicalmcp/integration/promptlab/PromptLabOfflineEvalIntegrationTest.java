package com.example.medicalmcp.integration.promptlab;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.medicalmcp.dataset.service.DatasetLoaderService;
import com.example.medicalmcp.integration.AbstractPostgresIntegrationTest;
import com.example.medicalmcp.medicalcase.domain.CaseSummary;
import com.example.medicalmcp.medicalcase.domain.MedicalCase;
import com.example.medicalmcp.medicalcase.repository.MedicalCaseRepository;
import com.example.medicalmcp.promptlab.config.PromptLabProperties;
import com.example.medicalmcp.promptlab.domain.ClassificationEvalSummary;
import com.example.medicalmcp.promptlab.service.OfflineClassificationSimulator;
import com.example.medicalmcp.promptlab.service.SpecialtyClassificationEvaluator;
import com.example.medicalmcp.quality.PromptLabQualityReporter;
import com.example.medicalmcp.quality.QualityReportWriter;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

@Tag("prompt-lab")
@ActiveProfiles({"test", "prompt-lab"})
class PromptLabOfflineEvalIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private DatasetLoaderService datasetLoaderService;

    @Autowired
    private MedicalCaseRepository medicalCaseRepository;

    @Autowired
    private SpecialtyClassificationEvaluator evaluator;

    @Autowired
    private PromptLabProperties promptLabProperties;

    @BeforeEach
    void loadFixture() {
        datasetLoaderService.loadIfEmpty();
    }

    @Test
    void reactTemplateContractPassesOfflineGate() {
        List<MedicalCase> cases = loadValidationCases();
        ClassificationEvalSummary summary = evaluator.evaluate(
                "react_self_reflection",
                promptLabProperties.getEvalSplit(),
                cases,
                OfflineClassificationSimulator::accurateOutput);

        PromptLabQualityReporter.mergePromptLabMetrics(summary, promptLabProperties.getMinAccuracy());

        assertThat(summary.meetsGate(promptLabProperties.getMinAccuracy())).isTrue();
        assertThat(QualityReportWriter.reportPath()).exists();
    }

    @Test
    void badTemplateFailsOfflineGate() {
        List<MedicalCase> cases = loadValidationCases();
        ClassificationEvalSummary summary = evaluator.evaluate(
                "bad", promptLabProperties.getEvalSplit(), cases, OfflineClassificationSimulator::badOutput);

        assertThat(summary.meetsGate(promptLabProperties.getMinAccuracy())).isFalse();
    }

    private List<MedicalCase> loadValidationCases() {
        List<CaseSummary> summaries =
                medicalCaseRepository.fullTextSearch("patient", null, promptLabProperties.getEvalSplit(), 50);
        List<MedicalCase> cases = new ArrayList<>(summaries.size());
        for (CaseSummary summary : summaries) {
            cases.add(medicalCaseRepository.findById(summary.id()).orElseThrow());
        }
        return cases;
    }
}
