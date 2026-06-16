package com.example.medicalmcp.mcp;

import com.example.medicalmcp.medicalcase.domain.DatasetStats;
import com.example.medicalmcp.medicalcase.domain.MedicalCase;
import com.example.medicalmcp.medicalcase.repository.MedicalCaseRepository;
import com.example.medicalmcp.retrieval.service.VectorSearchService;
import java.util.UUID;
import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class MedicalCaseResources {

    private final MedicalCaseRepository caseRepository;
    private final VectorSearchService vectorSearch;

    public MedicalCaseResources(MedicalCaseRepository caseRepository, VectorSearchService vectorSearch) {
        this.caseRepository = caseRepository;
        this.vectorSearch = vectorSearch;
    }

    @McpResource(
            uri = "medical://cases/{id}",
            name = "medical-case",
            description = "Full medical case record (including transcription) by UUID.",
            mimeType = "application/json")
    public MedicalCase getCase(String id) {
        if (!StringUtils.hasText(id)) {
            return null;
        }
        try {
            return caseRepository.findById(UUID.fromString(id.trim())).orElse(null);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @McpResource(
            uri = "medical://stats",
            name = "medical-dataset-stats",
            description = "Dataset statistics snapshot.",
            mimeType = "application/json")
    public DatasetStats getStats() {
        return vectorSearch.getDatasetStats();
    }
}
