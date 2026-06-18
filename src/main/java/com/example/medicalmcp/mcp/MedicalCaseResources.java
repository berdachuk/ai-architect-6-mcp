package com.example.medicalmcp.mcp;

import com.example.medicalmcp.core.util.IdGenerator;
import com.example.medicalmcp.medicalcase.domain.MedicalCase;
import com.example.medicalmcp.medicalcase.repository.MedicalCaseRepository;
import com.example.medicalmcp.retrieval.service.VectorSearchService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

@Component
public class MedicalCaseResources {

    private final MedicalCaseRepository caseRepository;
    private final VectorSearchService vectorSearch;
    private final ObjectMapper objectMapper;

    public MedicalCaseResources(
            MedicalCaseRepository caseRepository, VectorSearchService vectorSearch, ObjectMapper objectMapper) {
        this.caseRepository = caseRepository;
        this.vectorSearch = vectorSearch;
        this.objectMapper = objectMapper;
    }

    @McpResource(
            uri = "medical://cases/{id}",
            name = "medical-case",
            description = "Full medical case record (including transcription) by ID.",
            mimeType = "application/json")
    public String getCase(String id) throws JsonProcessingException {
        MedicalCase medicalCase = findCase(id);
        return medicalCase == null ? "" : objectMapper.writeValueAsString(medicalCase);
    }

    @McpResource(
            uri = "medical://stats",
            name = "medical-dataset-stats",
            description = "Dataset statistics snapshot.",
            mimeType = "application/json")
    public String getStats() throws JsonProcessingException {
        return objectMapper.writeValueAsString(vectorSearch.getDatasetStats());
    }

    MedicalCase findCase(String id) {
        if (!IdGenerator.isValidId(id)) {
            return null;
        }
        return caseRepository.findById(id.trim().toLowerCase()).orElse(null);
    }
}
