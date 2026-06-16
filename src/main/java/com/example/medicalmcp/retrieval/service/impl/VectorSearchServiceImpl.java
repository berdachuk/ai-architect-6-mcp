package com.example.medicalmcp.retrieval.service.impl;

import com.example.medicalmcp.embedding.service.EmbeddingService;
import com.example.medicalmcp.medicalcase.domain.CaseSummary;
import com.example.medicalmcp.medicalcase.domain.DatasetStats;
import com.example.medicalmcp.medicalcase.domain.SemanticMatch;
import com.example.medicalmcp.medicalcase.domain.SpecialtyCount;
import com.example.medicalmcp.medicalcase.repository.MedicalCaseRepository;
import com.example.medicalmcp.retrieval.service.VectorSearchService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class VectorSearchServiceImpl implements VectorSearchService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int DEFAULT_TOP_K = 5;
    private static final double DEFAULT_MIN_SIMILARITY = 0.70;

    private final MedicalCaseRepository repository;
    private final EmbeddingService embeddingService;
    private final int maxLimit;

    public VectorSearchServiceImpl(
            MedicalCaseRepository repository,
            EmbeddingService embeddingService,
            @Value("${medicalmcp.retrieval.max-limit:50}") int maxLimit) {
        this.repository = repository;
        this.embeddingService = embeddingService;
        this.maxLimit = maxLimit;
    }

    @Override
    public List<CaseSummary> searchCases(String query, String specialty, String split, Integer limit) {
        int effectiveLimit = limit == null ? DEFAULT_LIMIT : Math.min(Math.max(limit, 1), maxLimit);
        return repository.fullTextSearch(query, specialty, split, effectiveLimit);
    }

    @Override
    public List<SpecialtyCount> listSpecialties() {
        return repository.listSpecialties();
    }

    @Override
    @Cacheable(cacheNames = "datasetStats")
    public DatasetStats getDatasetStats() {
        Map<String, Long> bySpecialty = repository.listSpecialties().stream()
                .collect(Collectors.toMap(SpecialtyCount::specialty, SpecialtyCount::count));
        return new DatasetStats(repository.countAll(), bySpecialty, repository.countBySplit());
    }

    @Override
    public List<SemanticMatch> semanticSearch(
            String query, String specialty, Integer topK, Double minSimilarity) {
        int effectiveTopK = topK == null ? DEFAULT_TOP_K : Math.min(Math.max(topK, 1), maxLimit);
        double effectiveMin = minSimilarity == null ? DEFAULT_MIN_SIMILARITY : minSimilarity;
        float[] queryEmbedding = embeddingService.embedAsFloatArray(query);
        return repository.semanticSearch(queryEmbedding, specialty, effectiveTopK, effectiveMin);
    }
}
