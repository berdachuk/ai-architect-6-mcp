package com.example.medicalmcp.promptlab.service;

import com.example.medicalmcp.medicalcase.domain.MedicalCase;
import com.example.medicalmcp.promptlab.domain.ClassificationEvalResult;
import com.example.medicalmcp.promptlab.domain.ClassificationEvalSummary;
import com.example.medicalmcp.promptlab.eval.PredictedLabelExtractor;
import com.example.medicalmcp.promptlab.normalization.SpecialtyLabelNormalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class SpecialtyClassificationEvaluator {

    public ClassificationEvalSummary evaluate(
            String templateId, String split, List<MedicalCase> cases, Function<MedicalCase, String> classifier) {
        List<ClassificationEvalResult> results = new ArrayList<>(cases.size());
        Map<String, Long> errors = new HashMap<>();

        for (MedicalCase medicalCase : cases) {
            String rawPrediction = classifier.apply(medicalCase);
            String predicted = PredictedLabelExtractor.extract(rawPrediction)
                    .flatMap(SpecialtyLabelNormalizer::toCanonical)
                    .orElse("UNKNOWN");
            boolean correct = medicalCase.medicalSpecialty().equals(predicted);
            results.add(new ClassificationEvalResult(medicalCase.id(), medicalCase.medicalSpecialty(), predicted, correct));
            if (!correct) {
                errors.merge(medicalCase.medicalSpecialty(), 1L, Long::sum);
            }
        }

        long correctCount = results.stream().filter(ClassificationEvalResult::correct).count();
        double accuracy = cases.isEmpty() ? 0.0 : (double) correctCount / cases.size();
        return new ClassificationEvalSummary(templateId, split, cases.size(), (int) correctCount, accuracy, Map.copyOf(errors));
    }
}
