package com.example.medicalmcp.promptlab.domain;

import java.util.UUID;

public record ClassificationEvalResult(
        UUID caseId, String goldSpecialty, String predictedSpecialty, boolean correct) {}
