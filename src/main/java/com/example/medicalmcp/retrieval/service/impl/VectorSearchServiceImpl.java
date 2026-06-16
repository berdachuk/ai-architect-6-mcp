package com.example.medicalmcp.retrieval.service.impl;

import com.example.medicalmcp.embedding.service.EmbeddingService;
import com.example.medicalmcp.medicalcase.domain.CaseSummary;
import com.example.medicalmcp.medicalcase.domain.DatasetStats;
import com.example.medicalmcp.medicalcase.domain.SemanticMatch;
import com.example.medicalmcp.medicalcase.domain.SpecialtyCount;
import com.example.medicalmcp.medicalcase.repository.MedicalCaseRepository;
import com.example.medicalmcp.retrieval.config.RetrievalProperties;
import com.example.medicalmcp.retrieval.service.VectorSearchService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class VectorSearchServiceImpl implements VectorSearchService {

    private final MedicalCaseRepository repository;
    private final EmbeddingService embeddingService;
    private final RetrievalProperties retrievalProperties;

    public VectorSearchServiceImpl(
            MedicalCaseRepository repository,
            EmbeddingService embeddingService,
            RetrievalProperties retrievalProperties) {
        this.repository = repository;
        this.embeddingService = embeddingService;
        this.retrievalProperties = retrievalProperties;
    }

    @Override
    public List<CaseSummary> searchCases(String query, String specialty, String split, Integer limit) {
        int effectiveLimit = limit == null
                ? 10
                : Math.min(Math.max(limit, 1), retrievalProperties.getMaxLimit());
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
        int effectiveTopK = topK == null
                ? retrievalProperties.getDefaultTopK()
                : Math.min(Math.max(topK, 1), retrievalProperties.getMaxLimit());
        double effectiveMin = minSimilarity == null ? retrievalProperties.getSimilarityThreshold() : minSimilarity;
        float[] queryEmbedding = embeddingService.embedAsFloatArray(query);
        return repository.semanticSearch(queryEmbedding, specialty, effectiveTopK, effectiveMin);
    }
}
