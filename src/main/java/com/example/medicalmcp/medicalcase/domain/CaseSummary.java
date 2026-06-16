package com.example.medicalmcp.medicalcase.domain;

import java.util.UUID;

public record CaseSummary(
        UUID id,
        String sampleName,
        String description,
        String medicalSpecialty,
        String keywords,
        String split) {}
