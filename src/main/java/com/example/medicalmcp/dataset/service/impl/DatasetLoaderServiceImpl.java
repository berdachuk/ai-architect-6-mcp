package com.example.medicalmcp.dataset.service.impl;

import com.example.medicalmcp.dataset.config.DatasetLoaderProperties;
import com.example.medicalmcp.dataset.service.DatasetLoaderService;
import com.example.medicalmcp.medicalcase.domain.MedicalCase;
import com.example.medicalmcp.medicalcase.repository.MedicalCaseRepository;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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

    private final MedicalCaseRepository repository;
    private final DatasetLoaderProperties properties;
    private final ResourceLoader resourceLoader;

    public DatasetLoaderServiceImpl(
            MedicalCaseRepository repository,
            DatasetLoaderProperties properties,
            ResourceLoader resourceLoader) {
        this.repository = repository;
        this.properties = properties;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void loadIfEmpty() {
        if (repository.countAll() > 0) {
            log.info("Dataset already loaded ({} rows) — skipping CSV ingest", repository.countAll());
            return;
        }
        if (properties.getSources().isEmpty()) {
            log.warn("No dataset loader sources configured — skipping CSV ingest");
            return;
        }

        for (String source : properties.getSources()) {
            loadSource(source);
        }
    }

    private void loadSource(String location) {
        Resource resource = resourceLoader.getResource(location);
        if (!resource.exists()) {
            throw new IllegalStateException("Dataset source not found: " + location);
        }
        String split = inferSplit(location);
        log.info("Loading dataset split '{}' from {}", split, location);

        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
                CSVParser parser = CSVFormat.DEFAULT.builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .build()
                        .parse(reader)) {
            List<MedicalCase> batch = new ArrayList<>(properties.getBatchSize());
            for (CSVRecord record : parser) {
                batch.add(toMedicalCase(record, split));
                if (batch.size() >= properties.getBatchSize()) {
                    repository.insertBatch(batch);
                    batch.clear();
                }
            }
            repository.insertBatch(batch);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load dataset from " + location, ex);
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
