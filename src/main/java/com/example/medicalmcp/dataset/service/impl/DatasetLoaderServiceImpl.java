package com.example.medicalmcp.dataset.service.impl;

import com.example.medicalmcp.dataset.config.DatasetLoaderProperties;
import com.example.medicalmcp.dataset.service.DatasetLoaderService;
import com.example.medicalmcp.embedding.service.EmbeddingService;
import com.example.medicalmcp.medicalcase.domain.MedicalCase;
import com.example.medicalmcp.medicalcase.repository.MedicalCaseRepository;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DatasetLoaderServiceImpl implements DatasetLoaderService {

    private static final Logger log = LoggerFactory.getLogger(DatasetLoaderServiceImpl.class);

    private static final List<String> SPLIT_ORDER = List.of("train", "validation", "test");

    private final MedicalCaseRepository repository;
    private final EmbeddingService embeddingService;
    private final DatasetLoaderProperties properties;
    private final ResourceLoader resourceLoader;

    public DatasetLoaderServiceImpl(
            MedicalCaseRepository repository,
            EmbeddingService embeddingService,
            DatasetLoaderProperties properties,
            ResourceLoader resourceLoader) {
        this.repository = repository;
        this.embeddingService = embeddingService;
        this.properties = properties;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void loadIfEmpty() {
        if (repository.countAll() == 0) {
            if (properties.getSources().isEmpty()) {
                log.warn("No dataset loader sources configured — skipping CSV ingest");
                return;
            }
            for (String source : properties.getSources()) {
                loadSource(source);
            }
        } else {
            log.info("Dataset already loaded ({} rows) — skipping CSV ingest", repository.countAll());
        }
        embedMissingCases();
    }

    private void loadSource(String location) {
        Resource resource = resourceLoader.getResource(location);
        if (!resource.exists()) {
            throw new IllegalStateException("Dataset source not found: " + location);
        }
        String split = inferSplit(location);
        log.info("Loading dataset split '{}' from {}", split, location);

        int inserted = 0;
        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
                CSVParser parser = CSVFormat.DEFAULT.builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .get()
                        .parse(reader)) {
            List<MedicalCase> batch = new ArrayList<>(properties.getBatchSize());
            for (CSVRecord record : parser) {
                batch.add(toMedicalCase(record, split));
                if (batch.size() >= properties.getBatchSize()) {
                    repository.insertBatch(batch);
                    inserted += batch.size();
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                repository.insertBatch(batch);
                inserted += batch.size();
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load dataset from " + location, ex);
        }
        log.info("Loaded split '{}': {} rows inserted", split, inserted);
    }

    private void embedMissingCases() {
        log.info("Embedding pass: scanning {} total rows across splits {}", repository.countAll(), SPLIT_ORDER);

        int totalProcessed = 0;
        for (String split : SPLIT_ORDER) {
            List<MedicalCase> missing = repository.findWithoutEmbeddingsBySplit(split);
            if (missing.isEmpty()) {
                log.info("Embedding pass [{}]: 0 rows pending (already embedded)", split);
                continue;
            }
            log.info("Embedding pass [{}]: starting {} row(s)", split, missing.size());
            int processed = 0;
            for (int i = 0; i < missing.size(); i += properties.getBatchSize()) {
                int end = Math.min(i + properties.getBatchSize(), missing.size());
                List<MedicalCase> batch = missing.subList(i, end);
                List<String> texts = batch.stream()
                        .map(medicalCase -> embeddingService.buildEmbeddingInput(
                                medicalCase.sampleName(), medicalCase.description(), medicalCase.keywords()))
                        .toList();
                List<float[]> embeddings = embeddingService.embedBatch(texts);
                Map<UUID, float[]> updates = new HashMap<>();
                for (int j = 0; j < batch.size(); j++) {
                    updates.put(batch.get(j).id(), embeddings.get(j));
                }
                repository.updateEmbeddingsBatch(updates);
                processed += batch.size();
                if (processed % 100 == 0 || processed == missing.size()) {
                    log.info("Embedding pass [{}] progress: {}/{}", split, processed, missing.size());
                }
            }
            totalProcessed += processed;
            log.info("Embedding pass [{}] done: {}/{} row(s)", split, processed, missing.size());
        }

        if (totalProcessed > 0) {
            Map<String, Long> counts = repository.countBySplit();
            StringBuilder summary = new StringBuilder();
            for (String split : SPLIT_ORDER) {
                if (summary.length() > 0) {
                    summary.append(", ");
                }
                summary.append(split).append('=').append(counts.getOrDefault(split, 0L));
            }
            log.info("Embedding pass summary: processed {} row(s), per-split totals [{}]", totalProcessed, summary);
        } else {
            log.info("Embedding pass: nothing to do (all splits fully embedded)");
        }
    }

    static String inferSplit(String location) {
        String normalized = location.toLowerCase();
        if (normalized.contains("validation")) {
            return "validation";
        }
        if (normalized.contains("test")) {
            return "test";
        }
        if (normalized.contains("train")) {
            return "train";
        }
        throw new IllegalArgumentException("Cannot infer split from source location: " + location);
    }

    private static MedicalCase toMedicalCase(CSVRecord record, String split) {
        String keywords = blankToNull(record.get("keywords"));
        return new MedicalCase(
                UUID.randomUUID(),
                record.get("sample_name"),
                record.get("description"),
                record.get("transcription"),
                record.get("medical_specialty"),
                keywords,
                split,
                Instant.now());
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }
}
