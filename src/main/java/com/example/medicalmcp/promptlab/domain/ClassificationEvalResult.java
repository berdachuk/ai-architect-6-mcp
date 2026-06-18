package com.example.medicalmcp.promptlab.domain;

public record ClassificationEvalResult(
        String caseId, String goldSpecialty, String predictedSpecialty, boolean correct) {}
