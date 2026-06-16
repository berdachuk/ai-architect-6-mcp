package com.example.medicalmcp.promptlab.service;

import com.example.medicalmcp.medicalcase.domain.CaseSummary;
import com.example.medicalmcp.medicalcase.domain.MedicalCase;
import com.example.medicalmcp.medicalcase.repository.MedicalCaseRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Profile("prompt-lab")
public class PromptLabCaseLoader {

    private final MedicalCaseRepository medicalCaseRepository;

    public PromptLabCaseLoader(MedicalCaseRepository medicalCaseRepository) {
        this.medicalCaseRepository = medicalCaseRepository;
    }

    public List<MedicalCase> loadCases(String split, int limit, String searchQuery) {
        String query = StringUtils.hasText(searchQuery) ? searchQuery.trim() : "patient";
        List<CaseSummary> summaries = medicalCaseRepository.fullTextSearch(query, null, split, limit);
        List<MedicalCase> cases = new ArrayList<>(summaries.size());
        for (CaseSummary summary : summaries) {
            cases.add(medicalCaseRepository.findById(summary.id()).orElseThrow());
        }
        return cases;
    }
}
