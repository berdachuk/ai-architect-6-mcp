package com.example.medicalmcp.promptlab.service;

import com.example.medicalmcp.medicalcase.domain.MedicalCase;

@FunctionalInterface
public interface PromptLabClassificationClient {

    String classify(MedicalCase medicalCase, String systemPrompt, String templateId);
}
