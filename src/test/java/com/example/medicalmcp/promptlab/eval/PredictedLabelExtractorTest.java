package com.example.medicalmcp.promptlab.eval;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PredictedLabelExtractorTest {

    @Test
    void extractsLabelFromReactStyleOutput() {
        String output =
                """
                Thought: review case
                Answer: PREDICTED_LABEL: orthopedic
                """;

        assertThat(PredictedLabelExtractor.extract(output)).contains("orthopedic");
    }

    @Test
    void returnsEmptyWhenContractMissing() {
        assertThat(PredictedLabelExtractor.extract("Orthopedic")).isEmpty();
    }
}
