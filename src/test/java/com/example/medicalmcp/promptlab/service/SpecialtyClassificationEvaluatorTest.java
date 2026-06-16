package com.example.medicalmcp.promptlab.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.medicalmcp.medicalcase.domain.MedicalCase;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SpecialtyClassificationEvaluatorTest {

    @Test
    void computesAccuracyFromOfflineSimulator() {
        MedicalCase orthopedic = new MedicalCase(
                UUID.randomUUID(),
                "Knee aspiration",
                "Knee pain",
                "Procedure note",
                "Orthopedic",
                "knee",
                "validation",
                Instant.now());

        SpecialtyClassificationEvaluator evaluator = new SpecialtyClassificationEvaluator();
        var outcome = evaluator.evaluate(
                "react",
                "validation",
                List.of(orthopedic),
                OfflineClassificationSimulator::accurateOutput);

        assertThat(outcome.summary().accuracy()).isEqualTo(1.0);
        assertThat(outcome.summary().correct()).isEqualTo(1);
    }
}
