package com.example.medicalmcp.integration.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.medicalmcp.dataset.service.DatasetLoaderService;
import com.example.medicalmcp.integration.AbstractPostgresIntegrationTest;
import com.example.medicalmcp.mcp.MedicalCaseResources;
import com.example.medicalmcp.mcp.MedicalCaseTools;
import com.example.medicalmcp.medicalcase.domain.DatasetStats;
import com.example.medicalmcp.medicalcase.domain.MedicalCase;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(
        properties = {
            "medicalmcp.dataset.loader.enabled=false",
            "medicalmcp.dataset.loader.sources=classpath:dataset/train-sample-10.csv"
        })
class McpResourcesIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private DatasetLoaderService datasetLoaderService;

    @Autowired
    private MedicalCaseTools medicalCaseTools;

    @Autowired
    private MedicalCaseResources medicalCaseResources;

    @BeforeEach
    void loadFixture() {
        datasetLoaderService.loadIfEmpty();
    }

    @Test
    void medicalCaseResourceReturnsFullRecord() {
        UUID id = medicalCaseTools
                .searchCases("Pacemaker Interrogation", null, null, 1)
                .getFirst()
                .id();

        MedicalCase medicalCase = medicalCaseResources.getCase(id.toString());

        assertThat(medicalCase).isNotNull();
        assertThat(medicalCase.id()).isEqualTo(id);
        assertThat(medicalCase.transcription()).isNotBlank();
    }

    @Test
    void medicalStatsResourceMatchesToolOutput() {
        DatasetStats fromResource = medicalCaseResources.getStats();
        DatasetStats fromTool = medicalCaseTools.getDatasetStats();

        assertThat(fromResource.totalCases()).isEqualTo(fromTool.totalCases());
        assertThat(fromResource.bySplit()).isEqualTo(fromTool.bySplit());
        assertThat(fromResource.bySpecialty()).isEqualTo(fromTool.bySpecialty());
    }
}
