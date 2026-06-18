package com.example.medicalmcp.medicalcase.domain;

import java.time.Instant;

public record MedicalCase(
        String id,
        String sampleName,
        String description,
        String transcription,
        String medicalSpecialty,
        String keywords,
        String split,
        Instant createdAt) {}
