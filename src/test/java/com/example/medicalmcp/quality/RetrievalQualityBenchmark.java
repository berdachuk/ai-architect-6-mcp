package com.example.medicalmcp.quality;

import com.example.medicalmcp.embedding.service.EmbeddingService;
import com.example.medicalmcp.medicalcase.domain.CaseSummary;
import com.example.medicalmcp.medicalcase.domain.MedicalCase;
import com.example.medicalmcp.medicalcase.domain.SemanticMatch;
import com.example.medicalmcp.medicalcase.repository.MedicalCaseRepository;
import com.example.medicalmcp.retrieval.service.VectorSearchService;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.util.StringUtils;

public final class RetrievalQualityBenchmark {

    private final MedicalCaseRepository repository;
    private final VectorSearchService vectorSearch;
    private final EmbeddingService embeddingService;

    public RetrievalQualityBenchmark(
            MedicalCaseRepository repository,
            VectorSearchService vectorSearch,
            EmbeddingService embeddingService) {
        this.repository = repository;
        this.vectorSearch = vectorSearch;
        this.embeddingService = embeddingService;
    }

    public Map<String, Double> runFtsMetrics(String split) {
        List<CaseSummary> rows = repository.fullTextSearch("patient", null, split, 50);
        if (rows.isEmpty()) {
            return Map.of(
                    "sampleName_hit10", 0.0,
                    "transcriptionPhrase_hit10", 0.0,
                    "keyword_hit10", 0.0,
                    "mrr", 0.0);
        }

        double sampleNameHits = 0;
        double phraseHits = 0;
        double keywordHits = 0;
        double mrrSum = 0;
        int phraseCount = 0;
        int keywordCount = 0;

        for (CaseSummary row : rows) {
            MedicalCase fullCase = repository.findById(row.id()).orElseThrow();
            List<CaseSummary> byName = vectorSearch.searchCases(row.sampleName(), null, null, 10);
            if (containsId(byName, row.id())) {
                sampleNameHits++;
            }
            mrrSum += reciprocalRank(byName, row.id());

            String phrase = firstPhrase(fullCase.transcription());
            if (phrase != null) {
                phraseCount++;
                if (containsId(vectorSearch.searchCases(phrase, null, null, 10), row.id())) {
                    phraseHits++;
                }
            }

            String keyword = firstKeyword(fullCase.keywords());
            if (keyword != null) {
                keywordCount++;
                if (containsId(vectorSearch.searchCases(keyword, null, null, 10), row.id())) {
                    keywordHits++;
                }
            }
        }

        int n = rows.size();
        Map<String, Double> metrics = new HashMap<>();
        metrics.put("sampleName_hit10", sampleNameHits / n);
        metrics.put("transcriptionPhrase_hit10", phraseCount == 0 ? 0.0 : phraseHits / phraseCount);
        metrics.put("keyword_hit10", keywordCount == 0 ? 0.0 : keywordHits / keywordCount);
        metrics.put("mrr", mrrSum / n);
        return Map.copyOf(metrics);
    }

    public Map<String, Double> runSemanticMetrics(String split) {
        List<CaseSummary> rows = repository.fullTextSearch("patient", null, split, 50);
        if (rows.isEmpty()) {
            return Map.of(
                    "self_at_1", 0.0,
                    "self_at_5", 0.0,
                    "specialty_at_5", 0.0,
                    "mean_similarity_at_1", 0.0);
        }

        double selfAt1 = 0;
        double selfAt5 = 0;
        double specialtyAt5 = 0;
        double similaritySum = 0;

        for (CaseSummary row : rows) {
            MedicalCase fullCase = repository.findById(row.id()).orElseThrow();
            String query = embeddingService.buildEmbeddingInput(
                    fullCase.sampleName(), fullCase.description(), fullCase.keywords());
            List<SemanticMatch> matches = vectorSearch.semanticSearch(query, null, 5, 0.0);
            if (!matches.isEmpty()) {
                similaritySum += matches.getFirst().similarity();
                if (row.id().equals(matches.getFirst().caseSummary().id())) {
                    selfAt1++;
                }
            }
            if (matches.stream().anyMatch(match -> row.id().equals(match.caseSummary().id()))) {
                selfAt5++;
            }
            if (matches.stream().anyMatch(match -> row.medicalSpecialty().equals(match.caseSummary().medicalSpecialty()))) {
                specialtyAt5++;
            }
        }

        int n = rows.size();
        return Map.of(
                "self_at_1", selfAt1 / n,
                "self_at_5", selfAt5 / n,
                "specialty_at_5", specialtyAt5 / n,
                "mean_similarity_at_1", similaritySum / n);
    }

    private static boolean containsId(List<CaseSummary> results, UUID id) {
        return results.stream().anyMatch(result -> id.equals(result.id()));
    }

    private static double reciprocalRank(List<CaseSummary> results, UUID id) {
        for (int i = 0; i < results.size(); i++) {
            if (id.equals(results.get(i).id())) {
                return 1.0 / (i + 1);
            }
        }
        return 0.0;
    }

    private static String firstPhrase(String transcription) {
        if (!StringUtils.hasText(transcription)) {
            return null;
        }
        String[] words = transcription.toLowerCase(Locale.ROOT).split("\\s+");
        if (words.length < 3) {
            return null;
        }
        return words[0] + " " + words[1] + " " + words[2];
    }

    private static String firstKeyword(String keywords) {
        if (!StringUtils.hasText(keywords)) {
            return null;
        }
        String token = keywords.split(",")[0].trim();
        return StringUtils.hasText(token) ? token : null;
    }
}
