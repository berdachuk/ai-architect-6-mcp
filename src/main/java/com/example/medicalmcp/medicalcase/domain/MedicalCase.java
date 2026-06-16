package com.example.medicalmcp.medicalcase.domain;

import java.time.Instant;
import java.util.UUID;

public record MedicalCase(
        UUID id,
        String sampleName,
        String description,
        String transcription,
        String medicalSpecialty,
        String keywords,
        String split,
        Instant createdAt) {}
