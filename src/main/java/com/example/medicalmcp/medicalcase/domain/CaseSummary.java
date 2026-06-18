package com.example.medicalmcp.medicalcase.domain;

public record CaseSummary(
        String id,
        String sampleName,
        String description,
        String medicalSpecialty,
        String keywords,
        String split) {}
