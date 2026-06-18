package com.example.medicalmcp.promptlab.domain;

import java.util.List;

public record EvalRun(String runId, ClassificationEvalSummary summary, List<ClassificationEvalResult> results) {}
