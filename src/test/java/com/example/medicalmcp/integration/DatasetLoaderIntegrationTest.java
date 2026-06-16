package com.example.medicalmcp.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.medicalmcp.dataset.service.DatasetLoaderService;
import com.example.medicalmcp.medicalcase.domain.MedicalCase;
import com.example.medicalmcp.medicalcase.repository.MedicalCaseRepository;
import java.util.UUID;
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
class DatasetLoaderIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private DatasetLoaderService datasetLoaderService;

    @Autowired
    private MedicalCaseRepository medicalCaseRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTable() {
        jdbcTemplate.update("DELETE FROM medical_case");
    }

    @Test
    void loadsTrainSampleWithAssignedUuids() {
        datasetLoaderService.loadIfEmpty();

        assertThat(medicalCaseRepository.countAll()).isEqualTo(10);

        UUID id = jdbcTemplate.queryForObject("SELECT id FROM medical_case LIMIT 1", UUID.class);
        assertThat(id).isNotNull();

        MedicalCase loaded = medicalCaseRepository.findById(id).orElseThrow();
        assertThat(loaded.id()).isEqualTo(id);
        assertThat(loaded.split()).isEqualTo("train");
        assertThat(loaded.sampleName()).isNotBlank();
        assertThat(loaded.description()).isNotBlank();
        assertThat(loaded.transcription()).isNotBlank();
        assertThat(loaded.medicalSpecialty()).isNotBlank();
        assertThat(loaded.createdAt()).isNotNull();
    }

    @Test
    void loadIsIdempotent() {
        datasetLoaderService.loadIfEmpty();
        assertThat(medicalCaseRepository.countAll()).isEqualTo(10);

        datasetLoaderService.loadIfEmpty();
        assertThat(medicalCaseRepository.countAll()).isEqualTo(10);
    }
}
