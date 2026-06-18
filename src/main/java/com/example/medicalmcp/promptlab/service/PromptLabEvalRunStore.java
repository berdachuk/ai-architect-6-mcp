package com.example.medicalmcp.promptlab.service;

import com.example.medicalmcp.promptlab.domain.ClassificationEvalResult;
import com.example.medicalmcp.promptlab.domain.ClassificationEvalSummary;
import com.example.medicalmcp.promptlab.domain.EvalRun;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prompt-lab")
public class PromptLabEvalRunStore {

    private final Map<String, EvalRun> runs = new ConcurrentHashMap<>();

    public String save(ClassificationEvalSummary summary, List<ClassificationEvalResult> results) {
        String runId = UUID.randomUUID().toString();
        runs.put(runId, new EvalRun(runId, summary, List.copyOf(results)));
        return runId;
    }

    public Optional<EvalRun> findById(String runId) {
        return Optional.ofNullable(runs.get(runId));
    }

    public Optional<EvalRun> findLatestForTemplate(String templateId) {
        return runs.values().stream()
                .filter(run -> templateId.equals(run.summary().templateId()))
                .reduce((first, second) -> second);
    }

    public List<ClassificationEvalResult> failures(EvalRun run) {
        List<ClassificationEvalResult> failures = new ArrayList<>();
        for (ClassificationEvalResult result : run.results()) {
            if (!result.correct()) {
                failures.add(result);
            }
        }
        return failures;
    }
}
