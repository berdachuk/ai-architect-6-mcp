package com.example.medicalmcp.integration.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.medicalmcp.dataset.service.DatasetLoaderService;
import com.example.medicalmcp.integration.AbstractPostgresIntegrationTest;
import com.example.medicalmcp.medicalcase.domain.DatasetStats;
import com.example.medicalmcp.medicalcase.domain.MedicalCase;
import com.example.medicalmcp.support.McpSmokeTestSupport;
import com.example.medicalmcp.support.McpSseTestClientFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(
        properties = {
            "medicalmcp.dataset.loader.enabled=false",
            "medicalmcp.dataset.loader.sources=classpath:dataset/train-sample-10.csv"
        })
class McpSseSmokeIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final Set<String> EXPECTED_TOOLS = Set.of(
            "search_cases",
            "get_case",
            "semantic_search",
            "list_specialties",
            "get_dataset_stats");

    @LocalServerPort
    private int port;

    @Autowired
    private DatasetLoaderService datasetLoaderService;

    @Autowired
    private ObjectMapper objectMapper;

    private McpSyncClient mcpClient;

    @BeforeEach
    void setUp() {
        datasetLoaderService.loadIfEmpty();
        mcpClient = McpSseTestClientFactory.connect(port);
    }

    @AfterEach
    void tearDown() {
        if (mcpClient != null) {
            mcpClient.close();
        }
    }

    @Test
    void mcpServerExposesDatasetToolsOverSse() {
        Set<String> toolNames = mcpClient.listTools().tools().stream()
                .map(McpSchema.Tool::name)
                .collect(Collectors.toSet());

        assertThat(toolNames).containsExactlyInAnyOrderElementsOf(EXPECTED_TOOLS);
    }

    @Test
    void searchCasesToolReturnsSummariesViaSse() throws Exception {
        CallToolRequest request = new CallToolRequest(
                "search_cases", McpSmokeTestSupport.args("query", "Pacemaker Interrogation", "limit", 5));
        JsonNode payload = McpSmokeTestSupport.toJson(mcpClient.callTool(request), objectMapper);

        assertThat(payload.isArray()).isTrue();
        assertThat(payload).isNotEmpty();
        assertThat(payload.get(0).get("sampleName").asText()).isEqualTo("Pacemaker Interrogation");
        assertThat(payload.get(0).has("transcription")).isFalse();
    }

    @Test
    void getCaseRoundTripViaSse() throws Exception {
        String id = resolveCaseId("Pacemaker Interrogation");

        CallToolRequest request =
                new CallToolRequest("get_case", McpSmokeTestSupport.args("id", id.toString()));
        JsonNode payload = McpSmokeTestSupport.toJson(mcpClient.callTool(request), objectMapper);

        MedicalCase medicalCase = objectMapper.treeToValue(payload, MedicalCase.class);
        assertThat(medicalCase.id()).isEqualTo(id);
        assertThat(medicalCase.transcription()).isNotBlank();
        assertThat(medicalCase.sampleName()).isEqualTo("Pacemaker Interrogation");
    }

    @Test
    void semanticSearchToolReturnsMatchesViaSse() throws Exception {
        CallToolRequest request = new CallToolRequest(
                "semantic_search",
                McpSmokeTestSupport.args("query", "pacemaker device check", "topK", 5, "minSimilarity", -1.0));
        JsonNode payload = McpSmokeTestSupport.toJson(mcpClient.callTool(request), objectMapper);

        assertThat(payload.isArray()).isTrue();
        assertThat(payload).isNotEmpty();
        assertThat(payload.get(0).get("similarity").asDouble()).isBetween(0.0, 1.0);
        assertThat(payload.get(0).get("caseSummary").get("id").asText()).isNotBlank();
    }

    @Test
    void listSpecialtiesAndDatasetStatsViaSse() throws Exception {
        JsonNode specialties =
                McpSmokeTestSupport.toJson(mcpClient.callTool(new CallToolRequest("list_specialties", Map.of())), objectMapper);
        assertThat(specialties.isArray()).isTrue();
        assertThat(specialties).hasSize(8);

        JsonNode stats = McpSmokeTestSupport.toJson(
                mcpClient.callTool(new CallToolRequest("get_dataset_stats", Map.of())), objectMapper);
        DatasetStats datasetStats = objectMapper.treeToValue(stats, DatasetStats.class);
        assertThat(datasetStats.totalCases()).isEqualTo(10);
        assertThat(datasetStats.bySplit()).containsEntry("train", 10L);
    }

    @Test
    void resourcesAndPromptResolveViaSse() throws Exception {
        String id = resolveCaseId("Pacemaker Interrogation");

        String statsJson = McpSmokeTestSupport.textFromResource(
                mcpClient.readResource(new ReadResourceRequest("medical://stats")));
        DatasetStats statsFromResource = objectMapper.readValue(statsJson, DatasetStats.class);
        assertThat(statsFromResource.totalCases()).isEqualTo(10);

        String caseJson = McpSmokeTestSupport.textFromResource(
                mcpClient.readResource(new ReadResourceRequest("medical://cases/" + id)));
        MedicalCase caseFromResource = objectMapper.readValue(caseJson, MedicalCase.class);
        assertThat(caseFromResource.id()).isEqualTo(id);

        McpSchema.GetPromptResult prompt = mcpClient.getPrompt(new GetPromptRequest(
                "case-analysis", McpSmokeTestSupport.args("caseId", id.toString(), "focus", "transcription")));
        assertThat(prompt.messages()).hasSize(1);
        String text = ((TextContent) prompt.messages().getFirst().content()).text();
        assertThat(text).contains("Transcription:");
        assertThat(text).doesNotContain("null");
    }

    private String resolveCaseId(String sampleName) throws Exception {
        CallToolRequest request = new CallToolRequest(
                "search_cases", McpSmokeTestSupport.args("query", sampleName, "limit", 1));
        JsonNode results = McpSmokeTestSupport.toJson(mcpClient.callTool(request), objectMapper);
        return results.get(0).get("id").asText();
    }
}
