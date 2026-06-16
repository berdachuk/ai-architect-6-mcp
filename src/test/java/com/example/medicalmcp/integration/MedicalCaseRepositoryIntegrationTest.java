package com.example.medicalmcp.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.medicalmcp.dataset.service.DatasetLoaderService;
import com.example.medicalmcp.medicalcase.domain.CaseSummary;
import com.example.medicalmcp.medicalcase.domain.SpecialtyCount;
import com.example.medicalmcp.medicalcase.repository.MedicalCaseRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(
        properties = {
            "medicalmcp.dataset.loader.enabled=false",
            "medicalmcp.dataset.loader.sources=classpath:dataset/train-sample-10.csv"
        })
class MedicalCaseRepositoryIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private DatasetLoaderService datasetLoaderService;

    @Autowired
    private MedicalCaseRepository medicalCaseRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void loadFixture() {
        jdbcTemplate.update("DELETE FROM medical_case");
        datasetLoaderService.loadIfEmpty();
    }

    @Test
    void fullTextSearchFindsCaseBySampleName() {
        List<CaseSummary> results = medicalCaseRepository.fullTextSearch("Pacemaker Interrogation", null, null, 10);

        assertThat(results).isNotEmpty();
        assertThat(results.getFirst().sampleName()).isEqualTo("Pacemaker Interrogation");
    }

    @Test
    void fullTextSearchFiltersBySpecialty() {
        List<CaseSummary> results =
                medicalCaseRepository.fullTextSearch("patient", "Orthopedic", null, 50);

        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(caseSummary -> "Orthopedic".equals(caseSummary.medicalSpecialty()));
    }

    @Test
    void fullTextSearchFiltersBySplit() {
        List<CaseSummary> results = medicalCaseRepository.fullTextSearch("patient", null, "train", 50);

        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(caseSummary -> "train".equals(caseSummary.split()));
    }

    @Test
    void fullTextSearchReturnsEmptyForInvalidSplit() {
        assertThat(medicalCaseRepository.fullTextSearch("patient", null, "invalid", 10)).isEmpty();
    }

    @Test
    void fullTextSearchRespectsLimit() {
        List<CaseSummary> results = medicalCaseRepository.fullTextSearch("patient", null, "train", 3);

        assertThat(results).hasSizeLessThanOrEqualTo(3);
    }

    @Test
    void listSpecialtiesReturnsCountsForLoadedData() {
        List<SpecialtyCount> specialties = medicalCaseRepository.listSpecialties();

        assertThat(specialties).hasSize(8);
        assertThat(specialties.stream().map(SpecialtyCount::specialty).collect(Collectors.toSet()))
                .contains("Cardiovascular / Pulmonary", "Orthopedic", "Neurology");
        assertThat(specialties.stream().mapToLong(SpecialtyCount::count).sum())
                .isEqualTo(medicalCaseRepository.countAll());
    }
}
