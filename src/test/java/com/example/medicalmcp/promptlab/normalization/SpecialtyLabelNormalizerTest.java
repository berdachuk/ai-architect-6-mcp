package com.example.medicalmcp.promptlab.normalization;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SpecialtyLabelNormalizerTest {

    @Test
    void normalizesSnakeCaseAliasToCanonicalLabel() {
        assertThat(SpecialtyLabelNormalizer.toCanonical("cardiovascular_pulmonary"))
                .contains("Cardiovascular / Pulmonary");
        assertThat(SpecialtyLabelNormalizer.toCanonical("ent_otolaryngology"))
                .contains("ENT - Otolaryngology");
    }

    @Test
    void acceptsExactHuggingFaceLabel() {
        assertThat(SpecialtyLabelNormalizer.toCanonical("Radiology")).contains("Radiology");
    }

    @Test
    void mapsCommonMislabelCardiology() {
        assertThat(SpecialtyLabelNormalizer.toCanonical("cardiology"))
                .contains("Cardiovascular / Pulmonary");
    }
}
