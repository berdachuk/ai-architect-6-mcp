package com.example.medicalmcp.mcp;

import com.example.medicalmcp.core.util.UuidUtils;
import com.example.medicalmcp.medicalcase.domain.CaseSummary;
import com.example.medicalmcp.medicalcase.domain.DatasetStats;
import com.example.medicalmcp.medicalcase.domain.MedicalCase;
import com.example.medicalmcp.medicalcase.domain.SemanticMatch;
import com.example.medicalmcp.medicalcase.domain.SpecialtyCount;
import com.example.medicalmcp.medicalcase.repository.MedicalCaseRepository;
import com.example.medicalmcp.retrieval.service.VectorSearchService;
import java.util.List;
import java.util.UUID;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.ai.mcp.annotation.context.McpSyncRequestContext;
import org.springframework.stereotype.Component;

@Component
public class MedicalCaseTools {

    private final MedicalCaseRepository caseRepository;
    private final VectorSearchService vectorSearch;

    public MedicalCaseTools(MedicalCaseRepository caseRepository, VectorSearchService vectorSearch) {
        this.caseRepository = caseRepository;
        this.vectorSearch = vectorSearch;
    }

    @McpTool(
            name = "search_cases",
            description =
                    "Full-text search over medical case transcriptions, descriptions, and keywords. Returns case IDs (UUIDs) that can be used with get_case and case-analysis prompt.",
            annotations =
                    @McpTool.McpAnnotations(
                            readOnlyHint = true,
                            destructiveHint = false,
                            idempotentHint = true))
    public List<CaseSummary> searchCases(
            @McpToolParam(description = "Search terms", required = true) String query,
            @McpToolParam(description = "Exact medical_specialty label (one of 13 HF classes)", required = false)
                    String specialty,
            @McpToolParam(description = "Filter by dataset split: train | validation | test", required = false)
                    String split,
            @McpToolParam(description = "Max results (default 10, max 50)", required = false) Integer limit) {
        return vectorSearch.searchCases(query, specialty, split, limit);
    }

    @McpTool(
            name = "get_case",
            description =
                    "Retrieve a single medical case by UUID, including the full transcription text. Requires a UUID obtained from search_cases or semantic_search.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false),
            generateOutputSchema = true)
    public MedicalCase getCase(@McpToolParam(description = "Case UUID", required = true) String id) {
        UUID uuid = UuidUtils.parseUuid(id);
        if (uuid == null) {
            return null;
        }
        return caseRepository.findById(uuid).orElse(null);
    }

    @McpTool(
            name = "semantic_search",
            description =
                    "Vector similarity search over medical cases. Embeds the query and returns the most similar cases by cosine distance. Returns case IDs (UUIDs) that can be used with get_case.",
            annotations =
                    @McpTool.McpAnnotations(
                            readOnlyHint = true,
                            destructiveHint = false,
                            idempotentHint = true))
    public List<SemanticMatch> semanticSearch(
            McpSyncRequestContext context,
            @McpToolParam(description = "Free-text query to embed and compare", required = true) String query,
            @McpToolParam(description = "Exact medical_specialty label (one of 13 HF classes)", required = false)
                    String specialty,
            @McpToolParam(description = "Number of results (default 5)", required = false) Integer topK,
            @McpToolParam(description = "Minimum cosine similarity 0–1 (default 0.70)", required = false)
                    Double minSimilarity) {
        if (context != null) {
            context.progress(progress -> progress.progress(0).total(1.0).message("Embedding query"));
        }
        List<SemanticMatch> results = vectorSearch.semanticSearch(query, specialty, topK, minSimilarity);
        if (context != null) {
            context.progress(progress -> progress.progress(1.0).total(1.0).message("Search complete"));
        }
        return results;
    }

    @McpTool(
            name = "list_specialties",
            description = "List all medical specialties present in the dataset with case counts.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false))
    public List<SpecialtyCount> listSpecialties() {
        return vectorSearch.listSpecialties();
    }

    @McpTool(
            name = "get_dataset_stats",
            description =
                    "Return dataset statistics: total cases, breakdown by specialty and by split (train/validation/test).",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false))
    public DatasetStats getDatasetStats() {
        return vectorSearch.getDatasetStats();
    }
}
