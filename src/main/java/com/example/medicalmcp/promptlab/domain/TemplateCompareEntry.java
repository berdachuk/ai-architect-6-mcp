package com.example.medicalmcp.promptlab.domain;

public record TemplateCompareEntry(
        String templateId, double accuracy, int correct, int total, boolean passedGate) {}
