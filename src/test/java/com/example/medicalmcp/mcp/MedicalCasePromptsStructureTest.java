package com.example.medicalmcp.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.medicalmcp.core.prompt.PromotedSpecialtyClassificationInstructions;
import com.example.medicalmcp.promptlab.template.PromptTemplateLibrary;
import org.junit.jupiter.api.Test;

class MedicalCasePromptsStructureTest {

    @Test
    void promotedTemplateMatchesPromptLabWinner() {
        String promoted = PromotedSpecialtyClassificationInstructions.classificationBlock();
        String labTemplate = PromptTemplateLibrary.findById(PromotedSpecialtyClassificationInstructions.TEMPLATE_ID)
                .orElseThrow()
                .systemText();

        assertThat(labTemplate).isEqualTo(promoted);
    }

    @Test
    void classificationBlockContainsOutputContract() {
        String block = PromotedSpecialtyClassificationInstructions.classificationBlock();

        assertThat(block).contains("PREDICTED_LABEL:");
        assertThat(block).contains("Thought → Action → Observation → Reflection → Answer");
        assertThat(block).contains("cardiovascular_pulmonary");
        assertThat(block).contains("obstetrics_gynecology");
    }
}
