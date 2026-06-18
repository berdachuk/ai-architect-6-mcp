package com.example.medicalmcp.promptlab.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.medicalmcp.medicalcase.domain.MedicalCase;
import com.example.medicalmcp.medicalcase.repository.MedicalCaseRepository;
import com.example.medicalmcp.promptlab.config.PromptLabProperties;
import com.example.medicalmcp.promptlab.domain.ClassificationEvalResult;
import com.example.medicalmcp.promptlab.domain.ClassificationEvalSummary;
import com.example.medicalmcp.promptlab.domain.ImprovePromptResult;
import com.example.medicalmcp.promptlab.template.PromptTemplateRegistry;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetaPromptImprovementServiceTest {

    @Mock
    private MedicalCaseRepository medicalCaseRepository;

    private PromptLabEvalRunStore evalRunStore;
    private MetaPromptImprovementService service;

    @BeforeEach
    void setUp() {
        evalRunStore = new PromptLabEvalRunStore();
        PromptTemplateRegistry registry = new PromptTemplateRegistry();
        PromptLabProperties properties = new PromptLabProperties();
        properties.setMaxFailureExamples(3);
        service = new MetaPromptImprovementService(
                registry, evalRunStore, medicalCaseRepository, properties);
    }

    @Test
    void improvedPromptIncludesFailureContext() {
        UUID caseId = UUID.randomUUID();
        MedicalCase medicalCase = sampleCase(caseId);
        when(medicalCaseRepository.findById(any())).thenReturn(Optional.of(medicalCase));

        ClassificationEvalSummary summary =
                new ClassificationEvalSummary("bad", "validation", 1, 0, 0.0, Map.of("Orthopedic", 1L));
        List<ClassificationEvalResult> results =
                List.of(new ClassificationEvalResult(caseId, "Orthopedic", "UNKNOWN", false));
        String runId = evalRunStore.save(summary, results);

        ImprovePromptResult improved = service.improve("bad", runId);

        assertThat(improved.systemText()).contains("Failure examples");
        assertThat(improved.systemText()).contains("Gold label: Orthopedic");
        assertThat(improved.systemText()).contains("PREDICTED_LABEL");
        assertThat(improved.templateId()).startsWith("bad-meta-");
    }

    private static MedicalCase sampleCase(UUID id) {
        return new MedicalCase(
                id, "Knee", "Pain", "Note", "Orthopedic", "knee", "validation", Instant.now());
    }
}
