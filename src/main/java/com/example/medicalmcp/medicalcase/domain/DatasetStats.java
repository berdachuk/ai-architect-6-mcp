package com.example.medicalmcp.medicalcase.domain;

import java.util.Map;

public record DatasetStats(long totalCases, Map<String, Long> bySpecialty, Map<String, Long> bySplit) {}
