package com.example.medicalmcp.medicalcase.repository;

import com.example.medicalmcp.medicalcase.domain.CaseSummary;
import com.example.medicalmcp.medicalcase.domain.MedicalCase;
import com.example.medicalmcp.medicalcase.domain.SemanticMatch;
import com.example.medicalmcp.medicalcase.domain.SpecialtyCount;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface MedicalCaseRepository {

    Optional<MedicalCase> findById(String id);

    List<CaseSummary> fullTextSearch(String query, String specialty, String split, int limit);

    List<SpecialtyCount> listSpecialties();

    Map<String, Long> countBySplit();

    long countAll();

    void insertBatch(List<MedicalCase> cases);

    List<MedicalCase> findWithoutEmbeddings();

    List<MedicalCase> findWithoutEmbeddingsBySplit(String split);

    List<SemanticMatch> semanticSearch(float[] queryEmbedding, String specialty, int topK, double minSimilarity);

    void updateEmbeddingsBatch(Map<String, float[]> embeddings);
}
