package com.example.medicalmcp.integration.promptlab;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.medicalmcp.dataset.service.DatasetLoaderService;
import com.example.medicalmcp.integration.AbstractPostgresIntegrationTest;
import com.example.medicalmcp.promptlab.config.PromptLabProperties;
import com.example.medicalmcp.promptlab.domain.ClassificationEvalSummary;
import com.example.medicalmcp.promptlab.domain.ImprovePromptResult;
import com.example.medicalmcp.promptlab.domain.PromptTemplate;
import com.example.medicalmcp.promptlab.domain.TemplateCompareEntry;
import com.example.medicalmcp.promptlab.mcp.PromptLabTools;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

@Tag("prompt-lab")
@ActiveProfiles({"test", "prompt-lab"})
class PromptLabToolsIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private DatasetLoaderService datasetLoaderService;

    @Autowired
    private PromptLabTools promptLabTools;

    @Autowired
    private PromptLabProperties promptLabProperties;

    @BeforeEach
    void loadFixture() {
        datasetLoaderService.loadIfEmpty();
    }

    @Test
    void evaluateAndCompareTemplates() {
        ClassificationEvalSummary good = promptLabTools.evaluateSpecialtyPrompt(
                "react_self_reflection", "validation", 10, true);
        ClassificationEvalSummary bad =
                promptLabTools.evaluateSpecialtyPrompt("bad", "validation", 10, true);

        assertThat(good.meetsGate(promptLabProperties.getMinAccuracy())).isTrue();
        assertThat(bad.meetsGate(promptLabProperties.getMinAccuracy())).isFalse();

        List<TemplateCompareEntry> ranked =
                promptLabTools.compareSpecialtyPrompts(List.of("bad", "react_self_reflection"), "validation", 10);

        assertThat(ranked).hasSize(2);
        assertThat(ranked.getFirst().templateId()).isEqualTo("react_self_reflection");
        assertThat(ranked.getFirst().accuracy()).isGreaterThan(ranked.get(1).accuracy());
    }

    @Test
    void improveTemplateUsesFailureContext() {
        promptLabTools.evaluateSpecialtyPrompt("bad", "validation", 10, true);

        ImprovePromptResult improved = promptLabTools.improveSpecialtyPrompt("bad", null);

        assertThat(improved.systemText()).contains("Failure examples");
        assertThat(improved.templateId()).startsWith("bad-meta-");

        ClassificationEvalSummary gated =
                promptLabTools.gateSpecialtyPrompt(improved.templateId(), "validation", 10);
        assertThat(gated.total()).isGreaterThan(0);
    }

    @Test
    void listPromptTemplatesIncludesBuiltIns() {
        List<PromptTemplate> templates = promptLabTools.listPromptTemplates();

        assertThat(templates).extracting(PromptTemplate::id).contains("bad", "react_self_reflection");
    }
}
