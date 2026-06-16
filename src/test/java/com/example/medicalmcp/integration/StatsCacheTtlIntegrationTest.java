package com.example.medicalmcp.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.medicalmcp.dataset.service.DatasetLoaderService;
import com.example.medicalmcp.medicalcase.domain.DatasetStats;
import com.example.medicalmcp.retrieval.service.VectorSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(
        properties = {
            "medicalmcp.dataset.loader.enabled=false",
            "medicalmcp.dataset.loader.sources=classpath:dataset/train-sample-10.csv",
            "medicalmcp.retrieval.stats-cache-ttl-seconds=1"
        })
class StatsCacheTtlIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private DatasetLoaderService datasetLoaderService;

    @Autowired
    private VectorSearchService vectorSearchService;

    @BeforeEach
    void loadFixture() {
        datasetLoaderService.loadIfEmpty();
    }

    @Test
    void datasetStatsCacheExpiresAfterConfiguredTtl() throws InterruptedException {
        DatasetStats first = vectorSearchService.getDatasetStats();
        assertThat(first.totalCases()).isEqualTo(10);

        DatasetStats cached = vectorSearchService.getDatasetStats();
        assertThat(cached).isSameAs(first);

        Thread.sleep(1_200);

        DatasetStats refreshed = vectorSearchService.getDatasetStats();
        assertThat(refreshed).isNotSameAs(first);
        assertThat(refreshed.totalCases()).isEqualTo(10);
    }
}
