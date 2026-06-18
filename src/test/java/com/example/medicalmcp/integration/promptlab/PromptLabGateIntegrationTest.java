package com.example.medicalmcp.integration.promptlab;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.medicalmcp.dataset.service.DatasetLoaderService;
import com.example.medicalmcp.integration.AbstractPostgresIntegrationTest;
import com.example.medicalmcp.promptlab.config.PromptLabProperties;
import com.example.medicalmcp.promptlab.domain.ClassificationEvalSummary;
import com.example.medicalmcp.promptlab.mcp.PromptLabTools;
import com.example.medicalmcp.quality.PromptLabQualityReporter;
import com.example.medicalmcp.quality.QualityReportWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@Tag("prompt-lab")
@ActiveProfiles({"test", "prompt-lab"})
@TestPropertySource(
        properties = {"medicalmcp.dataset.loader.sources=classpath:dataset/test-sample-10.csv"})
class PromptLabGateIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String TEST_SPLIT = "test";

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
    void gatePassesOnTestSplitWithOfflineStub() {
        ClassificationEvalSummary summary =
                promptLabTools.gateSpecialtyPrompt("react_self_reflection", TEST_SPLIT, 10);

        PromptLabQualityReporter.mergePromptLabMetrics(summary, promptLabProperties.getMinAccuracy());

        assertThat(summary.total()).isGreaterThan(0);
        assertThat(summary.meetsGate(promptLabProperties.getMinAccuracy())).isTrue();
        assertThat(QualityReportWriter.reportPath()).exists();
    }
}
