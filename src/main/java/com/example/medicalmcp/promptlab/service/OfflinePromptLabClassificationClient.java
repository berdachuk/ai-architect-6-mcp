package com.example.medicalmcp.promptlab.service;

import com.example.medicalmcp.medicalcase.domain.MedicalCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prompt-lab")
@ConditionalOnProperty(
        prefix = "medicalmcp.prompt-lab.chat",
        name = "enabled",
        havingValue = "false",
        matchIfMissing = true)
public class OfflinePromptLabClassificationClient implements PromptLabClassificationClient {

    @Override
    public String classify(MedicalCase medicalCase, String systemPrompt, String templateId) {
        if ("bad".equals(templateId)) {
            return OfflineClassificationSimulator.badOutput(medicalCase);
        }
        return OfflineClassificationSimulator.accurateOutput(medicalCase);
    }
}
