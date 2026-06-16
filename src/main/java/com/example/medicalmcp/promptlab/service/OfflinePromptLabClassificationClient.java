package com.example.medicalmcp.promptlab.service;

import com.example.medicalmcp.medicalcase.domain.MedicalCase;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prompt-lab")
public class OfflinePromptLabClassificationClient implements PromptLabClassificationClient {

    @Override
    public String classify(MedicalCase medicalCase, String systemPrompt, String templateId) {
        if ("bad".equals(templateId)) {
            return OfflineClassificationSimulator.badOutput(medicalCase);
        }
        return OfflineClassificationSimulator.accurateOutput(medicalCase);
    }
}
