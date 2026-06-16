package com.example.medicalmcp.medicalcase.repository;

import com.example.medicalmcp.medicalcase.domain.CaseSummary;
import com.example.medicalmcp.medicalcase.domain.MedicalCase;
import com.example.medicalmcp.medicalcase.domain.SpecialtyCount;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface MedicalCaseRepository {

    Optional<MedicalCase> findById(UUID id);

    List<CaseSummary> fullTextSearch(String query, String specialty, String split, int limit);

    List<SpecialtyCount> listSpecialties();

    long countAll();

    void insertBatch(List<MedicalCase> cases);

    void updateEmbeddingsBatch(Map<UUID, float[]> embeddings);
}
