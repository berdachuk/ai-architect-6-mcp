package com.example.medicalmcp.retrieval.service;

import com.example.medicalmcp.medicalcase.domain.CaseSummary;
import com.example.medicalmcp.medicalcase.domain.DatasetStats;
import com.example.medicalmcp.medicalcase.domain.SemanticMatch;
import com.example.medicalmcp.medicalcase.domain.SpecialtyCount;
import java.util.List;

public interface VectorSearchService {

    List<CaseSummary> searchCases(String query, String specialty, String split, Integer limit);

    List<SpecialtyCount> listSpecialties();

    DatasetStats getDatasetStats();

    List<SemanticMatch> semanticSearch(String query, String specialty, Integer topK, Double minSimilarity);
}
