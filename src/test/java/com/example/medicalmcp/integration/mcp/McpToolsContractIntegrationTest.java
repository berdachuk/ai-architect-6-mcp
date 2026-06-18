package com.example.medicalmcp.integration.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.medicalmcp.core.prompt.PromotedSpecialtyClassificationInstructions;
import com.example.medicalmcp.dataset.service.DatasetLoaderService;
import com.example.medicalmcp.mcp.MedicalCasePrompts;
import com.example.medicalmcp.mcp.MedicalCaseTools;
import com.example.medicalmcp.medicalcase.domain.CaseSummary;
import com.example.medicalmcp.medicalcase.domain.DatasetStats;
import com.example.medicalmcp.medicalcase.domain.MedicalCase;
import com.example.medicalmcp.medicalcase.domain.SemanticMatch;
import com.example.medicalmcp.medicalcase.domain.SpecialtyCount;
import com.example.medicalmcp.integration.AbstractPostgresIntegrationTest;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(
        properties = {
            "medicalmcp.dataset.loader.enabled=false",
            "medicalmcp.dataset.loader.sources=classpath:dataset/train-sample-10.csv",
            "medicalmcp.retrieval.max-limit=50"
        })
class McpToolsContractIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private DatasetLoaderService datasetLoaderService;

    @Autowired
    private MedicalCaseTools medicalCaseTools;

    @Autowired
    private MedicalCasePrompts medicalCasePrompts;

    @BeforeEach
    void loadFixture() {
        datasetLoaderService.loadIfEmpty();
    }

    @Test
    void searchCasesReturnsCaseSummaryShape() {
        List<CaseSummary> results = medicalCaseTools.searchCases("Pacemaker Interrogation", null, null, 5);

        assertThat(results).isNotEmpty();
        CaseSummary summary = results.getFirst();
        assertThat(summary.id()).isNotNull();
        assertThat(summary.sampleName()).isEqualTo("Pacemaker Interrogation");
        assertThat(summary.description()).isNotBlank();
        assertThat(summary.medicalSpecialty()).isNotBlank();
        assertThat(summary.split()).isEqualTo("train");
    }

    @Test
    void getCaseReturnsFullMedicalCase() {
        String id = medicalCaseTools
                .searchCases("Pacemaker Interrogation", null, null, 1)
                .getFirst()
                .id();

        MedicalCase medicalCase = medicalCaseTools.getCase(id.toString());

        assertThat(medicalCase).isNotNull();
        assertThat(medicalCase.id()).isEqualTo(id);
        assertThat(medicalCase.transcription()).isNotBlank();
        assertThat(medicalCase.createdAt()).isNotNull();
    }

    @Test
    void getCaseReturnsNullForInvalidUuid() {
        assertThat(medicalCaseTools.getCase("not-a-uuid")).isNull();
    }

    @Test
    void semanticSearchReturnsOrderedMatches() {
        List<SemanticMatch> results =
                medicalCaseTools.semanticSearch(null, "pacemaker device check", null, 5, -1.0);

        assertThat(results).isNotEmpty();
        assertThat(results.getFirst().caseSummary().id()).isNotNull();
        assertThat(results.getFirst().similarity()).isBetween(0.0, 1.0);
    }

    @Test
    void listSpecialtiesReturnsCounts() {
        List<SpecialtyCount> specialties = medicalCaseTools.listSpecialties();

        assertThat(specialties).hasSize(8);
        assertThat(specialties).allMatch(row -> row.count() > 0);
    }

    @Test
    void getDatasetStatsMatchesLoadedFixture() {
        DatasetStats stats = medicalCaseTools.getDatasetStats();

        assertThat(stats.totalCases()).isEqualTo(10);
        assertThat(stats.bySplit()).containsEntry("train", 10L);
    }

    @Test
    void searchCasesClampsLimitToMax() {
        assertThat(medicalCaseTools.searchCases("patient", null, "train", 100)).hasSizeLessThanOrEqualTo(50);
    }

    @Test
    void searchCasesReturnsEmptyForUnknownSpecialty() {
        assertThat(medicalCaseTools.searchCases("patient", "Nonexistent Specialty", null, 10))
                .isEmpty();
    }

    @Test
    void caseAnalysisPromptIncludesTranscriptionWhenFocused() {
        String id = medicalCaseTools
                .searchCases("Pacemaker Interrogation", null, null, 1)
                .getFirst()
                .id();

        GetPromptResult prompt = medicalCasePrompts.analyzeCase(id.toString(), "transcription");

        assertThat(prompt.messages()).hasSize(1);
        String text = ((TextContent) prompt.messages().getFirst().content()).text();
        assertThat(text).contains("Transcription:");
        assertThat(text).doesNotContain("null");
        assertThat(text).doesNotContain("PREDICTED_LABEL:");
    }

    @Test
    void caseAnalysisPromptAllFocusModesReturnStructuredTemplate() {
        String id = medicalCaseTools
                .searchCases("Pacemaker Interrogation", null, null, 1)
                .getFirst()
                .id();

        for (String focus : new String[] {"description", "transcription", "keywords", "specialty", "all", null}) {
            GetPromptResult prompt = medicalCasePrompts.analyzeCase(id.toString(), focus);
            assertThat(prompt.messages()).hasSize(1);
            String text = ((TextContent) prompt.messages().getFirst().content()).text();
            assertThat(text).contains("Case ID:");
            assertThat(text).doesNotContain("null");
        }
    }

    @Test
    void caseAnalysisPromptSpecialtyFocusIncludesPromotedTemplate() {
        String id = medicalCaseTools
                .searchCases("Pacemaker Interrogation", null, null, 1)
                .getFirst()
                .id();

        GetPromptResult prompt = medicalCasePrompts.analyzeCase(id.toString(), "specialty");
        String text = ((TextContent) prompt.messages().getFirst().content()).text();

        assertThat(text).contains("Description:");
        assertThat(text).contains("Transcription:");
        assertThat(text).contains("Medical specialty:");
        assertThat(text).contains(PromotedSpecialtyClassificationInstructions.classificationBlock());
    }
}
